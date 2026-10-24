package com.ruoyi.education.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.education.domain.*;
import com.ruoyi.education.mapper.BizMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.ruoyi.common.core.redis.RedisCache;
import java.time.Duration;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import static com.ruoyi.education.service.AccessService.*;

@Service
public class GameService {
    final BizMapper db;final AccessService access;final CatalogService catalog;final RedisCache redis;final ObjectMapper json;
    final Path root; final String origin;
    public GameService(BizMapper db,AccessService a,CatalogService c,RedisCache redis,ObjectMapper json,@Value("${education.games-root:../games}") String root,@Value("${education.game-origin:http://localhost:5173}") String origin) {
        this.db=db;access=a;catalog=c;this.redis=redis;this.json=json;this.root=Path.of(root).toAbsolutePath().normalize();this.origin=origin.replaceAll("/$","");
    }
    static String random() {byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    void put(String key,Map<String,Object> value,Duration ttl) {
        try {redis.set(key,json.writeValueAsString(value),ttl);} catch(Exception ex) {throw new ServiceException("游戏会话服务不可用",503);}
    }
    Map<String,Object> read(String value) {
        try {if(value==null) deny();return json.readValue(value,new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});} catch(ServiceException ex) {throw ex;} catch(Exception ex) {deny();return Map.of();}
    }
    public Map<String,Object> launch(long gameId) {
        access.require("biz:game:preview");var u=access.current();var g=db.one(BizTable.GAME,gameId);
        if(!access.game(u,g)) deny();
        if("EXTERNAL".equals(g.get("source_type"))) {String url=text(g,"entry_path");catalog.external(url);return Map.of("url",url,"protection","ENTRY_ONLY","message","该外部游戏不支持平台鉴权，仅控制后台入口");}
        file(g,"index.html");String ticket=random();
        put("edu:ticket:"+ticket,new LinkedHashMap<>(Map.of("uid",u.getUserId(),"gid",gameId,"code",g.get("game_code"),"passwordHash",u.getPassword(),"parentToken",com.ruoyi.common.utils.SecurityUtils.getLoginUser().getToken())),Duration.ofSeconds(60));
        return Map.of("url",origin+"/play/"+g.get("game_code")+"/exchange?ticket="+ticket,"protection","GATEWAY");
    }
    public String exchange(String code,String ticket) {
        if(ticket==null||!ticket.matches("[A-Za-z0-9_-]{43}")) deny();
        var data=read(redis.getAndDelete("edu:ticket:"+ticket));
        if(!code.equals(data.get("code"))) deny();check(data);
        String sid=random();put("edu:game-session:"+sid,data,Duration.ofMinutes(30));return sid;
    }
    Map<String,Object> check(Map<String,Object> session) {
        if(session.get("parentToken")==null||!Boolean.TRUE.equals(redis.hasKey(com.ruoyi.common.constant.CacheConstants.LOGIN_TOKEN_KEY+session.get("parentToken")))) deny();
        var u=access.active(id(session.get("uid")));
        if(!access.permits(u,"biz:game:preview")) deny();
        if(!Objects.equals(session.get("passwordHash"),u.getPassword())) deny();
        var security=db.one(BizTable.SECURITY,u.getUserId());if(security!=null&&id(security.get("must_change_pwd"))==1) deny();
        var g=db.one(BizTable.GAME,id(session.get("gid")));
        if(!access.game(u,g)) deny();return g;
    }
    public Path asset(String code,String sid,String asset) {
        if(sid==null||!sid.matches("[A-Za-z0-9_-]{43}")) deny();
        var data=read(redis.getCacheObject("edu:game-session:"+sid));if(!code.equals(data.get("code"))) deny();return file(check(data),asset);
    }
    public void session(String code,String sid) { asset(code,sid,"index.html"); }
    Path file(Map<String,Object> g,String asset) {
        try {
            if(asset.isBlank()) asset="index.html";
            if(asset.contains("\\")||Arrays.asList(asset.split("/")).stream().anyMatch(x->x.startsWith(".")||x.equals(".."))) deny();
            if(!asset.matches("(?i).+\\.(html|js|css|png|jpe?g|webp|svg|json|woff2?|mp3|mp4|ogg|wav|ico)$")) deny();
            Path base=root.resolve(text(g,"game_code")).resolve(text(g,"version_no")).normalize();
            Path target=base.resolve(asset).normalize();
            if(!base.startsWith(root)||!target.startsWith(base)||!Files.isRegularFile(target)) throw new ServiceException("游戏尚未构建或资源不存在",404);
            if(!target.toRealPath().startsWith(base.toRealPath())||!base.toRealPath().startsWith(root.toRealPath())) deny();
            return target;
        } catch(java.io.IOException ex) {throw new ServiceException("游戏资源不存在",404);}
    }
}
