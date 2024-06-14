/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.util;

import com.google.common.collect.ImmutableList;
import io.camunda.optimize.dto.optimize.ReportConstants;
import io.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DefinitionVersionHandlingUtil {

  public static String convertToLatestParticularVersion(
      final String processDefinitionVersion, final Supplier<String> latestVersionSupplier) {
    return convertToLatestParticularVersion(
        ImmutableList.of(processDefinitionVersion), latestVersionSupplier);
  }

  public static String convertToLatestParticularVersion(
      @NonNull final List<String> definitionVersions,
      @NonNull final Supplier<String> latestVersionSupplier) {
    final boolean isDefinitionVersionSetToAllOrLatest =
        definitionVersions.stream()
            .anyMatch(
                version ->
                    ReportConstants.ALL_VERSIONS.equalsIgnoreCase(version)
                        || ReportConstants.LATEST_VERSION.equalsIgnoreCase(version));
    if (isDefinitionVersionSetToAllOrLatest) {
      return latestVersionSupplier.get();
    } else {
      try {
        return definitionVersions.stream()
            .filter(StringUtils::isNumeric)
            .map(Integer::parseInt)
            .max(Integer::compareTo)
            .map(Object::toString)
            .orElse(getLastEntryInList(definitionVersions));
      } catch (final NumberFormatException exception) {
        throw new OptimizeRuntimeException("Cannot determine latest version for definition");
      }
    }
  }

  private static String getLastEntryInList(@NonNull final List<String> processDefinitionVersions) {
    return processDefinitionVersions.get(processDefinitionVersions.size() - 1);
  }

  public static boolean isDefinitionVersionSetToAll(final List<String> definitionVersions) {
    return definitionVersions.stream().anyMatch(ReportConstants.ALL_VERSIONS::equalsIgnoreCase);
  }

  public static boolean isDefinitionVersionSetToLatest(final List<String> definitionVersions) {
    return definitionVersions.stream().anyMatch(ReportConstants.LATEST_VERSION::equalsIgnoreCase);
  }

  public static boolean isDefinitionVersionSetToAllOrLatest(final String definitionVersion) {
    return isDefinitionVersionSetToAllOrLatest(Collections.singletonList(definitionVersion));
  }

  public static boolean isDefinitionVersionSetToAllOrLatest(final List<String> definitionVersions) {
    return definitionVersions.stream()
        .anyMatch(
            v ->
                ReportConstants.ALL_VERSIONS.equalsIgnoreCase(v)
                    || ReportConstants.LATEST_VERSION.equalsIgnoreCase(v));
  }
}
