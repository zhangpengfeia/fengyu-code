package com.ruoyi.education;
import com.ruoyi.education.service.*;
import com.ruoyi.education.domain.*;
import com.ruoyi.education.mapper.BizMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.redis.RedisCache;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.util.*;

class GameGatewayTest {
    @TempDir Path root;
    GameService games;BizMapper db;AccessService access;RedisCache redis;
    final String sid="a".repeat(43),ticket="b".repeat(43);
    Map<String,Object> game;
    @SuppressWarnings("unchecked") @BeforeEach void setup() throws Exception {
        db=mock(BizMapper.class);access=mock(AccessService.class);redis=mock(RedisCache.class);
        games=new GameService(db,access,mock(CatalogService.class),redis,new ObjectMapper(),root.toString(),"http://127.0.0.1:8081");
        var u=new SysUser(20001L);u.setPassword("hash");when(access.active(20001)).thenReturn(u);when(access.game(any(),any())).thenReturn(true);
        when(access.permits(any(),eq("biz:game:preview"))).thenReturn(true);
        game=Map.of("game_id",1L,"game_code","planet","version_no","v1","status","PUBLISHED");when(db.one(BizTable.GAME,1)).thenReturn(game);
        when(redis.hasKey("login_tokens:parent")).thenReturn(true);
        var data=new ObjectMapper().writeValueAsString(Map.of("uid",20001,"gid",1,"code","planet","passwordHash","hash","parentToken","parent"));
        when(redis.<String>getCacheObject("edu:game-session:"+sid)).thenReturn(data);when(redis.<String>getAndDelete("edu:ticket:"+ticket)).thenReturn(data,(String)null);
        Files.createDirectories(root.resolve("planet/v1"));Files.writeString(root.resolve("planet/v1/index.html"),"protected");
    }
    @Test void ticketExchangeIsSingleUse() {assertEquals(43,games.exchange("planet",ticket).length());assertThrows(ServiceException.class,()->games.exchange("planet",ticket));}
    @Test void sessionIsBoundToGame() {assertThrows(ServiceException.class,()->games.asset("another-game",sid,"index.html"));}
    @Test void everyAssetRechecksPermission() {assertDoesNotThrow(()->games.asset("planet",sid,"index.html"));when(access.game(any(),any())).thenReturn(false);assertThrows(ServiceException.class,()->games.asset("planet",sid,"index.html"));}
    @Test void passwordResetInvalidatesGameSession() {when(access.active(20001)).thenAnswer(invocation->{var u=new SysUser(20001L);u.setPassword("new-hash");return u;});assertThrows(ServiceException.class,()->games.asset("planet",sid,"index.html"));}
    @Test void agentSessionRechecksParentAndPermission() {
        assertEquals(20001L,games.sessionUser("planet",sid).getUserId());
        when(access.game(any(),any())).thenReturn(false);
        assertThrows(ServiceException.class,()->games.sessionUser("planet",sid));
        when(access.game(any(),any())).thenReturn(true);
        when(redis.hasKey("login_tokens:parent")).thenReturn(false);
        assertThrows(ServiceException.class,()->games.sessionUser("planet",sid));
    }
    @Test void agentSessionCannotSelectAnotherGameOrSkipAuthentication() {
        assertThrows(ServiceException.class,()->games.sessionUser("another-game",sid));
        assertThrows(ServiceException.class,()->games.sessionUser("planet",null));
    }
    @Test void parentLogoutInvalidatesGameSession() {when(redis.hasKey("login_tokens:parent")).thenReturn(false);assertThrows(ServiceException.class,()->games.asset("planet",sid,"index.html"));}
    @Test void currentMenuPermissionIsRecheckedForEveryAsset() {assertDoesNotThrow(()->games.asset("planet",sid,"index.html"));when(access.permits(any(),eq("biz:game:preview"))).thenReturn(false);assertThrows(ServiceException.class,()->games.asset("planet",sid,"index.html"));}
    @Test void traversalAndSymlinkCannotEscapeVersionRoot() throws Exception {
        assertThrows(ServiceException.class,()->games.asset("planet",sid,"../index.html"));
        Path outside=Files.createTempFile("education-outside-",".html");
        try {Files.writeString(outside,"secret");Files.createSymbolicLink(root.resolve("planet/v1/leak.html"),outside);assertThrows(ServiceException.class,()->games.asset("planet",sid,"leak.html"));}finally{Files.deleteIfExists(outside);}
    }
}
