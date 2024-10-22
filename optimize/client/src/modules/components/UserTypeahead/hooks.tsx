import {useCallback, useState} from 'react';

import debouncePromise from 'debouncePromise';

import {Identity, searchIdentities} from './service';

const debounceRequest = debouncePromise();

export function useLoadIdentities({
  excludeGroups,
  fetchUsers,
}: {
  fetchUsers?: (
    query: string,
    excludeGroups?: boolean
  ) => Promise<{total: number; result: Identity[]}>;
  excludeGroups: boolean;
}) {
  const [loading, setLoading] = useState(true);
  const [identities, setIdentities] = useState<Identity[]>([]);
  const loadNewValues = useCallback(
    async (query: string, delay = 0) => {
      setLoading(true);

      const {result} = await debounceRequest(async () => {
        return await (fetchUsers || searchIdentities)(query, excludeGroups);
      }, delay);

      setIdentities(result);
      setLoading(false);
    },
    [fetchUsers, excludeGroups]
  );

  return {loading, setLoading, identities, loadNewValues};
}
