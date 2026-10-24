package com.ruoyi.education.service;

import com.ruoyi.education.domain.AccountImport;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.ruoyi.education.service.AccessService.*;

@Service
public class AccountBatchService {
    final AccountService accounts;final LearningSupport s;
    public AccountBatchService(AccountService a,LearningSupport s){accounts=a;this.s=s;}
    @Transactional public List<Map<String,Object>> importRows(List<AccountImport> rows,String mode) {
        s.access.require("biz:user:import");if(s.access.teacher(s.access.current())||s.access.student(s.access.current()))deny();
        if(!Set.of("SKIP","OVERWRITE").contains(mode)||rows.isEmpty()||rows.size()>1000)throw new ServiceException("导入模式无效或数据不在1至1000行",400);
        var result=new ArrayList<Map<String,Object>>();int row=1;
        for(var input:rows) {
            row++;
            try {
                String role=input.getRoleKey();if(!Set.of("edu_student","edu_teacher").contains(role))throw new ServiceException("身份只能为学生或老师",400);
                long school=Objects.requireNonNull(input.getSchoolId());s.access.school(school);
                Long uid="edu_student".equals(role)?s.db.studentUser(school,input.getStudentNo()):Optional.ofNullable(s.access.users.selectUserByUserName(input.getUserName())).map(com.ruoyi.common.core.domain.entity.SysUser::getUserId).orElse(null);
                if(uid!=null){s.access.userTarget(s.access.user(uid));if("SKIP".equals(mode)){result.add(Map.of("row",row,"status","已跳过重复"));continue;}}
                var p=new LinkedHashMap<String,Object>();p.put("school_id",school);p.put("class_id",input.getClassId());p.put("role_key",role);p.put("user_name",input.getUserName());p.put("nick_name",input.getNickName());p.put("student_no",input.getStudentNo());p.put("phone",Objects.toString(input.getPhone(),""));p.put("status","0");
                String classes=Objects.toString(input.getClassIds(),"");p.put("class_ids",classes.isBlank()?List.of():Arrays.stream(classes.split(",")).map(String::trim).map(Long::valueOf).toList());
                var saved=new LinkedHashMap<>(accounts.save(uid,p));saved.put("row",row);saved.put("status",uid==null?"已新增":"已更新");result.add(saved);
            } catch(Exception e){throw new ServiceException("Excel第"+row+"行导入失败，整批未保存："+(e instanceof ServiceException?e.getMessage():"字段格式或唯一性校验失败"),400);}
        }
        return result;
    }
    @Transactional public List<Map<String,Object>> reset(List<Long> ids,String password) {
        s.access.require("biz:user:resetPwd");if(s.access.teacher(s.access.current())||s.access.student(s.access.current()))deny();
        if(ids.isEmpty()||ids.size()>1000)throw new ServiceException("每次选择1至1000个账号",400);
        if(password!=null&&!password.isBlank())AccountService.password(password);
        var result=new ArrayList<Map<String,Object>>();
        for(long uid:new TreeSet<>(ids)) {
            var u=s.access.user(uid);s.access.userTarget(u);
            String p=password==null||password.isBlank()?AccountService.temporaryPassword():password;
            u.setPassword(SecurityUtils.encryptPassword(p));s.access.resetPassword(u);s.db.bumpPassword(uid,true);result.add(Map.of("user_id",uid,"temporary_password",p));
        }
        return result;
    }
}
