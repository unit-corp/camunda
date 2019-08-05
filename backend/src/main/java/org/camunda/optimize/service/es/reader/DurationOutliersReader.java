/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.reader;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.optimize.dto.optimize.query.analysis.DurationChartEntryDto;
import org.camunda.optimize.dto.optimize.query.analysis.FindingsDto;
import org.camunda.optimize.service.es.OptimizeElasticsearchClient;
import org.camunda.optimize.service.es.schema.type.ProcessInstanceType;
import org.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import org.camunda.optimize.service.security.TenantAuthorizationService;
import org.camunda.optimize.service.util.DefinitionQueryUtil;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.histogram.HistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentileRanks;
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentileRanksAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.stats.Stats;
import org.elasticsearch.search.aggregations.metrics.stats.StatsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.stats.extended.ExtendedStats;
import org.elasticsearch.search.aggregations.metrics.stats.extended.ExtendedStatsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Component;

import javax.ws.rs.ForbiddenException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.camunda.optimize.service.es.schema.type.ProcessInstanceType.ACTIVITY_DURATION;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.NUMBER_OF_DATA_POINTS_FOR_AUTOMATIC_INTERVAL_SELECTION;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.PROC_INSTANCE_TYPE;

@AllArgsConstructor
@Component
@Slf4j
public class DurationOutliersReader {
  private static final String HISTOGRAM_AGG = "histogram";
  private static final String STATS_AGG = "stats";
  private static final String FILTERED_FLOW_NODES_AGG = "filteredFlowNodes";
  private static final String EVENTS = "events";
  private static final String ACTIVITY_ID = "activityId";
  private static final String NESTED_AGG = "nested";
  private static final String RANKS_AGG = "ranks_agg";

  private TenantAuthorizationService tenantAuthorizationService;
  private final OptimizeElasticsearchClient esClient;
  private ProcessDefinitionReader processDefinitionReader;


  public List<DurationChartEntryDto> getCountByDurationChart(String procDefKey, List<String> procDefVersion, String
    flowNodeId, String userId, List<String> tenantId) {
    if (!tenantAuthorizationService.isAuthorizedToSeeAllTenants(userId, tenantId)) {
      throw new ForbiddenException("Current user is not authorized to access data of the provided tenant");
    }

    final BoolQueryBuilder query = DefinitionQueryUtil.createDefinitionQuery(
      procDefKey,
      procDefVersion,
      tenantId,
      new ProcessInstanceType(),
      processDefinitionReader::getLatestVersionToKey
    );

    long interval = getInterval(query, flowNodeId);
    HistogramAggregationBuilder histogram = AggregationBuilders.histogram(HISTOGRAM_AGG)
      .field(EVENTS + "." + ProcessInstanceType.DURATION)
      .interval(interval);

    NestedAggregationBuilder termsAgg = buildNestedAggregation(flowNodeId, histogram);

    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
      .query(query)
      .fetchSource(false)
      .aggregation(termsAgg)
      .size(0);

    SearchRequest searchRequest =
      new SearchRequest(PROC_INSTANCE_TYPE)
        .types(PROC_INSTANCE_TYPE)
        .source(searchSourceBuilder);

    SearchResponse search;
    try {
      search = esClient.search(searchRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      log.warn("Couldn't retrieve duration chart");
      throw new OptimizeRuntimeException(e.getMessage());
    }


    return ((Histogram) ((Filter) ((Nested) search.getAggregations().get(EVENTS)).getAggregations()
      .get(FILTERED_FLOW_NODES_AGG)).getAggregations().get(HISTOGRAM_AGG)).getBuckets()
      .stream()
      .map(b -> new DurationChartEntryDto(b.getKeyAsString(), b.getDocCount()))
      .collect(Collectors.toList());
  }

  private long getInterval(BoolQueryBuilder query, String flowNodeId) {
    StatsAggregationBuilder statsAgg = AggregationBuilders.stats(STATS_AGG)
      .field(EVENTS + "." + ProcessInstanceType.DURATION);

    NestedAggregationBuilder termsAgg = buildNestedAggregation(flowNodeId, statsAgg);

    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
      .query(query)
      .fetchSource(false)
      .aggregation(termsAgg)
      .size(0);

    SearchRequest searchRequest =
      new SearchRequest(PROC_INSTANCE_TYPE)
        .types(PROC_INSTANCE_TYPE)
        .source(searchSourceBuilder);

    SearchResponse search;
    try {
      search = esClient.search(searchRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      throw new OptimizeRuntimeException(e.getMessage());
    }

    double min = ((Stats) ((Filter) ((Nested) search.getAggregations()
      .get(EVENTS)).getAggregations().get(FILTERED_FLOW_NODES_AGG)).getAggregations()
      .get(STATS_AGG)).getMin();
    double max = ((Stats) ((Filter) ((Nested) search.getAggregations()
      .get(EVENTS)).getAggregations().get(FILTERED_FLOW_NODES_AGG)).getAggregations()
      .get(STATS_AGG)).getMax();

    return (long) Math.ceil((max - min) / (NUMBER_OF_DATA_POINTS_FOR_AUTOMATIC_INTERVAL_SELECTION));
  }

  private NestedAggregationBuilder buildNestedAggregation(String flowNodeId, AggregationBuilder... agg) {
    TermQueryBuilder terms = QueryBuilders.termQuery(EVENTS + "." + ACTIVITY_ID, flowNodeId);

    FilterAggregationBuilder filteredFlowNodes = AggregationBuilders.filter(FILTERED_FLOW_NODES_AGG, terms);
    for (AggregationBuilder aggregationBuilder : agg) {
      filteredFlowNodes.subAggregation(aggregationBuilder);
    }
    return AggregationBuilders.nested(EVENTS, EVENTS)
      .subAggregation(filteredFlowNodes);
  }

  public Map<String, FindingsDto> getFlowNodeOutlierMap(String procDefKey, List<String> procDefVersion, String userId,
                                                        List<String> tenantId) {
    if (!tenantAuthorizationService.isAuthorizedToSeeAllTenants(userId, tenantId)) {
      throw new ForbiddenException("Current user is not authorized to access data of the provided tenant");
    }

    final BoolQueryBuilder query = DefinitionQueryUtil.createDefinitionQuery(
      procDefKey,
      procDefVersion,
      tenantId,
      new ProcessInstanceType(),
      processDefinitionReader::getLatestVersionToKey
    );
    ExtendedStatsAggregationBuilder stats = AggregationBuilders.extendedStats(STATS_AGG)
      .field(EVENTS + "." + ACTIVITY_DURATION);


    TermsAggregationBuilder terms = AggregationBuilders.terms(EVENTS)
      .field(EVENTS + "." + ProcessInstanceType.ACTIVITY_ID)
      .subAggregation(stats);

    NestedAggregationBuilder nested = AggregationBuilders.nested(NESTED_AGG, EVENTS)
      .subAggregation(terms);

    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
      .query(query)
      .fetchSource(false)
      .aggregation(nested)
      .size(0);

    SearchRequest searchRequest = new SearchRequest(PROC_INSTANCE_TYPE)
      .types(PROC_INSTANCE_TYPE)
      .source(searchSourceBuilder);

    Aggregations aggregations;
    try {
      aggregations = esClient.search(searchRequest, RequestOptions.DEFAULT).getAggregations();
    } catch (IOException e) {
      log.warn("Couldn't retrieve outliers from Elasticsearch");
      throw new OptimizeRuntimeException(e.getMessage());
    }

    return mapToFlowNodeOutlierMap(query, aggregations);
  }

  private Map<String, FindingsDto> mapToFlowNodeOutlierMap(BoolQueryBuilder query,
                                                           Aggregations aggregations) {
    HashMap<String, FindingsDto> result = new HashMap<>();

    List<? extends Terms.Bucket> buckets = ((Terms) ((Nested) aggregations.get(NESTED_AGG)).getAggregations()
      .get(EVENTS))
      .getBuckets();
    buckets.forEach((bucket) -> {
      ExtendedStats statsAgg = bucket.getAggregations().get(STATS_AGG);
      FindingsDto finding = new FindingsDto();

      double stdDeviationBoundLower = statsAgg.getStdDeviationBound(ExtendedStats.Bounds.LOWER);
      double stdDeviationBoundHigher = statsAgg.getStdDeviationBound(ExtendedStats.Bounds.UPPER);
      double avg = statsAgg.getAvg();

      double[] values = {stdDeviationBoundHigher, stdDeviationBoundLower};
      PercentileRanksAggregationBuilder percentileRanks = AggregationBuilders.percentileRanks(
        RANKS_AGG, values
      ).field(EVENTS + "." + ACTIVITY_DURATION);

      NestedAggregationBuilder nested = buildNestedAggregation(bucket.getKeyAsString(), percentileRanks);

      SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
        .query(query)
        .fetchSource(false)
        .aggregation(nested)
        .size(0);

      SearchRequest searchRequest = new SearchRequest(PROC_INSTANCE_TYPE)
        .types(PROC_INSTANCE_TYPE)
        .source(searchSourceBuilder);

      Aggregations singleNodeAggregation;
      try {
        singleNodeAggregation = esClient.search(searchRequest, RequestOptions.DEFAULT).getAggregations();
      } catch (IOException e) {
        throw new OptimizeRuntimeException(e.getMessage());
      }
      PercentileRanks ranks =
        ((Filter) (((Nested) singleNodeAggregation.get(EVENTS)).getAggregations()
          .get(FILTERED_FLOW_NODES_AGG))).getAggregations().get(RANKS_AGG);

      if (stdDeviationBoundLower > statsAgg.getMin()) {
        double percent = ranks.percent(stdDeviationBoundLower);
        finding.setLowerOutlier(percent, avg / stdDeviationBoundLower);
        finding.setOutlierCount(Math.round(statsAgg.getCount() * 0.01 * percent));
      }

      if (stdDeviationBoundHigher < statsAgg.getMax()) {
        double percent = ranks.percent(stdDeviationBoundHigher);
        finding.setHigherOutlier(100 - percent, stdDeviationBoundHigher / avg);
        // summing with existing outlier count as we want to have the sum of both lower and higher outliers count
        finding.setOutlierCount(finding.getOutlierCount() + Math.round(statsAgg.getCount() * 0.01 * (100 - percent)));
      }

      result.put(bucket.getKeyAsString(), finding);
    });
    Long totalOutlierCount = result.values()
      .stream()
      .reduce(0L, (accumulator, finding) -> accumulator + finding.getOutlierCount(), Long::sum);
    result.values().forEach(v -> v.setHeat((double) v.getOutlierCount() / totalOutlierCount));
    return result;
  }
}
