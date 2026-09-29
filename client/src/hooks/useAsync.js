import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Runs an async loader and tracks { data, error, loading }.
 * `deps` behaves like a useEffect dependency list, and stale responses from
 * earlier requests are discarded so fast filter changes cannot race.
 */
export function useAsync(loader, deps = [], { immediate = true } = {}) {
  const [state, setState] = useState({ data: null, error: null, loading: immediate });
  const requestId = useRef(0);
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  const run = useCallback(async () => {
    const id = ++requestId.current;
    setState((s) => ({ ...s, loading: true, error: null }));
    try {
      const data = await loader();
      if (mounted.current && id === requestId.current) {
        setState({ data, error: null, loading: false });
      }
      return data;
    } catch (error) {
      if (mounted.current && id === requestId.current) {
        setState({ data: null, error: error.message || 'Something went wrong', loading: false });
      }
      return null;
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    if (immediate) {
      run();
    }
  }, [run, immediate]);

  return { ...state, reload: run, setData: (data) => setState((s) => ({ ...s, data })) };
}
