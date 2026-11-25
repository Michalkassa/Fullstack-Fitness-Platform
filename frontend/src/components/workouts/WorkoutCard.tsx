import { Link } from "react-router";
import type { Workout } from "@/api/types";

/** Workout summary card; `start` renders the "Start" variant used on the start page. */
export default function WorkoutCard({ workout, to, start = false }: { workout: Workout; to: string; start?: boolean }) {
  return (
    <Link to={to} className="card flex flex-col transition hover:border-white/10 hover:bg-white/[0.05]">
      <h3 className="text-lg font-semibold text-white">{workout.name}</h3>
      <p className="line-clamp-3-safe mt-1 text-sm text-gray-400">{workout.description}</p>
      <ul className="mt-3 flex-1 space-y-1">
        {workout.exercises.map((we) => (
          <li key={we.id} className="text-sm text-gray-500">{we.name}</li>
        ))}
      </ul>
      {start && <span className="btn-primary mt-4 w-full">Start</span>}
    </Link>
  );
}
