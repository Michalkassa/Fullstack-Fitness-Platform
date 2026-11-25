import { useWorkouts } from "@/api/queries";
import QueryState from "@/components/QueryState";
import AddWorkout from "@/components/workouts/WorkoutForm";
import WorkoutCard from "@/components/workouts/WorkoutCard";

export default function WorkoutsPage() {
  const query = useWorkouts();
  return (
    <section className="flex flex-col gap-6">
      <div className="flex items-center justify-between gap-4">
        <h1 className="page-header">Workouts</h1>
        <AddWorkout />
      </div>
      <QueryState query={query} empty={<p className="text-center text-gray-400">No workouts created yet.</p>}>
        {(workouts) => (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {workouts.map((w) => (
              <WorkoutCard key={w.id} workout={w} to={`/dashboard/workouts/${w.id}`} />
            ))}
          </div>
        )}
      </QueryState>
    </section>
  );
}
