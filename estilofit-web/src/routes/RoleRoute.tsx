import { Navigate, Outlet } from "react-router-dom";
import { useAuthStore } from "../store/authStore";
import type { Role } from "../types/api";

interface RoleRouteProps {
  roles: Role[];
}

/**
 * Protege rotas que exigem um perfil específico.
 * Exibe a página 403 se o usuário logado não tiver a role necessária.
 */
export function RoleRoute({ roles }: RoleRouteProps) {
  const hasRole = useAuthStore((s) => s.hasRole);

  if (!hasRole(roles)) {
    return <Navigate to="/403" replace />;
  }

  return <Outlet />;
}
