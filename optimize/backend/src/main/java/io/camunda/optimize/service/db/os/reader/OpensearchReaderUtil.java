/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.db.os.reader;

import io.camunda.optimize.service.db.os.externalcode.client.sync.OpenSearchDocumentOperations;
import io.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Buckets;
import org.opensearch.client.opensearch._types.aggregations.MultiBucketAggregateBase;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.get.GetResult;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.HitsMetadata;

@Slf4j
public class OpensearchReaderUtil {

  public static <T> List<T> extractResponseValues(final SearchResponse<T> searchResponse) {
    return Optional.ofNullable(searchResponse)
        .map(SearchResponse::hits)
        .map(HitsMetadata::hits)
        .map(hits -> hits.stream().map(Hit::source).toList())
        .orElseThrow(
            () -> {
              String reason = "Was not able to parse response values from OpenSearch";
              log.error(reason);
              return new OptimizeRuntimeException(reason);
            });
  }

  public static <T> List<T> extractResponseValues(
      final SearchResponse<T> searchResponse, final Function<Hit<T>, T> mappingFunction) {
    return Optional.ofNullable(searchResponse)
        .map(SearchResponse::hits)
        .map(HitsMetadata::hits)
        .map(hits -> hits.stream().map(mappingFunction).toList())
        .orElseThrow(
            () -> {
              String reason = "Was not able to parse response values from OpenSearch";
              log.error(reason);
              return new OptimizeRuntimeException(reason);
            });
  }

  public static <T> Set<String> extractAggregatedResponseValues(
      final SearchResponse<T> searchResponse, final String aggPath) {
    return Optional.ofNullable(searchResponse)
        .map(response -> response.aggregations().get(aggPath))
        .filter(Aggregate::isSterms)
        .map(Aggregate::sterms)
        .map(MultiBucketAggregateBase::buckets)
        .map(Buckets::array)
        .map(Collection::stream)
        .map(streamBuckets -> streamBuckets.map(StringTermsBucket::key).collect(Collectors.toSet()))
        .orElseThrow(
            () -> {
              String reason =
                  String.format(
                      "Was not able to parse aggregated sterm response values from OpenSearch with path %s",
                      aggPath);
              log.error(reason);
              return new OptimizeRuntimeException(reason);
            });
  }

  public static <T> List<T> extractAggregatedResponseValues(
      final OpenSearchDocumentOperations.AggregatedResult<Hit<T>> searchResponse) {
    return extractAggregatedResponseValues(searchResponse, Hit::source);
  }

  public static <T> List<T> extractAggregatedResponseValues(
      final OpenSearchDocumentOperations.AggregatedResult<Hit<T>> searchResponse,
      final Function<Hit<T>, T> mappingFunction) {
    return Optional.ofNullable(searchResponse)
        .map(OpenSearchDocumentOperations.AggregatedResult::values)
        .map(hits -> hits.stream().map(mappingFunction).collect(Collectors.toList()))
        .orElseThrow(
            () -> {
              String reason = "Was not able to parse response aggregations from OpenSearch";
              log.error(reason);
              return new OptimizeRuntimeException(reason);
            });
  }

  public static <T> Optional<T> processGetResponse(GetResult<T> getResponse) {
    return Optional.ofNullable(getResponse).filter(GetResult::found).map(GetResult::source);
  }

  public static <T> Collection<? extends T> mapHits(
      final HitsMetadata<JsonData> searchHits,
      final int resultLimit,
      final Class<T> typeClass,
      final Function<Hit<T>, T> mappingFunction) {
    final List<T> results = new ArrayList<>();
    for (Hit<JsonData> hit : searchHits.hits()) {
      if (results.size() >= resultLimit) {
        break;
      }
      try {
        final Optional<JsonData> optionalMappedHit = Optional.ofNullable(hit.source());
        optionalMappedHit.ifPresent(
            hitValue -> {
              try {
                T definitionDto = hitValue.to(typeClass);
                Hit<T> adaptedHit =
                    new Hit.Builder<T>().index(hit.index()).source(definitionDto).build();
                T enrichedDto = mappingFunction.apply(adaptedHit);
                results.add(enrichedDto);
              } catch (Exception e) {
                final String reason =
                    "While mapping search results to class {} "
                        + "it was not possible to deserialize a hit from OpenSearch!";
                log.error(reason, typeClass.getSimpleName(), e);
                throw new OptimizeRuntimeException(reason);
              }
            });
      } catch (Exception e) {
        final String reason =
            "While mapping search results to class {} "
                + "it was not possible to deserialize a hit from Opensearch!";
        log.error(reason, typeClass.getSimpleName(), e);
        throw new OptimizeRuntimeException(reason);
      }
    }
    return results;
  }
}
