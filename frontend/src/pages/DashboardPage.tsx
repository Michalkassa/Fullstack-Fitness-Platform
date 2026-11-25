import { Link } from "react-router";
import { BsSkipStartFill } from "react-icons/bs";
import { FaClipboardList, FaTrophy } from "react-icons/fa";
import { IoBody } from "react-icons/io5";
import { usePopularExercises } from "@/api/queries";
import QueryState from "@/components/QueryState";
import BodyWeightChart from "@/components/bodyweight/BodyWeightChart";
import TrainingSummary from "@/components/analytics/TrainingSummary";

const quickLinks = [
  { to: "/dashboard/runningworkout", label: "Start Workout", icon: <BsSkipStartFill size={22} />, accent: true },
  { to: "/dashboard/workouts", label: "Workouts", icon: <FaClipboardList size={22} /> },
  { to: "/dashboard/exercises", label: "Exercises", icon: <IoBody size={22} /> },
  { to: "/dashboard/records", label: "Records", icon: <FaTrophy size={22} /> },
];

function PopularExercises() {
  const query = usePopularExercises();
  return (
    <QueryState query={query} empty={<p className="text-gray-500">No exercises created yet.</p>}>
      {(data) => (
        <div className="flex flex-col gap-2">
          {data.map((exercise) => (
            <Link
              key={exercise.id}
              to={`/dashboard/exercises/${exercise.id}`}
              className="flex justify-between rounded-xl bg-white/[0.03] px-4 py-3 text-sm text-white transition hover:bg-white/[0.06]"
            >
              <span className="font-medium">{exercise.name}</span>
              <span className="text-gray-400">{exercise.logCount} logs</span>
            </Link>
          ))}
        </div>
      )}
    </QueryState>
  );
}

export default function DashboardPage() {
  return (
    <div className="flex flex-col gap-6">
      <header>
        <h1 className="page-header">Dashboard</h1>
        <p className="mt-1 text-gray-500">Your training at a glance.</p>
      </header>

      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        {quickLinks.map((link) => (
          <Link
            key={link.to}
            to={link.to}
            className={`flex items-center gap-3 rounded-2xl border p-4 font-medium text-white transition hover:brightness-110 active:scale-[.98] ${
              link.accent ? "border-transparent bg-atlantis_blue" : "border-white/5 bg-sleek_gray hover:bg-white/[0.06]"
            }`}
          >
            {link.icon}
            <span>{link.label}</span>
          </Link>
        ))}
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Link to="/dashboard/bodyweights" className="card transition hover:border-white/10">
          <h2 className="text-lg font-semibold text-white">Bodyweight Progress</h2>
          <div className="mt-4 h-56">
            <BodyWeightChart />
          </div>
        </Link>
        <div className="card">
          <h2 className="text-lg font-semibold text-white">Popular Exercises</h2>
          <div className="mt-4">
            <PopularExercises />
          </div>
        </div>
      </div>

      <TrainingSummary />
    </div>
  );
}
