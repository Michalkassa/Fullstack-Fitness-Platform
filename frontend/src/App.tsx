import { Route, Routes } from "react-router";
import { GuestOnly, RequireAuth } from "@/auth/guards";
import DashboardLayout from "@/layouts/DashboardLayout";
import LandingPage from "@/pages/LandingPage";
import { LoginPage, RegisterPage } from "@/pages/AuthPages";
import DashboardPage from "@/pages/DashboardPage";
import BodyWeightPage from "@/pages/BodyWeightPage";
import ExercisesPage from "@/pages/ExercisesPage";
import ExerciseDetailPage from "@/pages/ExerciseDetailPage";
import WorkoutsPage from "@/pages/WorkoutsPage";
import WorkoutDetailPage from "@/pages/WorkoutDetailPage";
import StartWorkoutPage from "@/pages/StartWorkoutPage";
import RunningWorkoutPage from "@/pages/RunningWorkoutPage";
import ProgramsPage from "@/pages/ProgramsPage";
import RecordsPage from "@/pages/RecordsPage";
import NutritionPage from "@/pages/NutritionPage";
import NotFoundPage from "@/pages/NotFoundPage";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />

      <Route element={<GuestOnly />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>

      <Route element={<RequireAuth />}>
        <Route path="/dashboard" element={<DashboardLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="bodyweights" element={<BodyWeightPage />} />
          <Route path="exercises" element={<ExercisesPage />} />
          <Route path="exercises/:id" element={<ExerciseDetailPage />} />
          <Route path="workouts" element={<WorkoutsPage />} />
          <Route path="workouts/:id" element={<WorkoutDetailPage />} />
          <Route path="runningworkout" element={<StartWorkoutPage />} />
          <Route path="runningworkout/:id" element={<RunningWorkoutPage />} />
          <Route path="programs" element={<ProgramsPage />} />
          <Route path="records" element={<RecordsPage />} />
          <Route path="nutrition" element={<NutritionPage />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
