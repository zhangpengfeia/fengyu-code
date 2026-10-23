package com.ruoyi.education;
import com.ruoyi.education.mapper.*;
import com.ruoyi.education.domain.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.h2.jdbcx.JdbcDataSource;
import org.apache.ibatis.session.*;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.FileSystemResource;
import java.nio.file.Path;
import java.util.*;

class MapperIntegrationTest {
    SqlSession session;BizMapper db;
    @BeforeEach void setup() throws Exception {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:t"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE,KEY");
        try(var c=ds.getConnection()){ScriptUtils.executeSqlScript(c,new FileSystemResource(Path.of("../ruoyi-admin/src/main/resources/db/development.sql")));}
        var config=new Configuration(new Environment("test",new JdbcTransactionFactory(),ds));config.addMapper(BizMapper.class);
        session=new SqlSessionFactoryBuilder().build(config).openSession(true);db=session.getMapper(BizMapper.class);
    }
    @AfterEach void close(){if(session!=null)session.close();}
    @Test void reviewedMigrationsAndPaginationUseSameFilters() {
        var q=new BizQuery(BizTable.GAME);q.limit=1;assertEquals(2,db.count(q));assertEquals(1,db.list(q).size());
        q.eq("access_policy","STUDENT");assertEquals(1,db.count(q));assertEquals("number-match",db.list(q).get(0).get("game_code"));
    }
    @Test void generatedKeysAndLogicalDeletionWork() {
        var v=new LinkedHashMap<String,Object>(Map.of("game_code","test-game","game_name","测试","source_type","INTERNAL","entry_path","index.html","access_policy","TEACHER_ONLY"));
        db.insert(BizTable.GAME,v,"test");long id=((Number)v.get("generatedId")).longValue();assertNotNull(db.one(BizTable.GAME,id));db.delete(BizTable.GAME,id,"test");assertNull(db.one(BizTable.GAME,id));
    }
    @Test void duplicateCodesStayReservedAfterLogicalDeletion() {
        db.delete(BizTable.GAME,2,"test");var v=new LinkedHashMap<String,Object>(Map.of("game_code","number-match","game_name","冲突","source_type","INTERNAL","entry_path","index.html"));
        assertThrows(Exception.class,()->db.insert(BizTable.GAME,v,"test"));
    }
    @Test void untrustedColumnNamesNeverBecomeSql() {
        var q=new BizQuery(BizTable.GAME).eq("game_id OR 1=1",1);assertThrows(Exception.class,()->db.list(q));
        var values=new LinkedHashMap<String,Object>(Map.of("status='PUBLISHED' --","x"));assertThrows(Exception.class,()->db.update(BizTable.GAME,1,values,"test"));
    }
    @Test void emptySchoolScopeCannotReadWholeDatabase() {
        var q=new HashMap<String,Object>();q.put("schools",List.of());q.put("limit",100);q.put("offset",0);assertEquals(0,db.countUsers(q));assertTrue(db.scopedUsers(q).isEmpty());
    }
    @Test void staleEditorCannotOverwriteANewerRevision() {
        var v=new LinkedHashMap<String,Object>(Map.of("game_name","新名称","expected_row_version",0));
        assertEquals(1,db.update(BizTable.GAME,1,v,"test"));
        v.put("game_name","过期页面覆盖");assertEquals(0,db.update(BizTable.GAME,1,v,"test"));
        assertEquals("新名称",db.one(BizTable.GAME,1).get("game_name"));
    }
}
