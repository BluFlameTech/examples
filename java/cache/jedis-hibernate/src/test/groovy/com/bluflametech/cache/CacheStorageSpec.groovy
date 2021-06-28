package com.bluflametech.cache

import com.bluflametech.test.UnitTest
import org.hibernate.cache.CacheException
import org.hibernate.engine.spi.SharedSessionContractImplementor
import redis.embedded.RedisServer
import spock.lang.Specification

@UnitTest
class CacheStorageSpec extends Specification {
  private static RedisServer redisServer = new RedisServer()

  private JedisRegionFactory jedisRegionFactory

  def setupSpec() {
    redisServer.start()
  }

  def cleanupSpec() {
    redisServer.stop()
  }

  def setup() {
    jedisRegionFactory = new JedisRegionFactory()
  }

  def 'CacheStorage throws a CacheException if CacheManager is not connected'() {
    given:

    String regionName = 'foo'
    CacheManager cacheManager = Mock(CacheManager)

    when:

    new CacheStorage(regionName, cacheManager)

    then:

    1 * cacheManager.connected >> false

    thrown CacheException
  }

  def 'getFromCache gets an object from cache that was previously stored in cache given its key and region name'() {
    given:

    String regionName = 'foo'
    String key = 'test'
    String object = 'Test object to store in Redis'
    CacheStorage storage = new CacheStorage(regionName, new JedisCacheManager())

    when:

    storage.putIntoCache(key, object, Mock(SharedSessionContractImplementor))
    String cachedObject = storage.getFromCache(key, Mock(SharedSessionContractImplementor))

    then:

    cachedObject == object
    storage.contains(key)
  }

  def 'getFromCache returns null when the object is not cached'() {
    given:

    String regionName = 'bar'
    String key = 'test'
    CacheStorage storage = new CacheStorage(regionName, new JedisCacheManager())

    when:

    String cachedObject = storage.getFromCache(key, Mock(SharedSessionContractImplementor))

    then:

    cachedObject == null
  }

  def 'release() purges the entire region from cache'() {
    given:

    String regionName = 'foo'
    CacheStorage storage = new CacheStorage(regionName, new JedisCacheManager())

    when:

    storage.putIntoCache('foo', 'bar', Mock(SharedSessionContractImplementor))
    storage.putIntoCache('hello', 'world', Mock(SharedSessionContractImplementor))
    storage.release()

    then:

    !storage.contains('foo')
    !storage.contains('hello')
    try (Cache cache = storage.cacheManager.cache) {
      !cache.regionExists(regionName)
    }
  }

  def 'evictData with key parameter only purges the specified objects from the region cache'() {
    given:

    String regionName = 'foo'
    CacheStorage storage = new CacheStorage(regionName, new JedisCacheManager())

    when:

    storage.putIntoCache('foo', 'bar', Mock(SharedSessionContractImplementor))
    storage.putIntoCache('hello', 'world', Mock(SharedSessionContractImplementor))
    storage.evictData('foo')

    then:

    !storage.contains('foo')
    storage.contains('hello')
    try (Cache cache = storage.cacheManager.cache) {
      cache.regionExists(regionName)
    }
  }
}
