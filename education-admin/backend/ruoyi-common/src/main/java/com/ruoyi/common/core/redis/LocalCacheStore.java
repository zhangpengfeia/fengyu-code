package com.ruoyi.common.core.redis;

import java.io.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/** Single-process expiring store. Serialized snapshots prevent principal mutation leaking across requests. */
public class LocalCacheStore implements AutoCloseable {
    private record Entry(byte[] value, long expiresAt) {}
    private final Map<String, Entry> entries = new HashMap<>();
    private final Clock clock;
    private final ScheduledExecutorService cleanup;
    public LocalCacheStore() { this(Clock.systemUTC()); }
    public LocalCacheStore(Clock clock) {
        this.clock = clock;
        cleanup = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "education-local-cache-cleanup");
            thread.setDaemon(true); return thread;
        });
        cleanup.scheduleWithFixedDelay(this::purge, 30, 30, TimeUnit.SECONDS);
    }
    private synchronized void purge() { entries.entrySet().removeIf(e -> expired(e.getValue())); }
    private boolean expired(Entry e) { return e.expiresAt != Long.MAX_VALUE && e.expiresAt <= clock.millis(); }
    private Entry live(String key) {
        Entry e = entries.get(key);
        if (e != null && expired(e)) { entries.remove(key); return null; }
        return e;
    }
    private byte[] encode(Object value) {
        try (var bytes = new ByteArrayOutputStream(); var out = new ObjectOutputStream(bytes)) {
            out.writeObject(value); out.flush(); return bytes.toByteArray();
        } catch (IOException e) { throw new IllegalArgumentException("Local cache value must be serializable", e); }
    }
    @SuppressWarnings("unchecked") private <T> T decode(Entry e) {
        if(e == null) return null;
        try (var in = new ObjectInputStream(new ByteArrayInputStream(e.value))) {
            return (T) in.readObject();
        } catch (IOException | ClassNotFoundException ex) { throw new IllegalStateException("Invalid local cache snapshot", ex); }
    }
    public synchronized void put(String key, Object value, long ttlMillis) {
        entries.put(key, new Entry(encode(value), ttlMillis < 0 ? Long.MAX_VALUE : clock.millis() + ttlMillis));
    }
    public synchronized <T> T get(String key) { return decode(live(key)); }
    public synchronized <T> T take(String key) { Entry e = live(key); entries.remove(key); return decode(e); }
    public synchronized boolean delete(String key) { return entries.remove(key) != null; }
    public synchronized boolean has(String key) { return live(key) != null; }
    public synchronized boolean expire(String key, long millis) {
        Entry e = live(key); if (e == null) return false;
        entries.put(key, new Entry(e.value, clock.millis() + millis)); return true;
    }
    public synchronized long ttl(String key) {
        Entry e = live(key); return e == null ? -2 : e.expiresAt == Long.MAX_VALUE ? -1 : Math.max(0, (e.expiresAt - clock.millis()) / 1000);
    }
    public synchronized Set<String> keys(String glob) {
        purge(); StringBuilder regex = new StringBuilder("^");
        for (char c : glob.toCharArray()) regex.append(c == '*' ? ".*" : c == '?' ? "." : Pattern.quote(String.valueOf(c)));
        Pattern pattern = Pattern.compile(regex.append('$').toString());
        Set<String> found = new HashSet<>();
        entries.keySet().stream().filter(k -> pattern.matcher(k).matches()).forEach(found::add); return found;
    }
    /** Increment and first expiry are atomic, retaining the original fixed window. */
    public synchronized long incrementWindow(String key, long ttlMillis) {
        Entry old = live(key); long count = old == null ? 1 : ((Number) decode(old)).longValue() + 1;
        entries.put(key, new Entry(encode(count), old == null ? clock.millis() + ttlMillis : old.expiresAt)); return count;
    }
    @Override public void close() { cleanup.shutdownNow(); synchronized(this) { entries.clear(); } }
}
