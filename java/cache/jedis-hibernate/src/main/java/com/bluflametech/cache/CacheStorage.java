package com.bluflametech.cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Implementation of DomainStorageAccess for CacheManager facilitating interaction with Jedis client.
 */
public class CacheStorage implements DomainDataStorageAccess {

  private final String regionName;
  private final CacheManager cacheManager;

  /**
   * CacheStorage constructor creates CacheStorage instance from regionName and CacheManager.
   *
   * @param regionName   a unique namespace for cached objects
   * @param cacheManager an instance of CacheManager
   */
  public CacheStorage(String regionName, CacheManager cacheManager) {
    this.regionName = regionName;
    this.cacheManager = cacheManager;
    if (!cacheManager.isConnected()) {
      throw new CacheException("CacheManager is not connected to cache!");
    }

    try (Cache cache = cacheManager.getCache()) {
      if (!cache.regionExists(regionName)) {
        cache.put(regionName, "", "");
      }
    } catch (Exception exception) {
      throw new CacheException(exception);
    }
  }

  @Override
  public Object getFromCache(Object key, SharedSessionContractImplementor sharedSessionContractImplementor) {
    try (Cache cache = cacheManager.getCache()) {
      return cache.get(regionName, key);
    } catch (Exception exception) {
      throw new CacheException(exception);
    }
  }

  @Override
  public void putIntoCache(Object key, Object value, SharedSessionContractImplementor sharedSessionContractImplementor) {
    try (Cache cache = cacheManager.getCache()) {
      cache.put(regionName, key, value);
    } catch (Exception exception) {
      throw new CacheException(exception);
    }
  }

  @Override
  public boolean contains(Object key) {
    try (Cache cache = cacheManager.getCache()) {
      return cache.containsObject(regionName, key);
    } catch (Exception exception) {
      throw new CacheException(exception);
    }
  }

  @Override
  public void evictData() {
    try (Cache cache = cacheManager.getCache()) {
      cache.purgeRegion(regionName);
    } catch (Exception exception) {
      throw new CacheException(exception);
    }
  }

  @Override
  public void evictData(Object key) {
    try (Cache cache = cacheManager.getCache()) {
      cache.purge(regionName, key);
    } catch (Exception exception) {
      throw new CacheException(exception);
    }
  }

  @Override
  public void release() {
    evictData();
  }

  String getRegionName() {
    return regionName;
  }

  CacheManager getCacheManager() {
    return cacheManager;
  }
}
