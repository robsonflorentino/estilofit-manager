import { Navigate, Outlet } from "react-router-dom";
import { useAuthStore } from "../store/authStore";

/**
 * Protege rotas que exigem usuário autenticado.
 * Redireciona para /login se não houver sessão ativa.
 */
export function PrivateRoute() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}
