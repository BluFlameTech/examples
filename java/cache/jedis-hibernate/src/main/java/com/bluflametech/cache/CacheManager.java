package com.bluflametech.cache;

/**
 * Manages connections to cache, like a pool.
 */
public interface CacheManager {
  Cache getCache();

  void returnCache(Cache cache);

  boolean isConnected();

  void shutdown();
}
