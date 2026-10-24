package com.ruoyi.education.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.education.domain.*;
import com.ruoyi.education.mapper.BizMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.time.*;
import java.util.*;
import static com.ruoyi.education.service.AccessService.*;

@Service
public class CatalogService {
    final BizMapper db; final AccessService access; final ObjectMapper json;
    final Set<String> externalHosts;
    @Value("${education.games-root:../games}") String gamesRoot;
    public CatalogService(BizMapper db,AccessService access,ObjectMapper json,@Value("${education.external-hosts:}") String hosts) {
        this.db=db;this.access=access;this.json=json; externalHosts=new HashSet<>(Arrays.asList(hosts.split(",")));
    }
    public static BizTable table(String kind) {
        return switch(kind) { case "games"->BizTable.GAME; case "courseware"->BizTable.COURSEWARE;
            case "schools"->BizTable.SCHOOL;case "classes"->BizTable.CLASS;case "pool"->BizTable.POOL;
            default ->throw new ServiceException("模块不存在",404); };
    }
    public String module(BizTable t) { return switch(t) {case GAME->"game";case COURSEWARE->"courseware";case SCHOOL->"school";case CLASS->"class";case POOL->"pool";default->throw new IllegalArgumentException();}; }
    public boolean manage(SysUser u,BizTable t) { return u.isAdmin()||access.permissions.getMenuPermission(u).contains("biz:"+module(t)+":edit"); }
    public Map<String,Object> list(String kind,int page,int size,String keyword,Map<String,String> filters) {
        BizTable t=table(kind); access.require("biz:"+module(t)+":list");
        SysUser u=access.current(); BizQuery q=new BizQuery(t);q.keyword=keyword;
        for(String k:List.of("status","source_type","category_code","subject_code","volume","school_id","stage_code","ability")) {
            if((t.writable.contains(k)||(t==BizTable.COURSEWARE&&"ability".equals(k))) && filters.get(k)!=null && !filters.get(k).isBlank()) q.eq(k,filters.get(k));
        }
        if(!u.isAdmin() && t!=BizTable.GAME) q.schoolIds=access.schoolIds(u);
        if((t==BizTable.GAME||t==BizTable.COURSEWARE)&&!manage(u,t)) {
            q.limit=10000;
            q.ids=db.list(q).stream().filter(r->t==BizTable.GAME?access.game(u,r):access.courseware(u,r)).map(r->id(r.get(t.id))).toList();
        }
        q.limit=Math.max(1,Math.min(size,100)); q.offset=(Math.max(1,Math.min(page,100000))-1)*q.limit;
        var rows=db.list(q); if(t==BizTable.GAME) rows.forEach(r->r.remove("entry_path"));
        return Map.of("code",200,"rows",rows,"total",db.count(q));
    }
    public Map<String,Object> detail(BizTable t,long rid,boolean editing) {
        if(!editing) access.require("biz:"+module(t)+((t==BizTable.GAME||t==BizTable.COURSEWARE)?":preview":":query"));
        Map<String,Object> r=db.one(t,rid); if(r==null) throw new ServiceException("记录不存在",404);
        SysUser u=access.current();
        if(editing) {
            access.require("biz:"+module(t)+":edit");
            if(t==BizTable.GAME && !u.isAdmin()) deny();
            if(t==BizTable.COURSEWARE && r.get("school_id")==null && !u.isAdmin()) deny();
            if(t!=BizTable.GAME && r.get("school_id")!=null) access.school(id(r.get("school_id")));
        } else if(!u.isAdmin()&&(t==BizTable.GAME ? !access.game(u,r) : t==BizTable.COURSEWARE ? !access.courseware(u,r) : false)) deny();
        else if(t!=BizTable.GAME && t!=BizTable.COURSEWARE) access.school(id(r.get("school_id")));
        if(t==BizTable.COURSEWARE) {
            var gameIds=db.gameIds(rid);
            if(!editing&&!u.isAdmin()) gameIds=gameIds.stream().filter(gid->access.game(u,db.one(BizTable.GAME,gid))).toList();
            r.put("game_ids",gameIds);
            BizTable ext="MATH".equals(r.get("subject_code"))?BizTable.MATH:"READING".equals(r.get("subject_code"))?BizTable.READ:null;
            if(ext!=null) r.put("extension",Objects.requireNonNullElse(db.one(ext,rid),Map.of()));
        }
        return r;
    }
    public static String str(Map<String,Object> v,String k,int max) {
        String s=Objects.toString(v.get(k),"").trim(); if(s.isEmpty()||s.length()>max) throw new ServiceException(k+"不能为空且长度不超过"+max,400); v.put(k,s); return s;
    }
    static void choice(Map<String,Object> v,String k,String... allowed) { if(!Arrays.asList(allowed).contains(v.get(k))) throw new ServiceException(k+"值无效",400); }
    public static void date(Map<String,Object> v,String k) {
        try { v.put(k,java.sql.Timestamp.from(Instant.parse(v.get(k).toString()))); }
        catch(Exception ex) {throw new ServiceException(k+"必须为带时区的时间",400);}
    }
    public void external(String url) {
        try { URI u=URI.create(url); if(!"https".equals(u.getScheme())||u.getUserInfo()!=null||u.getHost()==null||!externalHosts.contains(u.getHost())||u.getPort()!=-1) deny(); }
        catch(IllegalArgumentException ex) { throw new ServiceException("外部游戏地址无效",400); }
    }
    @Transactional
    public long save(String kind,Long rid,Map<String,Object> payload) {
        BizTable t=table(kind); SysUser u=access.current();
        access.require("biz:"+module(t)+(rid==null?":add":":edit"));
        Map<String,Object> old=rid==null?null:detail(t,rid,true);
        if(old!=null && (!payload.containsKey("row_version")||id(payload.get("row_version"))!=id(old.get("row_version")))) throw new ServiceException("记录已更新，请刷新后重新编辑",409);
        if(t==BizTable.GAME&&!u.isAdmin()) deny();
        Map<String,Object> v=new LinkedHashMap<>();
        for(String key:t.writable) if(payload.containsKey(key)) v.put(key,payload.get(key));
        v.remove("generatedId");
        if(t==BizTable.GAME) {
            str(v,"game_name",128);String code=str(v,"game_code",64);String version=str(v,"version_no",32);
            if(!code.matches("[a-z0-9][a-z0-9-]{0,63}")||!version.matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,31}")) throw new ServiceException("游戏编码或版本无效",400);
            if(old!=null&&!code.equals(old.get("game_code"))) throw new ServiceException("游戏编码创建后不能修改",400);
            choice(v,"source_type","INTERNAL","EXTERNAL");choice(v,"access_policy","TEACHER_ONLY","COURSEWARE","STUDENT");
            String entry=str(v,"entry_path",1024);
            if("EXTERNAL".equals(v.get("source_type"))) external(entry);
            else if(!entry.equals("index.html")) throw new ServiceException("内部游戏入口必须为index.html",400);
            if("PUBLISHED".equals(v.get("status"))&&"INTERNAL".equals(v.get("source_type"))) {
                try {
                    var root=java.nio.file.Path.of(gamesRoot).toRealPath();var dir=root.resolve(code).resolve(version).toRealPath();
                    if(!dir.startsWith(root)||!java.nio.file.Files.isRegularFile(dir.resolve("index.html"))) throw new IllegalArgumentException();
                    var manifest=json.readTree(dir.resolve("manifest.json").toFile());
                    if(!code.equals(manifest.path("game_code").asText())||!version.equals(manifest.path("version").asText())) throw new IllegalArgumentException();
                } catch(Exception ex) {throw new ServiceException("发布失败，请先构建对应版本并提供匹配的manifest.json",400);}
            }
        }
        if(t==BizTable.COURSEWARE) {
            if(old!=null&&!Objects.equals(old.get("subject_code"),v.get("subject_code")))throw new ServiceException("已建立课件不可切换学科",400);
            str(v,"courseware_name",128);if(!db.dictValues("biz_subject").contains(text(v,"subject_code")))throw new ServiceException("请选择已启用的学科字典项",400);
            if(id(v.get("volume"))<1||id(v.get("volume"))>12) throw new ServiceException("册次须为1至12",400);
            v.put("version_no",old==null?0:old.get("version_no"));
        }
        if(t==BizTable.GAME||t==BizTable.COURSEWARE) {
            choice(v,"status","DRAFT","PUBLISHED","DISABLED");
            if(text(v,"description").length()>4000) throw new ServiceException("说明过长",400);
            v.putIfAbsent("sort_no",0);
            if(id(v.get("sort_no"))<0||id(v.get("sort_no"))>99999) throw new ServiceException("排序值无效",400);
        }
        if(t!=BizTable.GAME) {
            if(t==BizTable.COURSEWARE && v.get("school_id")==null) {if(!u.isAdmin()) deny();}
            else access.school(id(v.get("school_id")));
        }
        if(t==BizTable.SCHOOL) {
            str(v,"school_name",100);str(v,"school_code",32);date(v,"expire_time");choice(v,"school_status","ACTIVE","DISABLED");
            if("ACTIVE".equals(v.get("school_status"))&&!instant(v.get("expire_time")).isAfter(Instant.now()))throw new ServiceException("启用学校的到期时间须晚于当前时间",400);
            if(db.schoolDepartment(id(v.get("school_id")))==0) throw new ServiceException("须先在部门管理建立学校节点",400);
        }
        if(t==BizTable.CLASS) {
            str(v,"class_name",64);String year=str(v,"academic_year",9);
            if(!year.matches("20\\d{2}-20\\d{2}")||id(v.get("grade"))<1||id(v.get("grade"))>6) throw new ServiceException("学年或年级无效",400);
            choice(v,"status","ACTIVE","ARCHIVED");
            if(old!=null && "ARCHIVED".equals(v.get("status")) && db.currentClassStudents(rid)>0) throw new ServiceException("请先归档或转移班级学生",400);
        }
        if(t==BizTable.POOL) {date(v,"start_time");date(v,"end_time");if(!instant(v.get("end_time")).isAfter(instant(v.get("start_time")))) throw new ServiceException("截止时间须晚于开始时间",400);}
        if(old!=null && t!=BizTable.GAME && !Objects.equals(old.get("school_id"),v.get("school_id")) && id(old.get("school_id"))!=id(v.get("school_id"))) throw new ServiceException("资源归属创建后不能直接变更",400);
        if(rid==null) {db.insert(t,v,u.getUserName());rid=t==BizTable.SCHOOL?id(v.get("school_id")):id(v.get("generatedId"));}
        else {
            v.put("expected_row_version",old.get("row_version"));
            if(db.update(t,rid,v,u.getUserName())!=1) throw new ServiceException("记录已更新，请刷新后重新编辑",409);
        }
        if(t==BizTable.SCHOOL) db.schoolState(rid,v.get("expire_time"),"ACTIVE".equals(v.get("school_status"))?"0":"1",u.getUserName());
        if(t==BizTable.COURSEWARE) {
            BizTable ext="MATH".equals(v.get("subject_code"))?BizTable.MATH:"READING".equals(v.get("subject_code"))?BizTable.READ:null;
            Map<String,Object> extension=new LinkedHashMap<>();
            if(ext!=null) {
                Object raw=payload.get("extension");
                if(!(raw instanceof Map<?,?>))throw new ServiceException("请填写学科扩展信息",400);
                Map<?,?> supplied=(Map<?,?>)raw;
                for(String k:ext.writable)if(supplied.containsKey(k))extension.put(k,supplied.get(k));
                extension.put("courseware_id",rid);
                if(ext==BizTable.MATH){str(extension,"stage",32);str(extension,"ability",64);for(String k:List.of("discuss_img","intro_video","game_url","teacher_book"))LearningSupport.url(extension,k,false);}
                else {
                    try {var tree=json.readTree(text(extension,"config_json"));if(tree==null||!tree.isObject())throw new IllegalArgumentException();}
                    catch(Exception e){throw new ServiceException("课件流程须为有效JSON对象",400);}
                    if(extension.get("dubbing_material_id")!=null) {
                        var m=db.one(BizTable.MATERIAL,id(extension.get("dubbing_material_id")));
                        if(m==null||id(m.get("volume"))!=id(v.get("volume"))||!"0".equals(m.get("status"))||!DubbingService.courseTag(m))throw new ServiceException("请选择本册启用的课件配套或通用素材",400);
                    }
                }
                if(db.one(ext,rid)==null)db.insert(ext,extension,u.getUserName());else db.update(ext,rid,extension,u.getUserName());
            }
            Object gids=payload.get("game_ids"); List<?> ids=gids instanceof List<?> l?l:List.of();
            if(ids.size()>100) throw new ServiceException("单课件关联游戏不能超过100个",400);
            db.unlinkGames(rid);int sort=0;
            for(Object gid:new LinkedHashSet<>(ids)) {var game=db.one(BizTable.GAME,id(gid));if(game==null) throw new ServiceException("关联游戏不存在",400);db.linkGame(rid,id(gid),sort++);}
            if("PUBLISHED".equals(v.get("status"))) {
                int version=(old==null?0:((Number)old.get("version_no")).intValue())+1;
                db.update(BizTable.COURSEWARE,rid,new LinkedHashMap<>(Map.of("version_no",version)),u.getUserName());
                try {db.version(rid,version,json.writeValueAsString(Map.of("content",v,"game_ids",ids,"extension",extension)),u.getUserName());}
                catch(Exception ex) {throw new ServiceException("课件发布版本保存失败",500);}
            }
        }
        return rid;
    }
    public List<Map<String,Object>> versions(long course) {
        detail(BizTable.COURSEWARE,course,true);var versions=db.versions(course);
        for(var version:versions) {
            try {
                Object raw=version.get("content_snapshot_json");var tree=raw instanceof byte[] bytes?json.readTree(bytes):json.readTree(raw.toString());
                if(tree.isTextual())tree=json.readTree(tree.textValue());version.put("content_snapshot_json",tree.toString());
            } catch(Exception e){throw new ServiceException("课件快照数据格式无效",500);}
        }
        return versions;
    }
    @Transactional public long customize(long course,long school) {
        access.require("biz:courseware:add");access.school(school);access.schoolActive(school);
        var source=detail(BizTable.COURSEWARE,course,false);
        if(source.get("school_id")!=null)throw new ServiceException("仅支持从全局模板建立学校定制",400);
        var copy=new LinkedHashMap<>(source);copy.put("school_id",school);copy.put("status","DRAFT");
        return save("courseware",null,copy);
    }
    @Transactional
    public void remove(String kind,long rid) {
        BizTable t=table(kind);access.require("biz:"+module(t)+":remove");detail(t,rid,true);
        if(t==BizTable.SCHOOL||t==BizTable.CLASS) throw new ServiceException("学校班级请使用停用或归档，保留历史引用",400);
        if(t==BizTable.GAME&& !db.linkedCourseware(rid).isEmpty()) throw new ServiceException("游戏已被课件引用，请先解除关联",400);
        db.delete(t,rid,access.current().getUserName());
    }
}
