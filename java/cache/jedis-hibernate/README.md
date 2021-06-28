# Redis Hibernate L2 Cache With Jedis

_This example is associated with the [Redis L2 Cache Integration Using Jedis](https://www.bluflametech.com/blog/redis-l2-cache) post._

This is a working implementation of Redis L2 cache for Hibernate 5+ using the Jedis client. It supports Redis in
both standalone and sentinel configurations.

## Prerequisites

* Redis 6.2.4
* Java 15
* Maven 3.8.1

## Directory Structure

* ```src/main/java``` - the Jedis Hibernate L2 Java implementation
* ```src/test/groovy``` - the Spock test Specifications
* ```src/test/resources``` - the CodeNarc configuration

## Commands

Running the tests:
```mvn clean test```

Installing the library:
```mvn clean install```

## Configuring Hibernate to Use Jedis for Redis L2 Cache

1. Build this library ( ```mvn clean install``` ) or merge its contents into your codebase.
2. Add the JedisRegionFactory to the hibernate config.

Ex. Spring Boot application.yml config

```yaml
spring:
  jpa:
    properties:
      hibernate:
        cache:
          use_second_level_cache: true
          region:
            factory_class: com.bluflametech.cache.JedisRegionFactory
          redis:
            sentinel:
              master: mymaster
              nodes: localhost:26379
```

or

```yaml
spring:
  jpa:
    properties:
      hibernate:
        cache:
          use_second_level_cache: true
          region:
            factory_class: com.bluflametech.cache.JedisRegionFactory
          redis:
            standalone:
            host: localhost
            port: 6379
```
