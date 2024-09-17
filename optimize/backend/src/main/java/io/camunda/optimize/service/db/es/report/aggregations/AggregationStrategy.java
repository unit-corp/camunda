/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.report.aggregations;

import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation.Builder.ContainerBuilder;
import co.elastic.clients.elasticsearch._types.aggregations.FormatMetricAggregationBase;
import co.elastic.clients.util.Pair;
import io.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationDto;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class AggregationStrategy<
    T extends FormatMetricAggregationBase.AbstractBuilder<T>> {

  protected abstract Pair<String, ContainerBuilder> createAggregationBuilderForAggregation(
      final String customIdentifier, Script script, String... field);

  protected abstract Double getValueForAggregation(
      final String customIdentifier, final Map<String, Aggregate> aggs);

  public abstract AggregationDto getAggregationType();

  public Double getValue(final Map<String, Aggregate> aggs) {
    return getValue(null, aggs);
  }

  public Double getValue(final String customIdentifier, final Map<String, Aggregate> aggs) {
    return getValueForAggregation(customIdentifier, aggs);
  }

  public Pair<String, Aggregation.Builder.ContainerBuilder> createAggregationBuilder(
      Script script, String... field) {
    return createAggregationBuilder(null, script, field);
  }

  public Pair<String, Aggregation.Builder.ContainerBuilder> createAggregationBuilder(
      final String customIdentifier, Script script, String... field) {
    return createAggregationBuilderForAggregation(customIdentifier, script, field);
  }

  protected String createAggregationName(final String... segments) {
    return Arrays.stream(segments).filter(Objects::nonNull).collect(Collectors.joining("_"));
  }
}
