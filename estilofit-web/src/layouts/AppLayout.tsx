import { useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import {
  LayoutDashboard,
  Shirt,
  FolderTree,
  Package,
  Truck,
  ShoppingBag,
  CreditCard,
  BarChart2,
  Tag,
  Users,
  Settings,
  LogOut,
  Menu,
  UserCircle,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { useAuthStore } from "../store/authStore";
import type { Role } from "../types/api";

interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  roles: Role[];
}

const NAV_ITEMS: NavItem[] = [
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard, roles: ["ADMIN", "MANAGER", "SELLER"] },
  { to: "/categories", label: "Categorias", icon: FolderTree, roles: ["ADMIN", "MANAGER"] },
  { to: "/products", label: "Produtos", icon: Shirt, roles: ["ADMIN", "MANAGER"] },
  { to: "/stock", label: "Estoque", icon: Package, roles: ["ADMIN", "MANAGER", "SELLER"] },
  { to: "/suppliers", label: "Fornecedores", icon: Truck, roles: ["ADMIN", "MANAGER"] },
  { to: "/sales", label: "Vendas", icon: ShoppingBag, roles: ["ADMIN", "MANAGER", "SELLER"] },
  { to: "/receivables", label: "Contas a Receber", icon: CreditCard, roles: ["ADMIN", "MANAGER"] },
  { to: "/reports", label: "Relatórios", icon: BarChart2, roles: ["ADMIN", "MANAGER"] },
  { to: "/promotions", label: "Alertas", icon: Tag, roles: ["ADMIN", "MANAGER"] },
];

const ADMIN_ITEMS: NavItem[] = [
  { to: "/users", label: "Usuários", icon: Users, roles: ["ADMIN"] },
  { to: "/settings", label: "Configurações", icon: Settings, roles: ["ADMIN"] },
];

export function AppLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const hasRole = useAuthStore((s) => s.hasRole);

  const handleLogout = async () => {
    await logout();
    navigate("/login", { replace: true });
  };

  const visibleMain = NAV_ITEMS.filter((item) => hasRole(item.roles));
  const visibleAdmin = ADMIN_ITEMS.filter((item) => hasRole(item.roles));

  const renderNavItem = (item: NavItem) => {
    const Icon = item.icon;
    return (
      <NavLink
        key={item.to}
        to={item.to}
        className={({ isActive }) =>
          [
            "flex items-center gap-3 rounded-btn px-3 py-2.5 text-sm transition-colors",
            isActive
              ? "bg-brand-purple-muted text-brand-purple"
              : "text-content-secondary hover:bg-bg-surface-hover hover:text-content-primary",
          ].join(" ")
        }
        title={collapsed ? item.label : undefined}
      >
        <Icon className="h-5 w-5 shrink-0" />
        {!collapsed && <span>{item.label}</span>}
      </NavLink>
    );
  };

  return (
    <div className="flex h-full">
      {/* Sidebar */}
      <aside
        className={[
          "flex flex-col border-r border-border-subtle bg-bg-surface transition-all duration-200",
          collapsed ? "w-16" : "w-60",
        ].join(" ")}
      >
        {/* Logo */}
        <div className="flex h-16 items-center px-4">
          {collapsed ? (
            <span className="text-lg font-bold text-brand-purple">EF</span>
          ) : (
            <span className="text-xl font-bold tracking-tight">
              <span className="text-content-primary">ESTILO</span>
              <span className="text-brand-purple">FIT</span>
            </span>
          )}
        </div>

        {/* Navegação */}
        <nav className="flex-1 space-y-1 overflow-y-auto px-2 py-2">
          {!collapsed && (
            <p className="px-3 py-1 text-xs uppercase tracking-wider text-content-muted">
              Principal
            </p>
          )}
          {visibleMain.map(renderNavItem)}

          {visibleAdmin.length > 0 && (
            <>
              <div className="my-2 border-t border-border-subtle" />
              {!collapsed && (
                <p className="px-3 py-1 text-xs uppercase tracking-wider text-content-muted">
                  Administração
                </p>
              )}
              {visibleAdmin.map(renderNavItem)}
            </>
          )}
        </nav>
      </aside>

      {/* Área principal */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Topbar */}
        <header className="flex h-16 items-center justify-between border-b border-border-subtle bg-bg-surface px-6">
          <button
            onClick={() => setCollapsed((c) => !c)}
            className="rounded-btn p-2 text-content-secondary transition-colors hover:bg-bg-surface-hover hover:text-content-primary"
            aria-label="Alternar menu"
          >
            <Menu className="h-5 w-5" />
          </button>

          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2 text-sm">
              <UserCircle className="h-6 w-6 text-content-secondary" />
              <div className="text-right">
                <p className="font-medium text-content-primary">{user?.name}</p>
                <p className="text-xs text-content-muted">{user?.role}</p>
              </div>
            </div>
            <button
              onClick={handleLogout}
              className="rounded-btn p-2 text-content-secondary transition-colors hover:bg-bg-surface-hover hover:text-state-danger"
              aria-label="Sair"
              title="Sair"
            >
              <LogOut className="h-5 w-5" />
            </button>
          </div>
        </header>

        {/* Conteúdo */}
        <main className="flex-1 overflow-y-auto bg-bg-base p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
