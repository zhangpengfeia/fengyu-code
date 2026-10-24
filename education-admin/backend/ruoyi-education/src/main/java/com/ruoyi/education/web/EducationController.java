package com.ruoyi.education.web;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.education.service.*;
import com.ruoyi.education.mapper.BizMapper;
import com.ruoyi.education.domain.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static com.ruoyi.education.service.AccessService.*;

@RestController
@RequestMapping("/biz")
public class EducationController {
    final CatalogService catalog;final AccountService accounts;final AccessService access;final GrantService grants;final GameService games;final BizMapper db;
    public EducationController(CatalogService c,AccountService a,AccessService p,GrantService e,GameService g,BizMapper db) {catalog=c;accounts=a;access=p;grants=e;games=g;this.db=db;}
    @GetMapping("/catalog/{kind}") public Map<String,Object> list(@PathVariable String kind,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="") String keyword,@RequestParam Map<String,String> filters) {
        return catalog.list(kind,page,size,keyword,filters);
    }
    @GetMapping("/catalog/{kind}/{id}") public AjaxResult detail(@PathVariable String kind,@PathVariable long id,@RequestParam(defaultValue="false") boolean edit) {return AjaxResult.success(catalog.detail(CatalogService.table(kind),id,edit));}
    @PostMapping("/catalog/{kind}") @Log(title="教育资源新增",businessType=BusinessType.INSERT)
    public AjaxResult create(@PathVariable String kind,@RequestBody Map<String,Object> body) {return AjaxResult.success(Map.of("id",catalog.save(kind,null,body)));}
    @PutMapping("/catalog/{kind}/{id}") @Log(title="教育资源修改",businessType=BusinessType.UPDATE)
    public AjaxResult update(@PathVariable String kind,@PathVariable long id,@RequestBody Map<String,Object> body) {return AjaxResult.success(Map.of("id",catalog.save(kind,id,body)));}
    @DeleteMapping("/catalog/{kind}/{id}") @Log(title="教育资源删除",businessType=BusinessType.DELETE)
    public AjaxResult remove(@PathVariable String kind,@PathVariable long id) {catalog.remove(kind,id);return AjaxResult.success();}
    @GetMapping("/courseware/{id}/versions") public AjaxResult versions(@PathVariable long id) {return AjaxResult.success(catalog.versions(id));}
    @PostMapping("/courseware/{id}/customize") @Log(title="学校课件定制",businessType=BusinessType.INSERT)
    public AjaxResult customize(@PathVariable long id,@RequestBody Map<String,Object> body){return AjaxResult.success(Map.of("id",catalog.customize(id,AccessService.id(body.get("school_id")))));}
    @PostMapping("/games/{id}/launch") @Log(title="游戏预览",isSaveResponseData=false)
    public AjaxResult launch(@PathVariable long id) {return AjaxResult.success(games.launch(id));}
    @GetMapping("/users") public Map<String,Object> users(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="") String keyword,@RequestParam(required=false) String role_key,@RequestParam(required=false) String school_id,@RequestParam(required=false) String class_id) {return accounts.list(page,size,keyword,role_key,school_id,class_id);}
    @GetMapping("/users/{id}") public AjaxResult user(@PathVariable long id) {return AjaxResult.success(accounts.detail(id));}
    @PostMapping("/users") @Log(title="教育账号新增",businessType=BusinessType.INSERT,isSaveRequestData=false,isSaveResponseData=false)
    public AjaxResult createUser(@RequestBody Map<String,Object> body) {return AjaxResult.success(accounts.save(null,body));}
    @PutMapping("/users/{id}") @Log(title="教育账号修改",businessType=BusinessType.UPDATE,isSaveRequestData=false,isSaveResponseData=false)
    public AjaxResult updateUser(@PathVariable long id,@RequestBody Map<String,Object> body) {return AjaxResult.success(accounts.save(id,body));}
    @PostMapping("/users/{id}/reset-password") @Log(title="教育账号密码重置",isSaveRequestData=false,isSaveResponseData=false)
    public AjaxResult reset(@PathVariable long id) {return AjaxResult.success(Map.of("temporary_password",accounts.reset(id)));}
    @GetMapping("/entitlements") public AjaxResult entitlements(@RequestParam long student_user_id) {return AjaxResult.success(grants.list(student_user_id));}
    @PostMapping("/entitlements") @Log(title="学生授权发放",businessType=BusinessType.GRANT)
    public AjaxResult grant(@RequestBody Map<String,Object> body) {return AjaxResult.success(Map.of("id",grants.grant(body)));}
    @PostMapping("/entitlements/{id}/revoke") @Log(title="学生授权撤销",businessType=BusinessType.UPDATE)
    public AjaxResult revoke(@PathVariable long id,@RequestBody Map<String,String> body) {grants.revoke(id,body.get("reason"));return AjaxResult.success();}
    @GetMapping("/options") public AjaxResult options() {
        var u=access.current();var qs=new BizQuery(BizTable.SCHOOL);qs.ids=access.schoolIds(u);qs.limit=1000;
        var qc=new BizQuery(BizTable.CLASS);qc.schoolIds=access.schoolIds(u);qc.limit=10000;
        var schools=db.list(qs);schools.forEach(s->s.keySet().retainAll(Set.of("school_id","school_name","school_code")));
        var classes=db.list(qc);
        if(access.teacher(u)) classes.removeIf(c->db.teaches(u.getUserId(),id(c.get("class_id")))==0);
        if(access.student(u)) {var s=db.one(BizTable.STUDENT,u.getUserId());classes.removeIf(c->s==null||id(c.get("class_id"))!=id(s.get("class_id")));}
        var roles=u.isAdmin()?List.of("edu_student","edu_teacher","edu_school"):List.of("edu_student","edu_teacher");
        return AjaxResult.success(Map.of("schools",schools,"classes",classes,"roles",roles));
    }
}
