import { useState } from "react";
import { exportApi } from "@/api/endpoints";
import { useRecords } from "@/api/queries";
import type { CsvExportType } from "@/api/types";
import ErrorText from "@/components/ErrorText";
import QueryState from "@/components/QueryState";
import { saveBlob } from "@/lib/download";

const exports: { label: string; run: () => ReturnType<typeof exportApi.pdf>; fallback: string }[] = [
  ...(["logs", "bodyweight", "nutrition"] as CsvExportType[]).map((type) => ({
    label: `${type[0].toUpperCase()}${type.slice(1)} CSV`,
    run: () => exportApi.csv(type),
    fallback: `lockedin-${type}.csv`,
  })),
  { label: "Progress report (PDF)", run: exportApi.pdf, fallback: "lockedin-progress-report.pdf" },
];

function ExportButtons() {
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<unknown>(null);

  async function download(item: (typeof exports)[number]) {
    setBusy(item.label);
    setError(null);
    try {
      const { blob, filename } = await item.run();
      saveBlob(blob, filename ?? item.fallback);
    } catch (err) {
      setError(err);
    } finally {
      setBusy(null);
    }
  }

  return (
    <div className="flex flex-col items-center gap-2">
      <div className="flex flex-wrap justify-center gap-3">
        {exports.map((item) => (
          <button key={item.label} className="btn-ghost" disabled={busy !== null} onClick={() => download(item)}>
            {busy === item.label ? "Preparing…" : item.label}
          </button>
        ))}
      </div>
      <ErrorText error={error} />
    </div>
  );
}

export default function RecordsPage() {
  const query = useRecords();
  return (
    <section className="flex flex-col gap-6">
      <header>
        <h1 className="page-header">Personal Records</h1>
        <p className="mt-1 text-gray-500">Your best lifts, and export your data.</p>
      </header>
      <ExportButtons />
      <div className="card">
        <QueryState
          query={query}
          empty={<p className="text-center text-gray-400">No personal records yet — log some sets to set your first PRs.</p>}
        >
          {(records) => (
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Exercise</th>
                    <th>Best weight</th>
                    <th>Best est. 1RM</th>
                    <th>Best set volume</th>
                    <th>1RM achieved</th>
                  </tr>
                </thead>
                <tbody>
                  {records.map((r) => (
                    <tr key={r.exerciseId}>
                      <td className="font-semibold">{r.exerciseName}</td>
                      <td>{r.bestWeight} kg</td>
                      <td>{r.bestOneRepMax} kg</td>
                      <td>{r.bestVolume} kg</td>
                      <td className="text-gray-400">{r.achievedAt}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </QueryState>
      </div>
    </section>
  );
}
