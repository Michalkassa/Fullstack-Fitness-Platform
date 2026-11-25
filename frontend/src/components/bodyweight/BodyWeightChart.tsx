import { useBodyWeights } from "@/api/queries";
import LineChart from "@/components/LineChart";
import QueryState from "@/components/QueryState";
import { formatDate } from "@/lib/dates";

export default function BodyWeightChart() {
  const query = useBodyWeights();
  return (
    <QueryState
      query={query}
      empty={<div className="flex h-full items-center justify-center text-gray-500">No bodyweights logged yet.</div>}
    >
      {(data) => (
        <LineChart label="Body weight" labels={data.map((d) => formatDate(d.createdAt))} values={data.map((d) => d.weight)} />
      )}
    </QueryState>
  );
}
