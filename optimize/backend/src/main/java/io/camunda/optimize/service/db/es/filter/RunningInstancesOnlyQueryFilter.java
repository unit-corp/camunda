/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.db.es.filter;

import static io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex.END_DATE;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;

import io.camunda.optimize.dto.optimize.query.report.single.process.filter.data.RunningInstancesOnlyFilterDataDto;
import java.util.List;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.stereotype.Component;

@Component
public class RunningInstancesOnlyQueryFilter
    implements QueryFilter<RunningInstancesOnlyFilterDataDto> {

  @Override
  public void addFilters(
      final BoolQueryBuilder query,
      final List<RunningInstancesOnlyFilterDataDto> runningInstancesOnlyData,
      final FilterContext filterContext) {
    if (runningInstancesOnlyData != null && !runningInstancesOnlyData.isEmpty()) {
      final List<QueryBuilder> filters = query.filter();

      final BoolQueryBuilder onlyRunningInstancesQuery = boolQuery().mustNot(existsQuery(END_DATE));

      filters.add(onlyRunningInstancesQuery);
    }
  }
}
