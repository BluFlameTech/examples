package com.bluflametech.cache

import com.bluflametech.test.IntegrationTest
import org.hibernate.cache.CacheException

import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisSentinelPool
import redis.clients.jedis.util.Pool

import spock.lang.Specification

/**
 * Start redis in sentinel mode (i.e. redis-server sentinel.conf --sentinel) with defaults prior to testing to run these tests.
 */
@IntegrationTest
class JedisCacheManagerSpec extends Specification {

  def 'sentinel configuration creates JedisSentinelPool'() {
    given:

    def properties = [
        'hibernate.cache.redis.sentinel.master': 'mymaster',
        'hibernate.cache.redis.sentinel.nodes': 'localhost:26379'
    ]

    when:

    JedisCacheManager cacheManager = new JedisCacheManager(properties)

    then:

    cacheManager.jedisPool instanceof JedisSentinelPool
  }

  def 'sentinel configuration with password creates JedisSentinelPool'() {
    given:

    def properties = [
        'hibernate.cache.redis.sentinel.master': 'mymaster',
        'hibernate.cache.redis.sentinel.nodes': 'localhost:26379',
        'hibernate.cache.redis.sentinel.password': 'passw0rd'
    ]

    when:

    JedisCacheManager cacheManager = new JedisCacheManager(properties)

    then:

    cacheManager.jedisPool instanceof JedisSentinelPool
  }

  def 'error is thrown on getCache when pool is not connected'() {
    given:

    def properties = [
        'hibernate.cache.redis.sentinel.master': 'mymaster',
        'hibernate.cache.redis.sentinel.nodes': 'localhost:26379'
    ]

    JedisCacheManager cacheManager = new JedisCacheManager(properties)
    cacheManager.jedisPool = Stub(Pool<Jedis>) {
      getResource() >> Spy(Jedis) {
        isConnected() >> false
      }
    }

    when:

    cacheManager.cache

    then:

    thrown CacheException
  }

  def 'attempting to return Cache that is not JedisCache results in a CacheException'() {
    given:

    def properties = [
        'hibernate.cache.redis.sentinel.master': 'mymaster',
        'hibernate.cache.redis.sentinel.nodes': 'localhost:26379'
    ]

    JedisCacheManager cacheManager = new JedisCacheManager(properties)
    Cache cache = Mock(Cache)

    when:

    cacheManager.returnCache(cache)

    then:

    thrown CacheException
  }
}
