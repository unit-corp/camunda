/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.db.schema.index;

import io.camunda.optimize.dto.optimize.TenantDto;
import io.camunda.optimize.service.db.DatabaseConstants;
import io.camunda.optimize.service.db.schema.DefaultIndexMappingCreator;
import java.io.IOException;
import org.elasticsearch.xcontent.XContentBuilder;

public abstract class TenantIndex<TBuilder> extends DefaultIndexMappingCreator<TBuilder> {
  public static final int VERSION = 3;

  @Override
  public String getIndexName() {
    return DatabaseConstants.TENANT_INDEX_NAME;
  }

  @Override
  public int getVersion() {
    return VERSION;
  }

  @Override
  public XContentBuilder addProperties(XContentBuilder xContentBuilder) throws IOException {
    // @formatter:off
    return xContentBuilder
        .startObject(TenantDto.Fields.id.name())
        .field("type", "text")
        .field("index", false)
        .endObject()
        .startObject(TenantDto.Fields.name.name())
        .field("type", "text")
        .field("index", false)
        .endObject()
        .startObject(TenantDto.Fields.engine.name())
        .field("type", "text")
        .field("index", false)
        .endObject();
    // @formatter:on
  }
}
