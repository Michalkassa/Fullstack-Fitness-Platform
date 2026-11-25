export default function Spinner({ className = "" }: { className?: string }) {
  return (
    <div className={`flex items-center justify-center py-6 ${className}`} role="status" aria-label="Loading">
      <div className="h-7 w-7 animate-spin rounded-full border-2 border-white/15 border-t-atlantis_blue" />
    </div>
  );
}
