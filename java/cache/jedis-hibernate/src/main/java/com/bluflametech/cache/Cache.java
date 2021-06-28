package com.bluflametech.cache;

/**
 * A common/canonical cache interface that supports try w/resources via AutoClosable.
 */
public interface Cache extends AutoCloseable {
  void put(String regionName, Object key, Object value);

  Object get(String regionName, Object key);

  boolean containsObject(String regionName, Object key);

  boolean regionExists(String regionName);

  void purgeRegion(String regionName);

  void purge(String regionName, Object key);
}
