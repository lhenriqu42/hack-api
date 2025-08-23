
package org.api.service;


import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CacheService {

    private static final long DEFAULT_TTL_SECONDS = 10L;

    private final Cache<String, Object> defaultCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(DEFAULT_TTL_SECONDS, TimeUnit.SECONDS)
            .build();

    // Adiciona/atualiza um valor no cache
    public void put(String key, Object value) {
        defaultCache.put(key, value);
    }

    // Recupera um valor do cache (retorna vazio se expirado)
    public Optional<Object> get(String key) {
        Object value = defaultCache.getIfPresent(key);
        if (value != null) {
            return Optional.of(value);
        }
		return Optional.empty();
    }

    public void remove(String key) {
        defaultCache.invalidate(key);
    }

    public void clear() {
        defaultCache.invalidateAll();
    }
}