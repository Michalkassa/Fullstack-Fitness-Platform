import { useActivity, useWeeklyStats } from "@/api/queries";
import LineChart from "@/components/LineChart";
import QueryState from "@/components/QueryState";
import ActivityHeatmap from "./ActivityHeatmap";

/** Dashboard analytics block: weekly training volume trend + activity heatmap. */
export default function TrainingSummary() {
  const weekly = useWeeklyStats();
  const activity = useActivity();

  return (
    <div className="card flex flex-col gap-6">
      <div>
        <h2 className="text-lg font-semibold text-white">Weekly Volume</h2>
        <QueryState query={weekly} empty={<p className="mt-2 text-gray-500">Log some sets to see your weekly volume.</p>}>
          {(data) => (
            <div className="mt-4 h-56">
              <LineChart label="Weekly volume" labels={data.map((w) => w.week)} values={data.map((w) => w.volume)} />
            </div>
          )}
        </QueryState>
      </div>
      <div>
        <h2 className="pb-3 text-lg font-semibold text-white">Activity</h2>
        <QueryState query={activity}>{(data) => <ActivityHeatmap activity={data} />}</QueryState>
      </div>
    </div>
  );
}
