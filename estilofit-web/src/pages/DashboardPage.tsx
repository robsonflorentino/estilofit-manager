import { DollarSign, ShoppingBag, Package, Wallet } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { useAuthStore } from "../store/authStore";

interface KpiCardProps {
  icon: LucideIcon;
  value: string;
  label: string;
}

function KpiCard({ icon: Icon, value, label }: KpiCardProps) {
  return (
    <div className="card border-t-[3px] border-t-brand-purple">
      <Icon className="mb-3 h-7 w-7 text-brand-purple" />
      <p className="text-2xl font-bold text-content-primary">{value}</p>
      <p className="mt-1 text-sm text-content-secondary">{label}</p>
    </div>
  );
}

export function DashboardPage() {
  const user = useAuthStore((s) => s.user);

  return (
    <div>
      {/* Cabeçalho */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-content-primary">Dashboard</h1>
        <p className="mt-1 text-sm text-content-secondary">
          Olá, {user?.name}. Aqui está a visão geral da loja.
        </p>
      </div>

      {/* KPIs (placeholder — serão ligados à API nas próximas features) */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <KpiCard icon={DollarSign} value="R$ —" label="Vendas do mês" />
        <KpiCard icon={ShoppingBag} value="—" label="Nº de vendas" />
        <KpiCard icon={Package} value="—" label="Itens em estoque" />
        <KpiCard icon={Wallet} value="R$ —" label="Pró-labore estimado" />
      </div>

      <div className="card mt-6">
        <p className="text-sm text-content-secondary">
          Os indicadores serão preenchidos conforme os módulos de produtos, estoque e vendas
          forem implementados. Esta é a estrutura base do painel.
        </p>
      </div>
    </div>
  );
}
