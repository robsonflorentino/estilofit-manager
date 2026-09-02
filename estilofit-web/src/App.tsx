import { useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { Loader2 } from "lucide-react";
import { useAuthStore } from "./store/authStore";
import { LoginPage } from "./pages/LoginPage";
import { DashboardPage } from "./pages/DashboardPage";
import { CategoriesPage } from "./pages/CategoriesPage";
import { ProductsPage } from "./pages/ProductsPage";
import { SuppliersPage } from "./pages/SuppliersPage";
import { StockPage } from "./pages/StockPage";
import { SupplyEntryPage } from "./pages/SupplyEntryPage";
import { SalesPage } from "./pages/SalesPage";
import { NovaVendaPage } from "./pages/NovaVendaPage";
import { ReceivablesPage } from "./pages/ReceivablesPage";
import { ReportsPage } from "./pages/ReportsPage";
import { PromotionsPage } from "./pages/PromotionsPage";
import { CommissionsPage } from "./pages/CommissionsPage";
import { UsersPage } from "./pages/UsersPage";
import { SettingsPage } from "./pages/SettingsPage";
import { ForbiddenPage } from "./pages/ForbiddenPage";
import { AppLayout } from "./layouts/AppLayout";
import { PrivateRoute } from "./routes/PrivateRoute";
import { RoleRoute } from "./routes/RoleRoute";

function App() {
  const isInitializing = useAuthStore((s) => s.isInitializing);
  const restoreSession = useAuthStore((s) => s.restoreSession);

  // No boot, tenta restaurar a sessão a partir do refresh token (cookie httpOnly).
  useEffect(() => {
    restoreSession();
  }, [restoreSession]);

  // Enquanto verifica a sessão, evita o flash para /login em reloads.
  if (isInitializing) {
    return (
      <div className="flex h-full items-center justify-center bg-bg-base">
        <Loader2 className="h-8 w-8 animate-spin text-brand-purple" />
      </div>
    );
  }

  return (
    <BrowserRouter>
      <Routes>
        {/* Rota pública */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/403" element={<ForbiddenPage />} />

        {/* Rotas protegidas (exigem autenticação) */}
        <Route element={<PrivateRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/dashboard" element={<DashboardPage />} />

            {/* Consulta de estoque — todos os perfis */}
            <Route path="/stock" element={<StockPage />} />

            {/* Vendas — todos os perfis (vendedor vê apenas as próprias) */}
            <Route path="/sales" element={<SalesPage />} />
            <Route path="/sales/new" element={<NovaVendaPage />} />

            {/* Admin + Gestor */}
            <Route element={<RoleRoute roles={["ADMIN", "MANAGER"]} />}>
              <Route path="/categories" element={<CategoriesPage />} />
              <Route path="/products" element={<ProductsPage />} />
              <Route path="/suppliers" element={<SuppliersPage />} />
              <Route path="/stock/entry" element={<SupplyEntryPage />} />
              <Route path="/receivables" element={<ReceivablesPage />} />
              <Route path="/reports" element={<ReportsPage />} />
              <Route path="/promotions" element={<PromotionsPage />} />
              <Route path="/commissions" element={<CommissionsPage />} />
            </Route>

            {/* Admin */}
            <Route element={<RoleRoute roles={["ADMIN"]} />}>
              <Route path="/users" element={<UsersPage />} />
              <Route path="/settings" element={<SettingsPage />} />
            </Route>
          </Route>
        </Route>

        {/* Redirecionamentos */}
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
