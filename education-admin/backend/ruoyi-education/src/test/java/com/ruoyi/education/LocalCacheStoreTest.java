package com.ruoyi.education;

import com.ruoyi.common.core.redis.LocalCacheStore;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class LocalCacheStoreTest {
    static class TestClock extends Clock {
        final AtomicLong now = new AtomicLong(10000);
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return Instant.ofEpochMilli(now.get()); }
        public long millis() { return now.get(); }
    }
    @Test void expirationAndMissingKeys() {
        var clock=new TestClock();
        try(var cache=new LocalCacheStore(clock)) {
            assertEquals(-2,cache.ttl("absent"));
            cache.put("ticket","one-use",1000); assertEquals(1,cache.ttl("ticket"));
            clock.now.addAndGet(1000);
            assertNull(cache.get("ticket")); assertNull(cache.take("ticket")); assertFalse(cache.has("ticket"));
            assertFalse(cache.expire("ticket",1000));
        }
    }
    @Test void concurrentTicketConsumptionHasExactlyOneWinner() throws Exception {
        try(var cache=new LocalCacheStore()) {
            cache.put("ticket","value",10000);
            ExecutorService pool=Executors.newFixedThreadPool(8);
            try {
                var tasks=new ArrayList<Callable<String>>();
                for(int i=0;i<100;i++) tasks.add(()->cache.take("ticket"));
                int winners=0;for(var result:pool.invokeAll(tasks)) if(result.get()!=null)winners++;
                assertEquals(1,winners);
            } finally {pool.shutdownNow();}
        }
    }
    @Test void fixedRateWindowRetainsFirstExpiry() {
        var clock=new TestClock();
        try(var cache=new LocalCacheStore(clock)) {
            assertEquals(1,cache.incrementWindow("ip",1000));clock.now.addAndGet(500);
            assertEquals(2,cache.incrementWindow("ip",1000));clock.now.addAndGet(500);
            assertEquals(1,cache.incrementWindow("ip",1000));
        }
    }
    @Test void concurrentRateCounterDoesNotLoseUpdates() throws Exception {
        try(var cache=new LocalCacheStore()) {
            ExecutorService pool=Executors.newFixedThreadPool(8);
            try {
                var tasks=new ArrayList<Callable<Long>>();for(int i=0;i<100;i++)tasks.add(()->cache.incrementWindow("ip",10000));
                Set<Long> counters=new HashSet<>();for(var r:pool.invokeAll(tasks))counters.add(r.get());
                assertEquals(100,counters.size());assertEquals(100L,cache.<Long>get("ip"));
            } finally {pool.shutdownNow();}
        }
    }
    @Test void principalSnapshotsAreIsolatedFromCallerMutation() {
        try(var cache=new LocalCacheStore()) {
            var principal=new LoginUser();var user=new SysUser(1L);user.setPassword("original");principal.setUser(user);
            cache.put("login_tokens:one",principal,10000);principal.getUser().setPassword("caller-mutated");
            LoginUser copy=cache.get("login_tokens:one");assertEquals("original",copy.getUser().getPassword());
            copy.getUser().setPassword("request-mutated");
            assertEquals("original",cache.<LoginUser>get("login_tokens:one").getUser().getPassword());
        }
    }
    @Test void wildcardSearchDoesNotIncludeExpiredSessionsOrTreatDotsAsWildcards() {
        var clock=new TestClock();
        try(var cache=new LocalCacheStore(clock)) {
            cache.put("login_tokens:one","a",-1);cache.put("login_tokens:old","b",10);
            cache.put("config.a","c",-1);cache.put("configXa","d",-1);clock.now.addAndGet(10);
            assertEquals(Set.of("login_tokens:one"),cache.keys("login_tokens:*"));
            assertEquals(Set.of("config.a"),cache.keys("config.*"));
        }
    }
    @Test void facadeSupportsNativeConfigDictionaryAndLogoutOperations() {
        var cache=new RedisCache("memory");
        try {
            cache.setCacheObject("config","enabled");assertEquals(-1,cache.getExpire("config"));
            cache.setCacheList("dict",new ArrayList<>(List.of("first")));cache.setCacheList("dict",List.of("second"));
            assertEquals(List.of("first","second"),cache.getCacheList("dict"));
            cache.setCacheMap("map",Map.of("a",1));cache.setCacheMapValue("map","b",2);
            assertEquals(Arrays.asList(1,2,null),cache.getMultiCacheMapValue("map",List.of("a","b","missing")));
            assertTrue(cache.deleteCacheMapValue("map","a"));assertNull(cache.getCacheMapValue("map","a"));
            cache.setCacheSet("set",Set.of("a"));cache.setCacheSet("set",Set.of("b"));assertEquals(Set.of("a","b"),cache.getCacheSet("set"));
            assertTrue(cache.deleteObject(List.of("config","dict")));assertFalse(cache.hasKey("config"));
            cache.set("ticket","test",Duration.ofSeconds(60));assertEquals("test",cache.getAndDelete("ticket"));assertNull(cache.getAndDelete("ticket"));
        } finally {cache.close();}
    }
}
