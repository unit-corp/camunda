/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.read.domain;

import io.camunda.search.filter.FilterBuilders;
import io.camunda.search.filter.VariableFilter;
import io.camunda.search.page.SearchQueryPage;
import io.camunda.search.query.SearchQueryBase;
import io.camunda.search.sort.SortOptionBuilders;
import io.camunda.search.sort.VariableSort;
import io.camunda.util.ObjectBuilder;
import java.util.Objects;
import java.util.function.Function;

public record VariableDbQuery(VariableDbFilter filter, VariableSort sort, SearchQueryPage page) {

  public VariableDbQuery(VariableFilter filter, VariableSort sort, SearchQueryPage page) {
    this(VariableDbFilter.of(filter), sort, page);
  }

  public VariableDbQuery {
    // There should be a default in the SearchQueryPage, so this should never happen
    Objects.requireNonNull(page);
  }

  public static VariableDbQuery of(
      final Function<VariableDbQuery.Builder, ObjectBuilder<VariableDbQuery>> fn) {
    return fn.apply(new VariableDbQuery.Builder()).build();
  }

  public static final class Builder
      extends SearchQueryBase.AbstractQueryBuilder<VariableDbQuery.Builder>
      implements ObjectBuilder<VariableDbQuery> {

    private static final VariableFilter EMPTY_FILTER = FilterBuilders.variable().build();
    private static final VariableSort EMPTY_SORT = SortOptionBuilders.variable().build();

    private VariableFilter filter;
    private VariableSort sort;

    public Builder filter(final VariableFilter value) {
      filter = value;
      return this;
    }

    public Builder sort(final VariableSort value) {
      sort = value;
      return this;
    }

    public Builder filter(
        final Function<VariableFilter.Builder, ObjectBuilder<VariableFilter>> fn) {
      return filter(FilterBuilders.variable(fn));
    }

    public Builder sort(final Function<VariableSort.Builder, ObjectBuilder<VariableSort>> fn) {
      return sort(SortOptionBuilders.variable(fn));
    }

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    public VariableDbQuery build() {
      filter = Objects.requireNonNullElse(filter, EMPTY_FILTER);
      sort = Objects.requireNonNullElse(sort, EMPTY_SORT);
      return new VariableDbQuery(VariableDbFilter.of(filter), sort, page().sanitize());
    }
  }
}
