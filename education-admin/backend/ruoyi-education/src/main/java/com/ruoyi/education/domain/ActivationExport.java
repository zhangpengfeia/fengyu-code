package com.ruoyi.education.domain;
import com.ruoyi.common.annotation.Excel;
import java.util.Map;
public class ActivationExport {
    @Excel(name="批次ID") public Long batchId;
    @Excel(name="激活码") public String code;
    @Excel(name="状态",readConverterExp="0=未使用,1=已使用,2=已过期") public String status;
    @Excel(name="学生ID") public Long studentId;
    public ActivationExport(){}
    public ActivationExport(Map<String,Object> r){batchId=((Number)r.get("batch_id")).longValue();code=r.get("code").toString();status=r.get("status").toString();studentId=r.get("student_id")==null?null:((Number)r.get("student_id")).longValue();}
    public Long getBatchId(){return batchId;}public void setBatchId(Long v){batchId=v;}
    public String getCode(){return code;}public void setCode(String v){code=v;}
    public String getStatus(){return status;}public void setStatus(String v){status=v;}
    public Long getStudentId(){return studentId;}public void setStudentId(Long v){studentId=v;}
}
