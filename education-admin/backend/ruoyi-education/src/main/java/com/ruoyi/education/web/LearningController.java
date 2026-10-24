package com.ruoyi.education.web;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.education.service.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static com.ruoyi.education.service.AccessService.*;

@RestController
@RequestMapping("/biz/learning")
public class LearningController {
    final DubbingService dubbing;final ActivationService activation;final ScoreService scores;final ClassroomService classroom;
    public LearningController(DubbingService d,ActivationService a,ScoreService s,ClassroomService c){dubbing=d;activation=a;scores=s;classroom=c;}
    @GetMapping("/notices") public AjaxResult notices(){return AjaxResult.success(dubbing.notices());}
    @GetMapping("/dubbing/{kind}") public Map<String,Object> list(@PathVariable String kind,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,@RequestParam Map<String,String> filters){return dubbing.list(kind,page,size,filters);}
    @PostMapping("/dubbing/{kind}") @Log(title="配音配置新增",businessType=BusinessType.INSERT)
    public AjaxResult add(@PathVariable String kind,@RequestBody Map<String,Object> p){return AjaxResult.success(Map.of("id",dubbing.save(kind,null,p)));}
    @PutMapping("/dubbing/{kind}/{id}") @Log(title="配音配置修改",businessType=BusinessType.UPDATE)
    public AjaxResult edit(@PathVariable String kind,@PathVariable long id,@RequestBody Map<String,Object> p){return AjaxResult.success(Map.of("id",dubbing.save(kind,id,p)));}
    @DeleteMapping("/dubbing/materials/{id}") @Log(title="配音素材删除",businessType=BusinessType.DELETE)
    public AjaxResult remove(@PathVariable long id){dubbing.remove(id);return AjaxResult.success();}
    @GetMapping("/records") public Map<String,Object> records(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) Long class_id,@RequestParam(required=false) Long student_id,@RequestParam(required=false) Integer volume,@RequestParam(required=false) String dubbing_type){return dubbing.records(page,size,class_id,student_id,volume,dubbing_type);}
    @PostMapping("/records/export") @Log(title="配音记录导出",businessType=BusinessType.EXPORT)
    @SuppressWarnings("unchecked") public void exportRecords(@RequestParam(required=false) Long class_id,@RequestParam(required=false) Long student_id,@RequestParam(required=false) Integer volume,@RequestParam(required=false) String dubbing_type,jakarta.servlet.http.HttpServletResponse response){
        dubbing.requireExportPermission();
        var rows=new ArrayList<com.ruoyi.education.domain.DubbingExport>();
        for(int page=1;page<=1000;page++) {var result=dubbing.records(page,100,class_id,student_id,volume,dubbing_type);var batch=(List<Map<String,Object>>)result.get("rows");batch.forEach(r->rows.add(new com.ruoyi.education.domain.DubbingExport(r)));if(rows.size()>=((Number)result.get("total")).longValue())break;}
        new com.ruoyi.common.utils.poi.ExcelUtil<>(com.ruoyi.education.domain.DubbingExport.class).exportExcel(response,rows,"配音记录");
    }
    @PostMapping("/exam/draw") public AjaxResult draw(@RequestBody Map<String,Object> p){return AjaxResult.success(dubbing.draw((int)id(p.get("volume"))));}
    @PostMapping("/dubbing/score") @Log(title="配音评分",isSaveRequestData=false)
    public AjaxResult score(@RequestBody Map<String,Object> p){return AjaxResult.success(dubbing.score(p));}
    @GetMapping("/activation/batches") public Map<String,Object> batches(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size){return activation.batches(page,size);}
    @PostMapping("/activation/batches") @Log(title="激活码生成",businessType=BusinessType.INSERT,isSaveResponseData=false)
    public AjaxResult generate(@RequestBody Map<String,Object> p){return AjaxResult.success(Map.of("id",activation.generate(p)));}
    @GetMapping("/activation/batches/{id}/codes") public AjaxResult codes(@PathVariable long id){return AjaxResult.success(activation.codes(id));}
    @PostMapping("/activation/batches/{id}/export") @Log(title="激活码导出",businessType=BusinessType.EXPORT,isSaveResponseData=false)
    public void export(@PathVariable long id,jakarta.servlet.http.HttpServletResponse response){
        var rows=activation.codes(id).stream().map(com.ruoyi.education.domain.ActivationExport::new).toList();
        new com.ruoyi.common.utils.poi.ExcelUtil<>(com.ruoyi.education.domain.ActivationExport.class).exportExcel(response,rows,"激活码");
    }
    @PostMapping("/activation/redeem") @Log(title="激活码兑换",businessType=BusinessType.GRANT,isSaveRequestData=false)
    public AjaxResult redeem(@RequestBody Map<String,String> p){return AjaxResult.success(Map.of("id",activation.redeem(p.get("code"))));}
    @PostMapping("/scores") @Log(title="课堂积分调整",businessType=BusinessType.UPDATE)
    public AjaxResult adjust(@RequestBody Map<String,Object> p){scores.adjust(p);return AjaxResult.success();}
    @PostMapping("/scores/all-plus") @Log(title="全班课堂表现加分",businessType=BusinessType.UPDATE)
    public AjaxResult allPlus(@RequestBody Map<String,Object> p){scores.allPlus(id(p.get("class_id")),id(p.get("courseware_id")));return AjaxResult.success();}
    @GetMapping("/ranking/school") public AjaxResult schoolRanking(@RequestParam long school_id,@RequestParam(required=false) Integer grade,@RequestParam(required=false) Integer volume){return AjaxResult.success(scores.schoolRanking(school_id,grade,volume));}
    @PostMapping("/scores/{id}/reverse") @Log(title="课堂积分撤销",businessType=BusinessType.UPDATE)
    public AjaxResult reverse(@PathVariable long id){scores.reverse(id);return AjaxResult.success();}
    @GetMapping("/scores") public AjaxResult ledger(@RequestParam long student_id){return AjaxResult.success(scores.ledger(student_id));}
    @GetMapping("/ranking") public AjaxResult ranking(@RequestParam long class_id,@RequestParam(required=false) Long courseware_id,@RequestParam(required=false) Integer volume){return AjaxResult.success(scores.ranking(class_id,courseware_id,volume));}
    @GetMapping("/groups") public AjaxResult groups(@RequestParam long class_id){return AjaxResult.success(classroom.groups(class_id));}
    @PostMapping("/groups/regroup") @Log(title="班级随机重分组",businessType=BusinessType.UPDATE)
    public AjaxResult regroup(@RequestBody Map<String,Object> p){classroom.regroup(id(p.get("class_id")),(int)id(p.get("size")));return AjaxResult.success();}
    @PostMapping("/groups/add") @Log(title="班级小组新增",businessType=BusinessType.INSERT)
    public AjaxResult addGroup(@RequestBody Map<String,Object> p){return AjaxResult.success(Map.of("id",classroom.addGroup(id(p.get("class_id")),text(p,"group_name"))));}
    @PostMapping("/groups/remove") @Log(title="班级小组删除",businessType=BusinessType.DELETE)
    public AjaxResult removeGroup(@RequestBody Map<String,Object> p){classroom.removeGroup(id(p.get("class_id")),id(p.get("group_id")),id(p.get("target_group_id")));return AjaxResult.success();}
    @PostMapping("/groups/move") @Log(title="班级小组成员调整",businessType=BusinessType.UPDATE)
    public AjaxResult move(@RequestBody Map<String,Object> p){classroom.move(id(p.get("class_id")),id(p.get("student_id")),id(p.get("group_id")));return AjaxResult.success();}
    @GetMapping("/pk") public AjaxResult rounds(@RequestParam long class_id,@RequestParam long courseware_id,@RequestParam(defaultValue="PERSONAL") String mode,@RequestParam(defaultValue="BASIC") String stage){return AjaxResult.success(classroom.rounds(class_id,courseware_id,mode,stage));}
    @PostMapping("/pk/match") @Log(title="课堂PK整体随机匹配",businessType=BusinessType.INSERT)
    public AjaxResult match(@RequestBody Map<String,Object> p){return AjaxResult.success(Map.of("id",classroom.match(id(p.get("class_id")),id(p.get("courseware_id")),text(p,"mode"),text(p,"stage"))));}
    @PostMapping("/pk/{id}/result") @Log(title="课堂PK结果调整",businessType=BusinessType.UPDATE)
    public AjaxResult result(@PathVariable long id,@RequestBody Map<String,Object> p){classroom.result(id,p.get("winner_id")==null?null:AccessService.id(p.get("winner_id")));return AjaxResult.success();}
    @PostMapping("/pk/clear") @Log(title="课堂PK清空赛程",businessType=BusinessType.CLEAN)
    public AjaxResult clear(@RequestBody Map<String,Object> p){classroom.clear(id(p.get("class_id")),id(p.get("courseware_id")),text(p,"mode"),text(p,"stage"));return AjaxResult.success();}
    @PostMapping("/classes/archive") @Log(title="班级毕业归档",businessType=BusinessType.UPDATE)
    public AjaxResult archive(@RequestBody List<Long> ids){classroom.archive(ids);return AjaxResult.success();}
}
