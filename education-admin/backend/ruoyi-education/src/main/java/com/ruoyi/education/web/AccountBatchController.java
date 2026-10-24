package com.ruoyi.education.web;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.education.domain.AccountImport;
import com.ruoyi.education.service.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
@RestController
@RequestMapping("/biz/users/batch")
public class AccountBatchController {
    final AccountBatchService service;final AccessService access;
    public AccountBatchController(AccountBatchService s,AccessService a){service=s;access=a;}
    @PostMapping("/template") public void template(HttpServletResponse response){access.require("biz:user:import");new ExcelUtil<>(AccountImport.class).importTemplateExcel(response,"教育账号导入模板");}
    @PostMapping("/import") @Log(title="教育账号批量导入",businessType=BusinessType.IMPORT,isSaveRequestData=false,isSaveResponseData=false)
    public AjaxResult imports(@RequestParam MultipartFile file,@RequestParam(defaultValue="SKIP") String mode) throws Exception {
        access.require("biz:user:import");if(file.getSize()>5*1024*1024)throw new com.ruoyi.common.exception.ServiceException("文件不能超过5MB",400);
        try(var in=file.getInputStream()){return AjaxResult.success(service.importRows(new ExcelUtil<>(AccountImport.class).importExcel(in),mode));}
    }
    @PostMapping("/reset") @Log(title="教育账号批量重置密码",businessType=BusinessType.UPDATE,isSaveRequestData=false,isSaveResponseData=false)
    public AjaxResult reset(@RequestBody ResetRequest p){return AjaxResult.success(service.reset(p.ids(),p.password()));}
    public record ResetRequest(List<Long> ids,String password){}
}
