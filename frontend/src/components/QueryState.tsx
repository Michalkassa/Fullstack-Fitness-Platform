import type { ReactNode } from "react";
import type { UseQueryResult } from "@tanstack/react-query";
import Spinner from "./Spinner";
import ErrorText from "./ErrorText";

interface QueryStateProps<T> {
  query: UseQueryResult<T>;
  children: (data: T) => ReactNode;
  /** Shown instead of children when the data is an empty array. */
  empty?: ReactNode;
}

/** Standard loading / error / empty handling for a query. */
export default function QueryState<T>({ query, children, empty }: QueryStateProps<T>) {
  if (query.isPending) return <Spinner />;
  if (query.isError) return <ErrorText error={query.error} />;
  if (empty !== undefined && Array.isArray(query.data) && query.data.length === 0) return <>{empty}</>;
  return <>{children(query.data)}</>;
}
