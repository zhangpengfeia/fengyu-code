package com.ruoyi.common.core.redis;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;

/** Compatibility facade: memory for standalone demos, Redis for shared multi-instance state. */
@Component
@SuppressWarnings({"unchecked", "rawtypes"})
public class RedisCache {
    @Autowired(required = false) public RedisTemplate redisTemplate;
    private final LocalCacheStore local;
    public RedisCache(@Value("${education.cache.type:redis}") String type) {
        if (!Set.of("memory", "redis").contains(type)) throw new IllegalArgumentException("education.cache.type must be memory or redis");
        local = "memory".equals(type) ? new LocalCacheStore() : null;
    }
    public boolean isLocal() { return local != null; }
    @PreDestroy public void close() { if (local != null) local.close(); }
    public <T> void setCacheObject(String key, T value) {
        if (isLocal()) local.put(key, value, -1); else redisTemplate.opsForValue().set(key, value);
    }
    public <T> void setCacheObject(String key, T value, Integer timeout, TimeUnit unit) {
        if (isLocal()) local.put(key, value, unit.toMillis(timeout)); else redisTemplate.opsForValue().set(key, value, timeout, unit);
    }
    public <T> void set(String key, T value, Duration ttl) {
        if (isLocal()) local.put(key, value, ttl.toMillis()); else redisTemplate.opsForValue().set(key, value, ttl);
    }
    public boolean expire(String key, long timeout) { return expire(key, timeout, TimeUnit.SECONDS); }
    public boolean expire(String key, Duration ttl) { return expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS); }
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return isLocal() ? local.expire(key, unit.toMillis(timeout)) : Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }
    public long getExpire(String key) { return isLocal() ? local.ttl(key) : redisTemplate.getExpire(key); }
    public Boolean hasKey(String key) { return isLocal() ? local.has(key) : redisTemplate.hasKey(key); }
    public <T> T getCacheObject(String key) { return isLocal() ? local.get(key) : (T) redisTemplate.opsForValue().get(key); }
    public <T> T getAndDelete(String key) { return isLocal() ? local.take(key) : (T) redisTemplate.opsForValue().getAndDelete(key); }
    public boolean deleteObject(String key) { return isLocal() ? local.delete(key) : Boolean.TRUE.equals(redisTemplate.delete(key)); }
    public boolean deleteObject(Collection keys) {
        if (!isLocal()) return redisTemplate.delete(keys) > 0;
        boolean any = false; for (Object key : keys) any |= local.delete(String.valueOf(key)); return any;
    }
    public long incrementWindow(String key, Duration ttl) {
        if (isLocal()) return local.incrementWindow(key, ttl.toMillis());
        // Atomic first expiry avoids immortal rate-limit keys if the client disconnects.
        var script = new DefaultRedisScript<Long>("local n=redis.call('incr',KEYS[1]); if n==1 then redis.call('pexpire',KEYS[1],ARGV[1]); end; return n", Long.class);
        return ((Number) redisTemplate.execute(script, List.of(key), ttl.toMillis())).longValue();
    }
    public synchronized <T> long setCacheList(String key, List<T> values) {
        if (!isLocal()) { Long n=redisTemplate.opsForList().rightPushAll(key, values); return n == null ? 0 : n; }
        List<T> list = getCacheList(key); list.addAll(values); local.put(key, list, -1); return list.size();
    }
    public <T> List<T> getCacheList(String key) {
        if (!isLocal()) return redisTemplate.opsForList().range(key, 0, -1);
        List<T> value=local.get(key); return value == null ? new ArrayList<>() : value;
    }
    public synchronized <T> void setCacheSet(String key, Set<T> values) {
        if (!isLocal()) { for (T value : values) redisTemplate.opsForSet().add(key, value); return; }
        Set<T> set=getCacheSet(key); set.addAll(values); local.put(key, set, -1);
    }
    public <T> Set<T> getCacheSet(String key) {
        if (!isLocal()) return redisTemplate.opsForSet().members(key);
        Set<T> value=local.get(key); return value == null ? new HashSet<>() : value;
    }
    public synchronized <T> void setCacheMap(String key, Map<String,T> values) {
        if (values == null) return;
        if (!isLocal()) { redisTemplate.opsForHash().putAll(key, values); return; }
        Map<String,T> map=getCacheMap(key); map.putAll(values); local.put(key, map, -1);
    }
    public <T> Map<String,T> getCacheMap(String key) {
        if (!isLocal()) return redisTemplate.opsForHash().entries(key);
        Map<String,T> value=local.get(key); return value == null ? new HashMap<>() : value;
    }
    public <T> void setCacheMapValue(String key, String field, T value) { setCacheMap(key, Collections.singletonMap(field,value)); }
    public <T> T getCacheMapValue(String key, String field) { return isLocal() ? (T) getCacheMap(key).get(field) : (T) redisTemplate.opsForHash().get(key,field); }
    public <T> List<T> getMultiCacheMapValue(String key, Collection<Object> fields) {
        if (!isLocal()) return redisTemplate.opsForHash().multiGet(key, fields);
        Map<String,T> map=getCacheMap(key); List<T> result=new ArrayList<>(); for(Object field:fields) result.add(map.get(field)); return result;
    }
    public synchronized boolean deleteCacheMapValue(String key, String field) {
        if (!isLocal()) return redisTemplate.opsForHash().delete(key,field)>0;
        Map<String,Object> map=getCacheMap(key); boolean found=map.containsKey(field); map.remove(field); local.put(key,map,-1); return found;
    }
    public Collection<String> keys(String pattern) { return isLocal() ? local.keys(pattern) : redisTemplate.keys(pattern); }
}
