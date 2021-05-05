/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import {get, post} from 'request';

export async function loadDefinitions(type, collectionId, camundaEventImportedOnly = false) {
  const params = {camundaEventImportedOnly};
  if (collectionId) {
    params.filterByCollectionScope = collectionId;
  }

  const response = await get(`api/definition/${type}/keys`, params);

  return await response.json();
}

export async function loadVersions(type, collectionId, key) {
  const params = {};
  if (collectionId) {
    params.filterByCollectionScope = collectionId;
  }

  const response = await get(`api/definition/${type}/${key}/versions`, params);

  return await response.json();
}

export async function loadTenants(type, collectionId, key, versions) {
  const params = {definitions: [{versions, key}]};
  if (collectionId) {
    params.filterByCollectionScope = collectionId;
  }

  const response = await post(`api/definition/${type}/_resolveTenantsForVersions`, params);

  return (await response.json())[0].tenants;
}
