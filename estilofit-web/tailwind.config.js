/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        brand: {
          purple: "#CC00FF",
          "purple-hover": "#AA00CC",
          "purple-light": "#E566FF",
          "purple-muted": "rgba(204, 0, 255, 0.10)",
        },
        bg: {
          base: "#1A1A1A",
          surface: "#242424",
          "surface-raised": "#2E2E2E",
          "surface-hover": "#333333",
          input: "#1F1F1F",
        },
        border: {
          DEFAULT: "#333333",
          subtle: "#2A2A2A",
          focus: "#CC00FF",
        },
        content: {
          primary: "#E8E8E8",
          secondary: "#A0A0A0",
          muted: "#666666",
        },
        state: {
          success: "#22C55E",
          warning: "#F59E0B",
          danger: "#EF4444",
          info: "#3B82F6",
        },
      },
      fontFamily: {
        sans: ["Inter", "system-ui", "sans-serif"],
      },
      borderRadius: {
        card: "12px",
        btn: "8px",
        modal: "16px",
      },
      boxShadow: {
        card: "0 1px 3px rgba(0,0,0,0.4)",
        modal: "0 20px 60px rgba(0,0,0,0.6)",
        focus: "0 0 0 2px rgba(204,0,255,0.3)",
      },
    },
  },
  plugins: [],
};
