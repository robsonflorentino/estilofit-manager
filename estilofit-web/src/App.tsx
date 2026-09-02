import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { LoginPage } from "./pages/LoginPage";
import { DashboardPage } from "./pages/DashboardPage";
import { CategoriesPage } from "./pages/CategoriesPage";
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

            {/* Admin + Gestor */}
            <Route element={<RoleRoute roles={["ADMIN", "MANAGER"]} />}>
              <Route path="/categories" element={<CategoriesPage />} />
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
