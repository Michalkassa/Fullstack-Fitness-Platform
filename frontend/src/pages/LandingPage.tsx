import { Link } from "react-router";
import { FaAppleAlt, FaChartLine, FaDumbbell } from "react-icons/fa";
import { useAuth } from "@/auth/AuthContext";
import PublicNavbar from "@/layouts/PublicNavbar";

const features = [
  { icon: <FaDumbbell size={20} />, title: "Track every set", text: "Log workouts and watch your estimated 1RM climb over time." },
  { icon: <FaChartLine size={20} />, title: "See your progress", text: "Volume trends, personal records and a year-long activity heatmap." },
  { icon: <FaAppleAlt size={20} />, title: "Stay accountable", text: "Daily nutrition goals, programs and streak-building XP." },
];

export default function LandingPage() {
  const { isAuthenticated } = useAuth();

  return (
    <div className="relative flex min-h-screen flex-col overflow-hidden">
      <div className="absolute inset-0 bg-[url('/bg.jpg')] bg-cover bg-center" />
      <div className="absolute inset-0 bg-gradient-to-b from-dark_gray/70 via-dark_gray/85 to-dark_gray" />

      <PublicNavbar />

      <main className="relative z-10 flex flex-1 items-center justify-center">
        <div className="mx-auto w-full max-w-4xl px-6 py-16 text-center">
          <span className="chip mb-6">Your training, locked in</span>
          <h1 className="text-4xl font-semibold tracking-tight text-white md:text-6xl">Lift with confidence.</h1>
          <p className="mx-auto mt-4 max-w-xl text-base text-gray-300 md:text-lg">
            Log your workouts, track your strength, and stay on top of your goals — all in one clean, focused app.
          </p>

          <div className="mt-8 flex justify-center gap-3">
            {isAuthenticated ? (
              <Link to="/dashboard" className="btn-primary">Go to dashboard</Link>
            ) : (
              <>
                <Link to="/register" className="btn-primary">Sign up</Link>
                <Link to="/login" className="btn-ghost">Sign in</Link>
              </>
            )}
          </div>

          <div className="mt-16 grid gap-4 text-left sm:grid-cols-3">
            {features.map((f) => (
              <div key={f.title} className="card">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-atlantis_blue text-white">
                  {f.icon}
                </div>
                <h3 className="mt-4 font-semibold text-white">{f.title}</h3>
                <p className="mt-1 text-sm text-gray-400">{f.text}</p>
              </div>
            ))}
          </div>
        </div>
      </main>
    </div>
  );
}
