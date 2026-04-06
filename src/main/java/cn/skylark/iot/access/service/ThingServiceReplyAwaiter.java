package cn.skylark.iot.access.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class ThingServiceReplyAwaiter {

    private final ConcurrentHashMap<String, CompletableFuture<String>> pending = new ConcurrentHashMap<String, CompletableFuture<String>>();

    public CompletableFuture<String> register(String key) {
        CompletableFuture<String> future = new CompletableFuture<String>();
        CompletableFuture<String> exists = pending.putIfAbsent(key, future);
        return exists == null ? future : exists;
    }

    public String await(CompletableFuture<String> future, int timeoutMs) throws Exception {
        return future.get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public void clear(String key) {
        pending.remove(key);
    }

    public void complete(String key, String result) {
        CompletableFuture<String> future = pending.remove(key);
        if (future != null) {
            future.complete(result);
        }
    }
}

