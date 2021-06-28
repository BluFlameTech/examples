package com.bluflametech.cache;

import org.hibernate.cache.CacheException;
import redis.clients.jedis.Jedis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.Optional;

/**
 * Cache implementation using Jedis client for Redis.
 */
public class JedisCache implements Cache {

  private JedisCacheManager cacheManager;
  Jedis jedis;

  JedisCache(JedisCacheManager cacheManager, Jedis jedis) {
    this.cacheManager = cacheManager;
    this.jedis = jedis;
  }

  @Override
  public void put(String regionName, Object key, Object value) {
    try {
      jedis.hset(regionName, convertObjectToString(key), convertObjectToString(value));
    } catch (IOException exception) {
      throw new CacheException("Unable to serialize object " + key.toString() + " into a String for cache storage");
    }
  }

  @Override
  public Object get(String regionName, Object key) {
    try {
      Optional<String> cachedObject = Optional.ofNullable(jedis.hget(regionName, convertObjectToString(key)));
      if (cachedObject.isPresent()) {
        return convertStringToObject(cachedObject.get());
      }
      return null;
    } catch (IOException | ClassNotFoundException exception) {
      throw new CacheException("Unable to deserialize cached object " + key.toString(), exception);
    }
  }

  @Override
  public boolean regionExists(String regionName) {
    return this.jedis.exists(regionName);
  }

  @Override
  public boolean containsObject(String regionName, Object key) {
    try {
      return this.jedis.hexists(regionName, convertObjectToString(key));
    } catch (IOException exception) {
      throw new CacheException("Unable to eval containsObject; region: " + regionName + " key: " + key, exception);
    }
  }

  @Override
  public void purgeRegion(String regionName) {
    this.jedis.del(regionName);
  }

  @Override
  public void purge(String regionName, Object key) {
    try {
      this.jedis.hdel(regionName, convertObjectToString(key));
    } catch (IOException exception) {
      throw new CacheException("Unable to purge; region: " + regionName + " key: " + key, exception);
    }
  }

  @Override
  public void close() throws Exception {
    this.cacheManager.returnCache(this);
    this.jedis = null;
    this.cacheManager = null;
  }

  static String convertObjectToString(Object obj) throws IOException {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
      out.writeObject(obj);
      out.flush();
      return Base64.getEncoder().encodeToString(bos.toByteArray());
    }
  }

  static Object convertStringToObject(String str) throws IOException, ClassNotFoundException {
    try (ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(str.getBytes())); ObjectInput in = new ObjectInputStream(bis)) {
      return in.readObject();
    }
  }
}
