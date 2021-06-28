package com.bluflametech.cache;

import org.hibernate.cache.CacheException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.util.Pool;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * CacheManager implementation for Jedis.
 * This class effectively creates a Pool&lt;Jedis&gt; facade.
 */
public class JedisCacheManager implements CacheManager {

  private static final String CONFIG_PREFIX = "hibernate.cache.redis.";
  private static final String SENTINEL_CONFIG_PREFIX = CONFIG_PREFIX + "sentinel.";
  private static final String STANDALONE_CONFIG_PREFIX = CONFIG_PREFIX + "standalone.";

  Pool<Jedis> jedisPool;

  public JedisCacheManager() {
    this(Map.of());
  }

  /**
   * Constructor creates a Jedis connection pool from config - either standalone or sentinel.
   *
   * @param properties hibernate properties (e.g. from application.yml)
   */
  public JedisCacheManager(Map<String, Object> properties) {
    if (properties.keySet().stream().anyMatch(key -> key.toString().startsWith(SENTINEL_CONFIG_PREFIX))) {
      String master = (String) properties.get(SENTINEL_CONFIG_PREFIX + "master");
      Set<String> sentinels = Set.of(((String) properties.get(SENTINEL_CONFIG_PREFIX + "nodes")).split(",\\s?"));
      String password = (String) properties.get(SENTINEL_CONFIG_PREFIX + "password");
      this.jedisPool = Optional.ofNullable(password)
          .map(pass -> new JedisSentinelPool(master, sentinels, pass))
          .orElse(new JedisSentinelPool(master, sentinels));
      return;
    }

    if (properties.keySet().stream().anyMatch(key -> key.toString().startsWith(STANDALONE_CONFIG_PREFIX))) {
      String host = (String) properties.get(STANDALONE_CONFIG_PREFIX + "host");
      String port = (String) properties.get(STANDALONE_CONFIG_PREFIX + "port");
      this.jedisPool = new JedisPool(host, Integer.parseInt(port));
      return;
    }

    this.jedisPool = new JedisPool();
  }

  @Override
  public Cache getCache() {
    for (int x = 0; x < 3; x++) {
      Jedis jedis = this.jedisPool.getResource();
      if (jedis.isConnected()) {
        return new JedisCache(this, jedis);
      }
    }
    throw new CacheException("Jedis connection pool is unable to get a connection to Redis!");
  }

  @Override
  public void returnCache(Cache cache) {
    if (!(cache instanceof JedisCache)) {
      throw new CacheException("Attempted to return Cache object of type other than JedisCache to JedisCacheManager!");
    }
    JedisCache jedisCache = (JedisCache) cache;
    this.jedisPool.returnResource(jedisCache.jedis);
  }

  @Override
  public boolean isConnected() {
    return !this.jedisPool.isClosed();
  }

  @Override
  public void shutdown() {
    this.jedisPool.close();
  }
}
