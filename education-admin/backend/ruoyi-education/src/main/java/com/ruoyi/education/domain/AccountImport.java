package com.ruoyi.education.domain;
import com.ruoyi.common.annotation.Excel;
public class AccountImport {
    @Excel(name="学校ID 必填") private Long schoolId;
    @Excel(name="学生班级ID 学生必填") private Long classId;
    @Excel(name="身份 edu_student或edu_teacher 必填") private String roleKey;
    @Excel(name="老师登录账号 老师必填") private String userName;
    @Excel(name="姓名 必填") private String nickName;
    @Excel(name="学号 学生必填 保留前导零") private String studentNo;
    @Excel(name="手机号 选填") private String phone;
    @Excel(name="任教班级ID 逗号分隔 老师选填") private String classIds;
    public Long getSchoolId(){return schoolId;} public void setSchoolId(Long v){schoolId=v;}
    public Long getClassId(){return classId;} public void setClassId(Long v){classId=v;}
    public String getRoleKey(){return roleKey;} public void setRoleKey(String v){roleKey=v;}
    public String getUserName(){return userName;} public void setUserName(String v){userName=v;}
    public String getNickName(){return nickName;} public void setNickName(String v){nickName=v;}
    public String getStudentNo(){return studentNo;} public void setStudentNo(String v){studentNo=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getClassIds(){return classIds;} public void setClassIds(String v){classIds=v;}
}
