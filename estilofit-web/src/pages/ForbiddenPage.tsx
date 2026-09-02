import { ShieldAlert, ArrowLeft } from "lucide-react";
import { useNavigate } from "react-router-dom";

export function ForbiddenPage() {
  const navigate = useNavigate();

  return (
    <div className="flex min-h-full flex-col items-center justify-center px-4 text-center">
      <ShieldAlert className="mb-4 h-16 w-16 text-state-danger" />
      <h1 className="text-2xl font-bold text-content-primary">Acesso negado</h1>
      <p className="mt-2 max-w-sm text-sm text-content-secondary">
        Você não tem permissão para acessar esta página. Se acha que isso é um engano,
        fale com o administrador do sistema.
      </p>
      <button onClick={() => navigate("/dashboard")} className="btn-secondary mt-6">
        <ArrowLeft className="h-4 w-4" />
        Voltar ao início
      </button>
    </div>
  );
}
