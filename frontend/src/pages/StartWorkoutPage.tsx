import { useWorkouts } from "@/api/queries";
import QueryState from "@/components/QueryState";
import WorkoutCard from "@/components/workouts/WorkoutCard";

export default function StartWorkoutPage() {
  const query = useWorkouts();
  return (
    <section className="flex flex-col gap-6">
      <header>
        <h1 className="page-header">Start a Workout</h1>
        <p className="mt-1 text-gray-500">Pick a workout to begin your session.</p>
      </header>
      <QueryState query={query} empty={<p className="text-center text-gray-400">No workouts created yet.</p>}>
        {(workouts) => (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {workouts.map((w) => (
              <WorkoutCard key={w.id} workout={w} to={`/dashboard/runningworkout/${w.id}`} start />
            ))}
          </div>
        )}
      </QueryState>
    </section>
  );
}
