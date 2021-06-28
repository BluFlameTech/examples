package com.bluflametech.cache;

import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.SecondLevelCacheLogger;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.support.DomainDataRegionImpl;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.cache.spi.support.RegionFactoryTemplate;
import org.hibernate.cache.spi.support.RegionNameQualifier;
import org.hibernate.cache.spi.support.StorageAccess;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import java.util.List;
import java.util.Map;

/**
 * Hibernate RegionFactory for Redis L2 cache support via Jedis.
 */
public class JedisRegionFactory extends RegionFactoryTemplate {

  private CacheKeysFactory cacheKeysFactory;
  CacheManager cacheManager;

  /**
   * Sets up Jedis connection pool and initializes RegionFactory.
   *
   * @param settings   hibernate SessionFactoryOptions
   * @param properties hibernate properties (e.g. from spring.jpa.properties in application.yml)
   * @throws CacheException
   */
  @Override
  @SuppressWarnings("unchecked")
  public void prepareForUse(SessionFactoryOptions settings, Map properties) throws CacheException {
    StrategySelector selector = settings.getServiceRegistry().getService(StrategySelector.class);
    cacheKeysFactory = selector.resolveDefaultableStrategy(CacheKeysFactory.class,
        properties.get(Environment.CACHE_KEYS_FACTORY), DefaultCacheKeysFactory.INSTANCE);
    this.cacheManager = new JedisCacheManager((Map<String, Object>) properties);
  }

  @Override
  public boolean isMinimalPutsEnabledByDefault() {
    return true;
  }

  @Override
  protected DomainDataStorageAccess createDomainDataStorageAccess(
      DomainDataRegionConfig regionConfig,
      DomainDataRegionBuildingContext buildingContext) {
    return new CacheStorage(
        qualifyName(regionConfig.getRegionName(), buildingContext.getSessionFactory().getSessionFactoryOptions()),
        cacheManager);
  }

  @Override
  protected StorageAccess createQueryResultsRegionStorageAccess(
      String regionName,
      SessionFactoryImplementor sessionFactory) {
    String defaultedRegionName = defaultRegionName(
        regionName,
        sessionFactory,
        DEFAULT_QUERY_RESULTS_REGION_UNQUALIFIED_NAME,
        LEGACY_QUERY_RESULTS_REGION_UNQUALIFIED_NAMES
    );
    return new CacheStorage(qualifyName(defaultedRegionName, sessionFactory.getSessionFactoryOptions()), cacheManager);
  }

  @Override
  protected StorageAccess createTimestampsRegionStorageAccess(
      String regionName,
      SessionFactoryImplementor sessionFactory) {
    String defaultedRegionName = defaultRegionName(
        regionName,
        sessionFactory,
        DEFAULT_UPDATE_TIMESTAMPS_REGION_UNQUALIFIED_NAME,
        LEGACY_UPDATE_TIMESTAMPS_REGION_UNQUALIFIED_NAMES
    );
    return new CacheStorage(qualifyName(defaultedRegionName, sessionFactory.getSessionFactoryOptions()), cacheManager);
  }

  @Override
  public AccessType getDefaultAccessType() {
    return AccessType.TRANSACTIONAL;
  }

  @Override
  protected void releaseFromUse() {
    cacheManager.shutdown();
  }

  @Override
  public DomainDataRegion buildDomainDataRegion(DomainDataRegionConfig regionConfig, DomainDataRegionBuildingContext buildingContext) {
    verifyStarted();
    return new DomainDataRegionImpl(
        regionConfig,
        this,
        createDomainDataStorageAccess(regionConfig, buildingContext),
        getImplicitCacheKeysFactory(),
        buildingContext);
  }

  @Override
  protected CacheKeysFactory getImplicitCacheKeysFactory() {
    return this.cacheKeysFactory;
  }

  private String qualifyName(String unqualifiedName, SessionFactoryOptions options) {
    assert !RegionNameQualifier.INSTANCE.isQualified(unqualifiedName, options);
    return RegionNameQualifier.INSTANCE.qualify(unqualifiedName, options);
  }

  protected boolean cacheExists(String unqualifiedRegionName, SessionFactoryOptions options) {
    final String qualifiedRegionName = qualifyName(unqualifiedRegionName, options);
    if (this.cacheManager == null) {
      return false;
    }
    try (Cache cache = cacheManager.getCache()) {
      return cache.regionExists(qualifiedRegionName);
    } catch (Exception exception) {
      throw new CacheException(exception);
    }
  }

  //Lovingly poached from JCacheRegionFactory
  //https://github.com/hibernate/hibernate-orm/blob/main/hibernate-jcache/src/main/java/org/hibernate/cache/jcache/internal/JCacheRegionFactory.java
  private String defaultRegionName(String regionName, SessionFactoryImplementor sessionFactory,
                                   String defaultRegionName, List<String> legacyDefaultRegionNames) {
    if (defaultRegionName.equals(regionName)
        && !cacheExists(regionName, sessionFactory.getSessionFactoryOptions())) {
      // Maybe the user configured caches explicitly with legacy names; try them and use the first that exists

      for (String legacyDefaultRegionName : legacyDefaultRegionNames) {
        if (cacheExists(legacyDefaultRegionName, sessionFactory.getSessionFactoryOptions())) {
          SecondLevelCacheLogger.INSTANCE.usingLegacyCacheName(defaultRegionName, legacyDefaultRegionName);
          return legacyDefaultRegionName;
        }
      }
    }
    return regionName;
  }
}
