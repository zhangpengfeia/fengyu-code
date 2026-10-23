package com.ruoyi.education.web;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.education.service.GameService;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.*;
import java.util.*;
import java.nio.file.Files;

@RestController
public class PlayController {
    final GameService games;final boolean secure;
    public PlayController(GameService g,@Value("${education.secure-game-cookie:true}") boolean secure) {games=g;this.secure=secure;}
    @Anonymous @GetMapping("/play/{code}/session") public ResponseEntity<?> session(@PathVariable String code,HttpServletRequest req) {
        String sid=null;if(req.getCookies()!=null)for(Cookie c:req.getCookies())if(c.getName().equals("EDU_GAME"))sid=c.getValue();
        games.session(code,sid);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("authorized",true));
    }
    @Anonymous @GetMapping("/play/{code}/exchange")
    public ResponseEntity<Void> exchange(@PathVariable String code,@RequestParam String ticket) {
        String sid=games.exchange(code,ticket);
        // Lax permits the explicit top-level redirect from the separate admin site.
        var cookie=ResponseCookie.from("EDU_GAME",sid).httpOnly(true).secure(secure).sameSite("Lax").path("/play/"+code+"/").maxAge(1800).build();
        return ResponseEntity.status(303).header("Set-Cookie",cookie.toString()).header("Location","/play/"+code+"/index.html").header("Referrer-Policy","no-referrer").cacheControl(CacheControl.noStore()).build();
    }
    @Anonymous @GetMapping("/play/{code}/**")
    public ResponseEntity<FileSystemResource> asset(@PathVariable String code,HttpServletRequest req) throws java.io.IOException {
        String sid=null;if(req.getCookies()!=null) for(Cookie c:req.getCookies()) if(c.getName().equals("EDU_GAME")) sid=c.getValue();
        String prefix="/play/"+code+"/";String path=req.getRequestURI().substring(prefix.length());
        var file=games.asset(code,sid,path);String mime=Files.probeContentType(file);
        if(path.endsWith(".js")) mime="application/javascript";if(path.endsWith(".css")) mime="text/css";
        return ResponseEntity.ok().header("Content-Type",mime==null?"application/octet-stream":mime).header("X-Content-Type-Options","nosniff")
            .header("Referrer-Policy","no-referrer").header("Content-Security-Policy","default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; media-src 'self' blob:; connect-src 'self'; object-src 'none'; frame-ancestors 'none'; base-uri 'self'")
            .cacheControl(CacheControl.noStore()).body(new FileSystemResource(file));
    }
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<String> forbidden(ServiceException ex) {
        int status=ex.getCode()==null?403:ex.getCode();
        return ResponseEntity.status(status).contentType(MediaType.TEXT_HTML).cacheControl(CacheControl.noStore()).body("<!doctype html><html lang=zh-CN><meta charset=utf-8><title>访问受限</title><style>body{font-family:Arial,sans-serif;background:#f3f6fa;text-align:center;padding:15vh 24px;color:#24344b}h1{font-size:28px}p{color:#697b91}</style><h1>"+(status==404?"游戏资源尚未就绪":"没有权限访问该游戏")+"</h1><p>请从教育平台登录后，通过已授权菜单进入。</p></html>");
    }
}
