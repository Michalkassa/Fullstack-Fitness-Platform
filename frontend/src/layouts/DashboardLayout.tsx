import { Link, NavLink, Outlet, useNavigate } from "react-router";
import { IoIosLogOut } from "react-icons/io";
import { useAuth } from "@/auth/AuthContext";
import LevelBadge from "@/components/LevelBadge";
import { navItems } from "./navItems";

function Sidebar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="flex h-screen w-72 flex-col border-r border-white/5 bg-sleek_gray">
      <Link to="/dashboard" className="flex items-center gap-2 px-6 py-5">
        <img src="/logo.png" alt="" className="h-6 w-6" />
        <span className="text-lg font-semibold tracking-tight text-white">LockedIn</span>
      </Link>

      <div className="px-4 pb-2">
        <div className="flex items-center gap-3 rounded-2xl bg-white/[0.03] p-3">
          <img
            src={user?.image || "/blankUserImage.webp"}
            width={40}
            height={40}
            alt="Your profile picture"
            className="rounded-full"
          />
          <p className="truncate text-xs text-gray-400">{user?.email}</p>
        </div>
        <LevelBadge />
      </div>

      <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-2">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === "/dashboard"}
            className={({ isActive }) => `nav-item ${isActive ? "nav-item-active" : ""}`}
          >
            {item.icon}
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="border-t border-white/5 p-3">
        <button
          type="button"
          className="nav-item w-full text-left"
          onClick={() => {
            logout();
            navigate("/");
          }}
        >
          <IoIosLogOut size={20} />
          <span>Log out</span>
        </button>
      </div>
    </div>
  );
}

function BottomNav() {
  return (
    <nav className="no-scrollbar flex w-screen items-stretch overflow-x-auto border-t border-white/10 bg-sleek_gray/95 backdrop-blur-md">
      {navItems.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.to === "/dashboard"}
          className={({ isActive }) =>
            `flex shrink-0 flex-col items-center justify-center gap-1 px-4 py-2 text-[11px] transition ${
              isActive ? "text-white" : "text-gray-500 hover:text-gray-300"
            }`
          }
        >
          {item.icon}
          <span>{item.shortLabel}</span>
        </NavLink>
      ))}
    </nav>
  );
}

export default function DashboardLayout() {
  return (
    <div className="min-h-screen md:grid md:grid-cols-dashboard">
      <aside className="sticky top-0 hidden h-screen md:block">
        <Sidebar />
      </aside>

      <main className="mx-auto w-full max-w-6xl px-4 pb-28 pt-6 md:px-8 md:pb-12">
        <Outlet />
      </main>

      <div className="fixed inset-x-0 bottom-0 z-30 md:hidden">
        <BottomNav />
      </div>
    </div>
  );
}
