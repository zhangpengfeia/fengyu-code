package com.ruoyi.education.web;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.education.service.StudentRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.*;
@RestController
@RequestMapping("/biz/student-registration")
public class StudentRegistrationController {
    final StudentRegistrationService service;final RedisCache cache;
    public StudentRegistrationController(StudentRegistrationService s,RedisCache c){service=s;cache=c;}
    @Anonymous @GetMapping("/tree") public AjaxResult tree(){return AjaxResult.success(service.tree());}
    @Anonymous @GetMapping("/schools") public AjaxResult schools(){return AjaxResult.success(service.schools());}
    @Anonymous @GetMapping("/classes") public AjaxResult classes(@RequestParam long school_id){return AjaxResult.success(service.classes(school_id));}
    @Anonymous @PostMapping public AjaxResult register(@RequestBody Map<String,Object> p,HttpServletRequest request){
        Long count=cache.incrementWindow("biz:register:ip:"+request.getRemoteAddr(),Duration.ofMinutes(1));if(count!=null&&count>10)throw new ServiceException("请求过于频繁，请稍后再试",429);
        return AjaxResult.success(Map.of("audit_required",service.register(p)));
    }
}
