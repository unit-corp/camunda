/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.dto.optimize.query.alert;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum AlertIntervalUnit {
  SECONDS,
  MINUTES,
  HOURS,
  DAYS,
  WEEKS,
  MONTHS,
  ;

  @JsonValue
  public String getId() {
    return name().toLowerCase(Locale.ENGLISH);
  }
}
