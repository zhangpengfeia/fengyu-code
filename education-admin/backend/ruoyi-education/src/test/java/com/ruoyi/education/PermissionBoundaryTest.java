package com.ruoyi.education;
import com.ruoyi.education.service.*;
import com.ruoyi.education.mapper.BizMapper;
import com.ruoyi.education.domain.*;
import com.ruoyi.common.core.domain.entity.*;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.system.service.ISysUserService;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.time.*;

class PermissionBoundaryTest {
    BizMapper db;ISysUserService users;AccessService access;SysUser student,teacher;
    SysUser user(long uid,String key) {
        var u=new SysUser(uid);u.setDeptId(11000L);u.setStatus("0");u.setDelFlag("0");
        var r=new SysRole(103L);r.setRoleKey(key);r.setStatus("0");r.setDataScope("5");u.setRoles(List.of(r));return u;
    }
    @BeforeEach void setup() {
        db=mock(BizMapper.class);users=mock(ISysUserService.class);access=new AccessService(db,users,mock(SysPermissionService.class));
        student=user(20002,"edu_student");teacher=user(20001,"edu_teacher");
        when(db.one(BizTable.SCHOOL,11000)).thenReturn(Map.of("school_status","ACTIVE","expire_time",Date.from(Instant.now().plusSeconds(600))));
    }
    @Test void teacherOnlyGameRejectsStudentEvenWithDirectGrant() {
        var game=Map.<String,Object>of("game_id",1L,"status","PUBLISHED","access_policy","TEACHER_ONLY");
        when(db.scopes(20002)).thenReturn(List.of(Map.of("game_id",1L)));
        assertFalse(access.game(student,game));assertTrue(access.game(teacher,game));
    }
    @Test void studentNeedsBothSchoolPoolAndOwnCoursewareGrant() {
        var c=Map.<String,Object>of("courseware_id",7L,"status","PUBLISHED","volume",1,"subject_code","MATH");
        when(db.scopes(20002)).thenReturn(List.of(Map.of("courseware_id",7L)));
        assertFalse(access.courseware(student,c));
        when(db.pool(11000)).thenReturn(List.of(Map.of("subject_code","ANY")));
        assertTrue(access.courseware(student,c));
        when(db.scopes(20002)).thenReturn(List.of());assertFalse(access.courseware(student,c));
    }
    @Test void gameScopeCannotAccidentallyAuthorizeAllCourseware() {
        assertFalse(access.matches(Map.of("game_id",2L),Map.of("courseware_id",2L),"courseware_id"));
    }
    @Test void crossSchoolCoursewareIsRejected() {
        assertFalse(access.courseware(student,Map.of("status","PUBLISHED","courseware_id",1L,"school_id",12000L)));
    }
    @Test void expiredSchoolFailsImmediatelyWithoutScheduler() {
        when(db.one(BizTable.SCHOOL,11000)).thenReturn(Map.of("school_status","ACTIVE","expire_time",Date.from(Instant.now().minusMillis(1))));
        assertThrows(ServiceException.class,()->access.game(teacher,Map.of("game_id",1L,"status","PUBLISHED","access_policy","TEACHER_ONLY")));
    }
    @Test void disabledAncestorFailsClosed() {
        when(db.disabledSchoolPath(11000)).thenReturn(1);assertThrows(ServiceException.class,()->access.schoolActive(11000));
    }
    @Test void archivedUserHasNoAccess() {
        student.setStatus("1");when(users.selectUserById(20002L)).thenReturn(student);assertThrows(ServiceException.class,()->access.active(20002));
    }
    @Test void inactiveResourceIsDeniedForAllLaunches() {
        var admin=user(1,"admin");assertFalse(access.game(admin,Map.of("status","DISABLED")));
    }
    @Test void rootMayCreateANewSchoolBeforeItsBusinessRowExists() {
        var root=user(1,"admin");when(users.selectUserById(1L)).thenReturn(root);
        var principal=new com.ruoyi.common.core.domain.model.LoginUser(1L,11000L,root,Set.of("*:*:*"));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal,null,List.of()));
        try {assertDoesNotThrow(()->access.school(12000));}finally{org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    }
}
