package com.ruoyi.education.web;
import com.ruoyi.common.annotation.*;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.education.domain.*;
import com.ruoyi.education.mapper.BizMapper;
import com.ruoyi.education.service.*;
import com.ruoyi.framework.web.service.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.core.env.Environment;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.*;
import java.security.SecureRandom;
import static com.ruoyi.education.service.AccessService.*;

@RestController
@RequestMapping("/biz/auth")
public class AuthController {
    final BizMapper db;final AccessService access;final SysLoginService login;final TokenService tokens;final StringRedisTemplate redis;final Environment env;
    public AuthController(BizMapper db,AccessService a,SysLoginService l,TokenService t,StringRedisTemplate redis,Environment e) {this.db=db;access=a;login=l;tokens=t;this.redis=redis;env=e;}
    @Anonymous @PostMapping("/login")
    public AjaxResult login(@RequestBody Map<String,String> v,HttpServletRequest req) {
        String username=Objects.toString(v.get("username"),"").trim();String mode=Objects.toString(v.get("mode"),"ACCOUNT");
        String throttle="edu:login-ip:"+req.getRemoteAddr();Long attempts=redis.opsForValue().increment(throttle);if(attempts!=null&&attempts==1) redis.expire(throttle,Duration.ofMinutes(1));if(attempts!=null&&attempts>30) throw new ServiceException("请求过于频繁，请稍后重试",429);
        Long uid=null;
        if("PHONE".equals(mode)) uid=db.phoneUser(username);
        else if("STUDENT".equals(mode)) {try {uid=db.studentUser(Long.parseLong(v.get("school_id")),username);} catch(RuntimeException ignored) {}}
        else if(!"ACCOUNT".equals(mode)) throw new ServiceException("登录方式无效",400);
        if(!"ACCOUNT".equals(mode)) {if(uid==null) username="unknown_"+UUID.randomUUID().toString().substring(0,8);else username=access.user(uid).getUserName();}
        // Preserve native captcha, lockout and login audit. Never log this request body.
        String token=login.login(username,v.get("password"),Objects.toString(v.get("code"),""),Objects.toString(v.get("uuid"),""));
        return AjaxResult.success().put("token",token);
    }
    @GetMapping("/me") public AjaxResult me() {
        var u=access.current();var a=db.one(BizTable.SECURITY,u.getUserId());
        return AjaxResult.success(Map.of("user_id",u.getUserId(),"name",u.getNickName(),"school_id",u.getDeptId(),"must_change_pwd",a!=null&&id(a.get("must_change_pwd"))==1,"phone_verified",a!=null&&id(a.get("phone_verified"))==1));
    }
    @PostMapping("/password") @Transactional
    @Log(title="修改本人密码",isSaveRequestData=false,isSaveResponseData=false)
    public AjaxResult password(@RequestBody Map<String,String> body) {
        var u=access.current();if(!SecurityUtils.matchesPassword(body.get("old_password"),u.getPassword())) throw new ServiceException("原密码不正确",400);
        AccountService.password(body.get("new_password"));u.setPassword(SecurityUtils.encryptPassword(body.get("new_password")));access.resetPassword(u);db.bumpPassword(u.getUserId(),false);return AjaxResult.success("密码已修改，请重新登录");
    }
    @PostMapping("/phone/code")
    public AjaxResult phoneCode(@RequestBody Map<String,String> body) {
        if(!env.matchesProfiles("dev")) throw new ServiceException("短信服务尚未配置，请联系管理员",503);
        String phone=body.get("phone");if(phone==null||!phone.matches("1[3-9]\\d{9}")) throw new ServiceException("手机号无效",400);
        var u=access.current();String key="edu:phone:"+u.getUserId();
        if(redis.hasKey(key)) throw new ServiceException("验证码已发送，请稍后再试",429);
        String code=String.format("%06d",new SecureRandom().nextInt(1000000));redis.opsForValue().set(key,phone+":"+code,Duration.ofMinutes(5));
        return AjaxResult.success(Map.of("development_code",code,"message","仅开发环境返回模拟验证码，生产必须接短信服务"));
    }
    @PostMapping("/phone/bind") @Transactional
    @Log(title="绑定本人手机号",isSaveRequestData=false,isSaveResponseData=false)
    public AjaxResult bind(@RequestBody Map<String,String> v) {
        var u=access.current();if(!SecurityUtils.matchesPassword(v.get("password"),u.getPassword())) throw new ServiceException("密码不正确",400);
        String expected=redis.opsForValue().getAndDelete("edu:phone:"+u.getUserId());
        if(expected==null||!expected.equals(v.get("phone")+":"+v.get("code"))) throw new ServiceException("验证码无效或已过期",400);
        db.phone(u.getUserId(),v.get("phone"));db.verifyPhone(u.getUserId(),v.get("phone"));return AjaxResult.success();
    }
    @Anonymous @GetMapping("/school/{code}") public AjaxResult school(@PathVariable String code) {
        var q=new BizQuery(BizTable.SCHOOL).eq("school_code",code);var rows=db.list(q);
        if(rows.isEmpty()) throw new ServiceException("学校入口不存在",404);var s=rows.get(0);access.schoolActive(id(s.get("school_id")));
        return AjaxResult.success(Map.of("school_id",s.get("school_id"),"school_name",s.get("school_name")));
    }
}
