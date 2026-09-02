import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { Toaster } from "react-hot-toast";
import "./index.css";
import App from "./App.tsx";
import { registerSessionExpiredHandler } from "./lib/api";
import { useAuthStore } from "./store/authStore";

// Liga o handler de sessão expirada (401 sem refresh válido) ao store de auth.
// Quando disparado, limpa o estado; a navegação para /login é feita pelos guards.
registerSessionExpiredHandler(() => {
  useAuthStore.getState().clear();
  window.location.href = "/login";
});

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
    <Toaster
      position="top-right"
      toastOptions={{
        duration: 4000,
        style: {
          background: "#242424",
          color: "#E8E8E8",
          border: "1px solid #333333",
        },
        success: { iconTheme: { primary: "#22C55E", secondary: "#242424" } },
        error: { iconTheme: { primary: "#EF4444", secondary: "#242424" } },
      }}
    />
  </StrictMode>,
);
