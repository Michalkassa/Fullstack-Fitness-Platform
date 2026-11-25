import {
  CategoryScale,
  Chart as ChartJS,
  Filler,
  LinearScale,
  LineElement,
  PointElement,
  Tooltip,
  type ChartData,
  type ScriptableContext,
} from "chart.js";
import { Line } from "react-chartjs-2";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Filler, Tooltip);

const ACCENT = "#5b8def"; // lightened atlantis_blue for contrast on dark

interface LineChartProps {
  labels: string[];
  values: number[];
  label: string;
}

export default function LineChart({ labels, values, label }: LineChartProps) {
  const finite = values.filter((v) => Number.isFinite(v));
  const hasData = finite.length > 0;
  const min = hasData ? Math.min(...finite) : 0;
  const max = hasData ? Math.max(...finite) : 1;
  const pad = Math.max(1, (max - min) * 0.15);

  const data: ChartData<"line"> = {
    labels,
    datasets: [
      {
        label,
        data: values,
        tension: 0.35,
        borderWidth: 2,
        borderColor: ACCENT,
        pointRadius: 2,
        pointHoverRadius: 5,
        pointBackgroundColor: ACCENT,
        fill: true,
        backgroundColor: (ctx: ScriptableContext<"line">) => {
          const { ctx: c, chartArea } = ctx.chart;
          if (!chartArea) return "transparent";
          const g = c.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
          g.addColorStop(0, "rgba(91,141,239,0.25)");
          g.addColorStop(1, "rgba(91,141,239,0)");
          return g;
        },
      },
    ],
  };

  return (
    <div className="relative h-full min-h-[14rem] w-full">
      <Line
        data={data}
        options={{
          responsive: true,
          maintainAspectRatio: false,
          interaction: { intersect: false, mode: "index" },
          scales: {
            x: {
              grid: { display: false },
              ticks: { color: "#71717a", maxRotation: 0, autoSkipPadding: 16 },
              border: { display: false },
            },
            y: {
              beginAtZero: false,
              min: hasData ? min - pad : undefined,
              max: hasData ? max + pad : undefined,
              grid: { color: "rgba(255,255,255,0.06)" },
              ticks: { color: "#71717a" },
              border: { display: false },
            },
          },
          plugins: {
            legend: { display: false },
            tooltip: {
              backgroundColor: "#18181b",
              borderColor: "rgba(255,255,255,0.1)",
              borderWidth: 1,
              padding: 10,
              titleColor: "#fff",
              bodyColor: "#d4d4d8",
            },
          },
        }}
      />
    </div>
  );
}
