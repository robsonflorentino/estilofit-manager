import type { ReactNode } from "react";
import { useAuthStore } from "../store/authStore";
import type { Role } from "../types/api";

interface RoleGuardProps {
  roles: Role[];
  children: ReactNode;
}

/**
 * Oculta elementos de UI (botões, menus, abas) quando o usuário logado
 * não possui um dos perfis informados. É controle de UX — a segurança
 * real é garantida pelo backend (ADR-005).
 */
export function RoleGuard({ roles, children }: RoleGuardProps) {
  const hasRole = useAuthStore((s) => s.hasRole);
  if (!hasRole(roles)) return null;
  return <>{children}</>;
}
