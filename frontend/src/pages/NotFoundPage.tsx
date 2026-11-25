import { Link } from "react-router";

export default function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 text-center">
      <h1 className="page-header">Page not found</h1>
      <p className="text-gray-400">That page doesn’t exist.</p>
      <Link to="/" className="btn-primary">Go home</Link>
    </div>
  );
}
