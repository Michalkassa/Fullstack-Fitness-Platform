import { useNavigate } from "react-router";
import { useCopyProgram, usePrograms } from "@/api/queries";
import ErrorText from "@/components/ErrorText";
import QueryState from "@/components/QueryState";

export default function ProgramsPage() {
  const query = usePrograms();
  const copy = useCopyProgram();
  const navigate = useNavigate();

  return (
    <section className="flex flex-col gap-6">
      <header>
        <h1 className="page-header">Programs</h1>
        <p className="mt-1 text-gray-500">Pick a proven training program to copy into your workouts.</p>
      </header>
      <ErrorText error={copy.error} />
      <QueryState query={query} empty={<p className="text-center text-gray-400">No programs available yet.</p>}>
        {(programs) => (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {programs.map((program) => {
              const adding = copy.isPending && copy.variables === program.id;
              return (
                <div key={program.id} className="card flex flex-col gap-3">
                  <h2 className="text-xl font-semibold text-white">{program.name}</h2>
                  <p className="text-sm text-gray-400">{program.description}</p>
                  <ul className="flex-1 list-inside list-disc text-sm text-gray-300">
                    {program.exercises.map((we) => (
                      <li key={we.id}>{we.name}</li>
                    ))}
                  </ul>
                  <button
                    className="btn-primary"
                    disabled={copy.isPending}
                    onClick={() =>
                      copy.mutate(program.id, { onSuccess: (workout) => navigate(`/dashboard/workouts/${workout.id}`) })
                    }
                  >
                    {adding ? "Adding…" : "Use this program"}
                  </button>
                </div>
              );
            })}
          </div>
        )}
      </QueryState>
    </section>
  );
}
