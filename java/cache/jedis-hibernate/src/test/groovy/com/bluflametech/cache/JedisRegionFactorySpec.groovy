package com.bluflametech.cache

import com.bluflametech.test.UnitTest
import org.hibernate.boot.registry.StandardServiceRegistry
import org.hibernate.boot.registry.selector.spi.StrategySelector
import org.hibernate.boot.spi.SessionFactoryOptions
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig
import org.hibernate.cache.internal.DefaultCacheKeysFactory
import org.hibernate.cache.spi.CacheKeysFactory
import org.hibernate.cache.spi.support.DomainDataRegionImpl
import org.hibernate.engine.spi.SessionFactoryImplementor
import redis.clients.jedis.exceptions.JedisConnectionException
import redis.embedded.RedisServer
import spock.lang.Specification

@UnitTest
class JedisRegionFactorySpec extends Specification {

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

  def 'cacheExists is false prior to prepareForUse'() {
    given:

    def regionName = 'MyRegionName'
    def regionPrefix = 'foo'

    SessionFactoryOptions options = Mock SessionFactoryOptions

    when:

    boolean cacheExists = jedisRegionFactory.cacheExists(regionName, options)

    then:

    !cacheExists

    2 * options.cacheRegionPrefix >> regionPrefix
  }

  def 'prepareForUse creates a JedisPool for a default standalone redis and a default CacheKeysFactory'() {
    given:

    SessionFactoryOptions settings = Mock SessionFactoryOptions
    StandardServiceRegistry registry = Mock StandardServiceRegistry
    StrategySelector selector = Mock StrategySelector

    when:

    jedisRegionFactory.prepareForUse(settings, [:])

    then:

    1 * settings.serviceRegistry >> registry
    1 * registry.getService(StrategySelector) >> selector
    1 * selector.resolveDefaultableStrategy(CacheKeysFactory, null, DefaultCacheKeysFactory.INSTANCE) >> DefaultCacheKeysFactory.INSTANCE
    jedisRegionFactory.cacheManager.connected
  }

  def 'prepareForUse creates a JedisPool for a configured standalone redis and a default CacheKeysFactory'() {
    given:

    SessionFactoryOptions settings = Mock SessionFactoryOptions
    StandardServiceRegistry registry = Mock StandardServiceRegistry
    StrategySelector selector = Mock StrategySelector

    when:

    jedisRegionFactory.prepareForUse(settings, [
        'hibernate.cache.redis.standalone.host': 'localhost',
        'hibernate.cache.redis.standalone.port': '6379'])

    then:

    1 * settings.serviceRegistry >> registry
    1 * registry.getService(StrategySelector) >> selector
    1 * selector.resolveDefaultableStrategy(CacheKeysFactory, null, DefaultCacheKeysFactory.INSTANCE) >> DefaultCacheKeysFactory.INSTANCE
    jedisRegionFactory.cacheManager.connected
  }

  def 'prepareForUse creates a JedisSentinelPool for standalone redis and a default CacheKeysFactory'() {
    given:

    SessionFactoryOptions settings = Mock SessionFactoryOptions
    StandardServiceRegistry registry = Mock StandardServiceRegistry
    StrategySelector selector = Mock StrategySelector

    when:

    jedisRegionFactory.prepareForUse(settings, [
        'hibernate.cache.redis.sentinel.master': 'mymaster',
        'hibernate.cache.redis.sentinel.nodes': 'localhost:36379'
    ])

    then:

    1 * settings.serviceRegistry >> registry
    1 * registry.getService(StrategySelector) >> selector
    1 * selector.resolveDefaultableStrategy(CacheKeysFactory, null, DefaultCacheKeysFactory.INSTANCE) >> DefaultCacheKeysFactory.INSTANCE

    thrown JedisConnectionException
  }

  def 'createDomainDataStorageAccess returns CacheStorage and sets up the region in cache'() {
    given:

    def regionName = 'MyRegionName'
    def regionPrefix = 'foo'

    SessionFactoryOptions settings = stubSessionFactory()

    DomainDataRegionConfig regionConfig = Mock DomainDataRegionConfig
    DomainDataRegionBuildingContext buildingContext = Mock DomainDataRegionBuildingContext
    SessionFactoryImplementor sessionFactory = Mock SessionFactoryImplementor
    SessionFactoryOptions options = Mock SessionFactoryOptions

    jedisRegionFactory.prepareForUse(settings, [:])

    when:

    CacheStorage storage = jedisRegionFactory.createDomainDataStorageAccess(regionConfig, buildingContext) as CacheStorage
    boolean cacheExists = jedisRegionFactory.cacheExists(regionName, options)

    then:

    1 * regionConfig.regionName >> regionName
    1 * buildingContext.sessionFactory >> sessionFactory
    1 * sessionFactory.sessionFactoryOptions >> options
    4 * options.cacheRegionPrefix >> regionPrefix

    cacheExists
    storage.regionName == "$regionPrefix.$regionName"
    storage.cacheManager.cache.regionExists("$regionPrefix.$regionName")
  }

  def 'createQueryResultsRegionStorageAccess returns CacheStorage and sets up the region in cache'() {
    given:

    def regionName = 'MyRegionName'
    def regionPrefix = 'foo'

    SessionFactoryImplementor sessionFactory = Mock SessionFactoryImplementor
    SessionFactoryOptions options = Mock SessionFactoryOptions

    SessionFactoryOptions settings = stubSessionFactory()
    jedisRegionFactory.prepareForUse(settings, [:])

    when:

    CacheStorage storage = jedisRegionFactory.createQueryResultsRegionStorageAccess(regionName, sessionFactory) as CacheStorage
    boolean cacheExists = jedisRegionFactory.cacheExists(regionName, options)

    then:

    1 * sessionFactory.sessionFactoryOptions >> options
    4 * options.cacheRegionPrefix >> regionPrefix

    cacheExists
    storage.regionName == "$regionPrefix.$regionName"
    storage.cacheManager.cache.regionExists("$regionPrefix.$regionName")
  }

  def 'createTimestampsRegionStorageAccess() returns CacheStorage and sets up the region in cache'() {
    given:

    def regionName = 'MyRegionName'
    def regionPrefix = 'foo'

    SessionFactoryImplementor sessionFactory = Mock SessionFactoryImplementor
    SessionFactoryOptions options = Mock SessionFactoryOptions

    SessionFactoryOptions settings = stubSessionFactory()
    jedisRegionFactory.prepareForUse(settings, [:])

    when:

    CacheStorage storage = jedisRegionFactory.createTimestampsRegionStorageAccess(regionName, sessionFactory) as CacheStorage

    then:

    1 * sessionFactory.sessionFactoryOptions >> options
    2 * options.cacheRegionPrefix >> regionPrefix

    storage.regionName == "$regionPrefix.$regionName"
    storage.cacheManager.cache.regionExists("$regionPrefix.$regionName")
  }

  def 'releaseFromUse shuts down CacheManager'() {
    given:

    CacheManager cacheManager = Mock CacheManager
    jedisRegionFactory.cacheManager = cacheManager

    when:

    jedisRegionFactory.releaseFromUse()

    then:

    1 * cacheManager.shutdown()
  }

  def 'buildDomainDataRegion() creates a DomainDataRegion with a CacheStorage object'() {
    given:

    def regionName = 'MyRegionName'
    def regionPrefix = 'foo'

    SessionFactoryOptions settings = stubSessionFactory()

    DomainDataRegionConfig regionConfig = Stub(DomainDataRegionConfig) {
      getRegionName() >> regionName
    }

    DomainDataRegionBuildingContext buildingContext = Stub(DomainDataRegionBuildingContext) {
      getSessionFactory() >> Stub(SessionFactoryImplementor) {
        getSessionFactoryOptions() >> Stub(SessionFactoryOptions) {
          getCacheRegionPrefix() >> regionPrefix
        }
      }
    }

    jedisRegionFactory.start(settings, [:])

    when:

    DomainDataRegionImpl domainDataRegion = (DomainDataRegionImpl) jedisRegionFactory.buildDomainDataRegion(regionConfig, buildingContext)

    then:

    CacheStorage storage = (CacheStorage) domainDataRegion.cacheStorageAccess
    storage.regionName == "$regionPrefix.$regionName"
    storage.cacheManager.cache.regionExists("$regionPrefix.$regionName")
  }

  private stubSessionFactory() {
    Stub(SessionFactoryOptions) {
      getServiceRegistry() >> Stub(StandardServiceRegistry) {
        getService(StrategySelector) >> Stub(StrategySelector) {
          resolveDefaultableStrategy(CacheKeysFactory, null, DefaultCacheKeysFactory.INSTANCE) >> DefaultCacheKeysFactory.INSTANCE
        }
      }
    }
  }
}
