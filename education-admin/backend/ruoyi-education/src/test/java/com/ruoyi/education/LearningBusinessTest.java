package com.ruoyi.education;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.education.domain.*;
import com.ruoyi.education.mapper.*;
import com.ruoyi.education.service.*;
import com.ruoyi.system.service.ISysConfigService;
import org.apache.ibatis.session.*;
import org.apache.ibatis.mapping.Environment;
import org.h2.jdbcx.JdbcDataSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LearningBusinessTest {
    JdbcTemplate jdbc;BizMapper db;AccessService access;LearningSupport support;ActivationService activation;ScoreService scores;ClassroomService classroom;DubbingService dubbing;TransactionTemplate tx;SysUser actor;
    @BeforeEach void setup() throws Exception {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:learning"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE,KEY;LOCK_TIMEOUT=5000");
        try(var c=ds.getConnection()){ScriptUtils.executeSqlScript(c,new FileSystemResource(Path.of("../ruoyi-admin/src/main/resources/db/development.sql")));}
        var config=new Configuration(new Environment("test",new SpringManagedTransactionFactory(),ds));config.addMapper(BizMapper.class);
        db=new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(config)).getMapper(BizMapper.class);jdbc=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        access=mock(AccessService.class);actor=new SysUser(1L);actor.setUserName("admin");when(access.current()).thenAnswer(i->actor);when(access.courseware(any(),any())).thenReturn(true);
        var settings=mock(ISysConfigService.class);when(settings.selectConfigByKey(anyString())).thenReturn("");
        support=new LearningSupport(db,access,jdbc,new ObjectMapper(),settings);activation=new ActivationService(support);scores=new ScoreService(support);classroom=new ClassroomService(support,scores);dubbing=new DubbingService(support);
        jdbc.update("INSERT INTO sys_dept(dept_id,parent_id,ancestors,dept_name) VALUES(11000,100,'0,100','测试学校')");
        jdbc.update("INSERT INTO biz_school(school_id,school_code,school_name,expire_time) VALUES(11000,'TEST','测试学校','2099-01-01')");
        jdbc.update("INSERT INTO biz_class(class_id,school_id,academic_year,grade,class_name) VALUES(1,11000,'2026-2027',1,'一班')");
        for(long uid=201;uid<=205;uid++) {
            jdbc.update("INSERT INTO sys_user(user_id,dept_id,user_name,nick_name,password) VALUES(?,11000,?,?, 'x')",uid,"student"+uid,"学生"+uid);
            jdbc.update("INSERT INTO biz_student_extend(user_id,school_id,class_id,student_no) VALUES(?,11000,1,?)",uid,"no"+uid);
        }
        jdbc.update("INSERT INTO biz_courseware_base(courseware_id,subject_code,volume,courseware_name,status) VALUES(1,'MATH',1,'数学','PUBLISHED')");
    }
    Map<String,Object> batch(){return new LinkedHashMap<>(Map.of("batch_name","测试批次","permission_type","1","permission_range","{}","valid_days",180,"total_num",2,"expire_time","2099-01-01T00:00:00Z"));}
    long createBatch(){return tx.execute(status->activation.generate(batch()));}
    String code(long batch){return jdbc.queryForObject("SELECT code FROM biz_activate_code WHERE batch_id=? ORDER BY code_id LIMIT 1",String.class,batch);}
    void student(){actor=new SysUser(201L);actor.setUserName("student201");when(access.student(any())).thenReturn(true);}
    @Test void redemptionIsAtomicAndAddsWorkingCoursewareScope() {
        long batch=createBatch();String code=code(batch);student();
        long eid=tx.execute(status->activation.redeem(code));assertTrue(eid>0);assertEquals(1,jdbc.queryForObject("SELECT used_num FROM biz_activate_code_batch WHERE batch_id=?",Integer.class,batch));
        assertEquals(1,db.scopes(201).size());assertEquals(1,((Number)db.scopes(201).get(0).get("volume")).intValue());
        assertThrows(ServiceException.class,()->tx.execute(status->activation.redeem(code)));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM biz_student_entitlement",Integer.class));
    }
    @Test void simultaneousRedemptionsHaveOnlyOneWinner() throws Exception {
        long batch=createBatch();String code=code(batch);student();var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {
            Callable<Boolean> task=()->{start.await();try{tx.execute(status->activation.redeem(code));return true;}catch(ServiceException e){return false;}};
            var a=pool.submit(task);var b=pool.submit(task);start.countDown();assertNotEquals(a.get(),b.get());
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM biz_student_entitlement",Integer.class));
            assertEquals(1,jdbc.queryForObject("SELECT used_num FROM biz_activate_code_batch WHERE batch_id=?",Integer.class,batch));
        } finally{pool.shutdownNow();}
    }
    @Test void expiredAndWrongSchoolCodesDoNotGrantPermissions() {
        long batch=createBatch();String code=code(batch);student();jdbc.update("UPDATE biz_activate_code_batch SET school_id=12000 WHERE batch_id=?",batch);
        assertThrows(ServiceException.class,()->tx.execute(status->activation.redeem(code)));
        jdbc.update("UPDATE biz_activate_code_batch SET school_id=NULL,expire_time='2000-01-01' WHERE batch_id=?",batch);
        assertThrows(ServiceException.class,()->tx.execute(status->activation.redeem(code)));assertTrue(db.scopes(201).isEmpty());
    }
    @Test void ledgerAndAggregatesReverseTogetherAndDuplicateEventsRollback() {
        var p=new LinkedHashMap<String,Object>(Map.of("student_id",201,"courseware_id",1,"score",1,"source","倾听","event_key","event-1"));
        tx.executeWithoutResult(status->scores.adjust(p));assertEquals(1,jdbc.queryForObject("SELECT total_score FROM biz_student_extend WHERE user_id=201",Integer.class));
        assertThrows(Exception.class,()->tx.executeWithoutResult(status->scores.adjust(p)));assertEquals(1,jdbc.queryForObject("SELECT total_score FROM biz_student_extend WHERE user_id=201",Integer.class));
        long log=jdbc.queryForObject("SELECT log_id FROM biz_score_log",Long.class);tx.executeWithoutResult(status->scores.reverse(log));
        assertEquals(0,jdbc.queryForObject("SELECT total_score FROM biz_student_extend WHERE user_id=201",Integer.class));assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM biz_score_log",Integer.class));
        assertThrows(Exception.class,()->tx.executeWithoutResult(status->scores.reverse(log)));
    }
    @Test void regroupPreservesHistoryAndEveryStudentAppearsOnce() {
        tx.executeWithoutResult(status->classroom.regroup(1,2));assertEquals(3,classroom.groups(1).size());
        tx.executeWithoutResult(status->classroom.regroup(1,4));assertEquals(2,classroom.groups(1).size());
        assertEquals(5,jdbc.queryForObject("SELECT COUNT(*) FROM biz_student_group WHERE is_current='0'",Integer.class));
        assertEquals(5,jdbc.queryForObject("SELECT COUNT(DISTINCT student_id) FROM biz_student_group WHERE is_current='1'",Integer.class));
    }
    @Test void pkByeAndResultReversalAndClearKeepCorrectLedger() {
        long round=tx.execute(status->classroom.match(1,1,"PERSONAL","BASIC"));
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM biz_pk_match WHERE round_id=?",Integer.class,round));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM biz_pk_match WHERE round_id=? AND right_id IS NULL AND winner_id IS NOT NULL",Integer.class,round));
        var match=jdbc.queryForList("SELECT * FROM biz_pk_match WHERE round_id=? AND right_id IS NOT NULL",round).get(0);long mid=((Number)match.get("match_id")).longValue(),win=((Number)match.get("left_id")).longValue();
        tx.executeWithoutResult(status->classroom.result(mid,win));tx.executeWithoutResult(status->classroom.result(mid,win));
        assertEquals(1,jdbc.queryForObject("SELECT total_score FROM biz_student_extend WHERE user_id=?",Integer.class,win));
        tx.executeWithoutResult(status->classroom.result(mid,null));assertEquals(0,jdbc.queryForObject("SELECT total_score FROM biz_student_extend WHERE user_id=?",Integer.class,win));
        tx.executeWithoutResult(status->classroom.result(mid,win));tx.executeWithoutResult(status->classroom.clear(1,1,"PERSONAL","BASIC"));
        assertEquals(1,jdbc.queryForObject("SELECT total_score FROM biz_student_extend WHERE user_id=?",Integer.class,win));assertThrows(ServiceException.class,()->tx.executeWithoutResult(status->classroom.result(mid,null)));
    }
    @Test void groupStageRequiresCompletedPreviousStageAndScoresSnapshotMembers() {
        tx.executeWithoutResult(status->classroom.regroup(1,2));assertThrows(ServiceException.class,()->tx.execute(status->classroom.match(1,1,"GROUP","LEVEL2")));
        long round=tx.execute(status->classroom.match(1,1,"GROUP","BASIC"));
        for(var match:jdbc.queryForList("SELECT * FROM biz_pk_match WHERE round_id=? AND right_id IS NOT NULL",round))tx.executeWithoutResult(status->classroom.result(((Number)match.get("match_id")).longValue(),((Number)match.get("left_id")).longValue()));
        assertDoesNotThrow(()->tx.execute(status->classroom.match(1,1,"GROUP","LEVEL2")));
    }
    @Test void invalidDubbingVolumeAndClientScoresAreRejected() {
        var p=new LinkedHashMap<String,Object>(Map.of("volume",13,"title","素材","material_text","text","material_tag","01","video_url","https://example.com/a.mp4","status","0"));
        assertThrows(ServiceException.class,()->tx.execute(status->dubbing.save("materials",null,p)));
        p.put("volume",1);long mid=tx.execute(status->dubbing.save("materials",null,p));
        assertThrows(ServiceException.class,()->dubbing.validateMaterial(mid,2));student();
        var ex=assertThrows(ServiceException.class,()->dubbing.score(Map.of("material_id",mid,"total_score",100)));assertEquals(503,ex.getCode());
    }
    @Test void materialTagsFilterUsesMembershipAndCoursewareSnapshotsContainExtensions() {
        var material=new LinkedHashMap<String,Object>(Map.of("volume",1,"title","阅读素材","material_text","text","material_tag","01,02","video_url","https://example.com/video.mp4","status","0"));
        long mid=tx.execute(status->dubbing.save("materials",null,material));
        assertEquals(1,db.count(new BizQuery(BizTable.MATERIAL).eq("material_tag","01")));
        var catalog=new CatalogService(db,access,new ObjectMapper(),"");
        var payload=new LinkedHashMap<String,Object>(Map.of("subject_code","READING","volume",1,"stage_code","BASIC","courseware_name","阅读课件","status","PUBLISHED","extension",Map.of("dubbing_material_id",mid,"config_json","{\"steps\":[]}")));
        long cid=tx.execute(status->catalog.save("courseware",null,payload));
        assertEquals(mid,((Number)db.one(BizTable.READ,cid).get("dubbing_material_id")).longValue());
        Object snapshot=db.versions(cid).get(0).get("content_snapshot_json");String saved=snapshot instanceof byte[] bytes?new String(bytes,java.nio.charset.StandardCharsets.UTF_8):snapshot.toString();assertTrue(saved.contains("extension"));assertTrue(catalog.versions(cid).get(0).get("content_snapshot_json").toString().contains("extension"));
        payload.put("courseware_name","跨册课件");payload.put("volume",2);
        assertThrows(ServiceException.class,()->tx.execute(status->catalog.save("courseware",null,payload)));
        assertEquals(1,db.count(new BizQuery(BizTable.COURSEWARE).eq("subject_code","READING")));
    }
    @Test void learningAccessRejectsOtherStudentsAndUnassignedTeachers() {
        student();assertThrows(ServiceException.class,()->support.student(202,false));
        assertThrows(ServiceException.class,()->support.classroom(1,true));
        when(access.student(any())).thenReturn(false);when(access.teacher(any())).thenReturn(true);
        assertThrows(ServiceException.class,()->support.classroom(1,false));
    }
    @Test void defaultGroupsFillFourAndTransfersPreserveOldMembership() {
        var settings=mock(ISysConfigService.class);when(settings.selectConfigByKey("biz.group.size")).thenReturn("4");
        var grouping=new GroupingService(jdbc,db,settings);
        for(long uid=201;uid<=205;uid++){final long id=uid;tx.executeWithoutResult(status->grouping.join(id,1,"test"));}
        assertEquals(2,classroom.groups(1).size());assertEquals(5,jdbc.queryForObject("SELECT COUNT(*) FROM biz_student_group WHERE is_current='1'",Integer.class));
        tx.executeWithoutResult(status->grouping.join(201,1,"test"));assertEquals(5,jdbc.queryForObject("SELECT COUNT(*) FROM biz_student_group",Integer.class));
    }
    @Test void examTicketsAreStudentBoundAndScoringUsesOnlyProviderMarks() {
        var p=new LinkedHashMap<String,Object>(Map.of("volume",1,"title","评分素材","material_text","text","material_tag","03","video_url","https://example.com/a.mp4","status","0"));
        tx.execute(status->dubbing.save("materials",null,p));student();
        DubbingScorer provider=mock(DubbingScorer.class);when(provider.evaluate(anyString(),anyString())).thenReturn(new DubbingScorer.Result(18,17,13,12,12,13));
        org.springframework.test.util.ReflectionTestUtils.setField(dubbing,"scorers",List.of(provider));
        var material=tx.execute(status->dubbing.draw(1)).get(0);
        var answer=new LinkedHashMap<String,Object>(Map.of("material_id",material.get("material_id"),"exam_ticket",material.get("exam_ticket"),"dubbing_type","2","audio_url","https://example.com/student.mp3","total_score",100));
        var result=tx.execute(status->dubbing.score(answer));assertEquals(85,result.get("total_score"));assertEquals(2,result.get("star_num"));
        tx.execute(status->dubbing.score(answer));verify(provider,times(1)).evaluate(anyString(),anyString());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM biz_student_dubbing_record",Integer.class));
        actor=new SysUser(202L);actor.setUserName("student202");assertThrows(ServiceException.class,()->tx.execute(status->dubbing.score(answer)));
    }
    @Test void remindersAreDeduplicatedAndOnlyAddressedToSchoolManagers() {
        jdbc.update("INSERT INTO sys_user_role(user_id,role_id) VALUES(201,101)");
        jdbc.update("UPDATE biz_school SET expire_time=? WHERE school_id=11000",java.sql.Timestamp.from(Instant.now().plusSeconds(7*86400)));
        var maintenance=new EducationMaintenance(jdbc);tx.executeWithoutResult(status->maintenance.expire());tx.executeWithoutResult(status->maintenance.expire());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM biz_school_expiry_notice",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM biz_notice_recipient WHERE user_id=201",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM biz_notice_recipient WHERE user_id=202",Integer.class));
    }
    @Test void archiveDisablesStudentsWithoutDeletingHistory() {
        tx.executeWithoutResult(status->classroom.archive(List.of(1L)));assertEquals("ARCHIVED",db.one(BizTable.CLASS,1).get("status"));
        assertEquals(5,jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE status='1'",Integer.class));assertEquals(5,jdbc.queryForObject("SELECT COUNT(*) FROM biz_student_extend WHERE del_flag='0'",Integer.class));
    }
}
