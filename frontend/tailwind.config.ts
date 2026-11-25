import type { Config } from "tailwindcss";

export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        atlantis_blue: "#264d8d",
        sleek_gray: "#121212",
        dark_gray: "#0A0A0A",
        light_gray: "#D3D3D3",
      },
      gridTemplateColumns: {
        dashboard: "288px 1fr",
      },
      fontFamily: {
        sans: ["Inter", "ui-sans-serif", "system-ui", "sans-serif"],
      },
    },
  },
  plugins: [],
} satisfies Config;
