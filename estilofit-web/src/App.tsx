import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
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
import { UsersPage } from "./pages/UsersPage";
import { ForbiddenPage } from "./pages/ForbiddenPage";
import { AppLayout } from "./layouts/AppLayout";
import { PrivateRoute } from "./routes/PrivateRoute";
import { RoleRoute } from "./routes/RoleRoute";

function App() {
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
            </Route>

            {/* Admin */}
            <Route element={<RoleRoute roles={["ADMIN"]} />}>
              <Route path="/users" element={<UsersPage />} />
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
