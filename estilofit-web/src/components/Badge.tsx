import type { ReactNode } from "react";

type BadgeVariant = "success" | "danger" | "warning" | "info" | "purple";

const VARIANT_CLASSES: Record<BadgeVariant, string> = {
  success: "bg-state-success/10 text-state-success",
  danger: "bg-state-danger/10 text-state-danger",
  warning: "bg-state-warning/10 text-state-warning",
  info: "bg-state-info/10 text-state-info",
  purple: "bg-brand-purple-muted text-brand-purple",
};

interface BadgeProps {
  variant: BadgeVariant;
  children: ReactNode;
}

export function Badge({ variant, children }: BadgeProps) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${VARIANT_CLASSES[variant]}`}
    >
      {children}
    </span>
  );
}
