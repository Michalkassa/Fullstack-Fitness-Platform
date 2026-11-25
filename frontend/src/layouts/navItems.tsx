import type { ReactNode } from "react";
import { BsSkipStartFill } from "react-icons/bs";
import { FaDumbbell, FaWeightScale } from "react-icons/fa6";
import { IoBody } from "react-icons/io5";
import { FaAppleAlt, FaClipboardList, FaHome, FaTrophy } from "react-icons/fa";

const ICON = 20;

export interface NavItem {
  to: string;
  label: string;
  shortLabel: string;
  icon: ReactNode;
}

export const navItems: NavItem[] = [
  { to: "/dashboard", label: "Dashboard", shortLabel: "Home", icon: <FaHome size={ICON} /> },
  { to: "/dashboard/runningworkout", label: "Start Workout", shortLabel: "Start", icon: <BsSkipStartFill size={ICON} /> },
  { to: "/dashboard/workouts", label: "Workouts", shortLabel: "Workouts", icon: <FaClipboardList size={ICON} /> },
  { to: "/dashboard/programs", label: "Programs", shortLabel: "Programs", icon: <FaDumbbell size={ICON} /> },
  { to: "/dashboard/exercises", label: "Exercises", shortLabel: "Exercises", icon: <IoBody size={ICON} /> },
  { to: "/dashboard/records", label: "Records", shortLabel: "Records", icon: <FaTrophy size={ICON} /> },
  { to: "/dashboard/nutrition", label: "Nutrition", shortLabel: "Nutrition", icon: <FaAppleAlt size={ICON} /> },
  { to: "/dashboard/bodyweights", label: "Body Weight", shortLabel: "Weight", icon: <FaWeightScale size={ICON} /> },
];
