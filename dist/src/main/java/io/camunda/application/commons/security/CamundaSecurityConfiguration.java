/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.application.commons.security;

import io.camunda.application.commons.security.CamundaSecurityConfiguration.CamundaSecurityProperties;
import io.camunda.security.configuration.MultiTenancyConfiguration;
import io.camunda.security.configuration.SecurityConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CamundaSecurityProperties.class)
public class CamundaSecurityConfiguration {

  @Bean
  public MultiTenancyConfiguration multiTenancyConfiguration(
      final SecurityConfiguration securityConfiguration) {
    return securityConfiguration.getMultiTenancy();
  }

  @ConfigurationProperties("camunda.security")
  public static final class CamundaSecurityProperties extends SecurityConfiguration {}
}
