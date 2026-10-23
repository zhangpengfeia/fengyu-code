package com.ruoyi.education.config;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Map;

@RestControllerAdvice(basePackages="com.ruoyi.education.web")
@Order(-10)
public class EducationErrors {
    @ExceptionHandler(ServiceException.class) public ResponseEntity<?> business(ServiceException ex) {
        int code=ex.getCode()==null?400:ex.getCode();return ResponseEntity.status(code>=400&&code<=599?code:400).body(Map.of("code",code,"msg",ex.getMessage()));
    }
    @ExceptionHandler({DuplicateKeyException.class,DataIntegrityViolationException.class}) public ResponseEntity<?> conflict(Exception ex) {return ResponseEntity.status(409).body(Map.of("code",409,"msg","账号、手机号、学号或资源编码重复，或关联数据不满足约束"));}
    @ExceptionHandler({IllegalArgumentException.class,org.springframework.http.converter.HttpMessageNotReadableException.class}) public ResponseEntity<?> bad(Exception ex) {return ResponseEntity.badRequest().body(Map.of("code",400,"msg","请求参数格式无效"));}
}
