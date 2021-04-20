package com.bluflametech.test

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface AlwaysTest { }

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface UnitTest { }

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface IntegrationTest { }

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface DatabaseTest { }

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface SyncInfraTest { }

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface AsyncInfraTest { }
