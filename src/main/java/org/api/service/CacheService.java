package org.api.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CacheService {

    // TTL padrão (10 segundos)
    private static final long TTL_MILLIS = 10_000L;

    private static class CacheEntry {
        final Object value;
        final long expiresAt;

        CacheEntry(Object value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }

    // ConcurrentHashMap para ser thread-safe
    private final Map<String, CacheEntry> cacheMap = new ConcurrentHashMap<>();


    // Adiciona/atualiza um valor no cache com TTL padrão
    public void put(String key, Object value) {
        put(key, value, TTL_MILLIS);
    }

    // Adiciona/atualiza um valor no cache com TTL customizável (ms)
    public void put(String key, Object value, long ttlMillis) {
        long expiresAt = System.currentTimeMillis() + TTL_MILLIS;
        cacheMap.put(key, new CacheEntry(value, expiresAt));
    }

    // Recupera um valor do cache (retorna vazio se expirado)
    public Optional<Object> get(String key) {
        CacheEntry entry = cacheMap.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (System.currentTimeMillis() >= entry.expiresAt) {
            cacheMap.remove(key);
            return Optional.empty();
        }
        return Optional.ofNullable(entry.value);
    }

    // Remove um valor do cache
    public void remove(String key) {
        cacheMap.remove(key);
    }

    // Limpa todo o cache
    public void clear() {
        cacheMap.clear();
    }
}
