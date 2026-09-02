import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { Lock, Mail, LogIn, Loader2 } from "lucide-react";
import { useAuthStore } from "../store/authStore";
import { getApiErrorMessage } from "../lib/api";

const loginSchema = z.object({
  email: z.string().min(1, "E-mail é obrigatório").email("E-mail inválido"),
  password: z.string().min(1, "Senha é obrigatória"),
});

type LoginForm = z.infer<typeof loginSchema>;

export function LoginPage() {
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);
  const [submitting, setSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginForm>({ resolver: zodResolver(loginSchema) });

  const onSubmit = async (data: LoginForm) => {
    setSubmitting(true);
    try {
      await login(data.email, data.password);
      toast.success("Bem-vinda de volta!");
      navigate("/dashboard", { replace: true });
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Não foi possível entrar."));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-full items-center justify-center bg-bg-base px-4">
      <div className="w-full max-w-md">
        {/* Marca */}
        <div className="mb-8 text-center">
          <h1 className="text-4xl font-bold tracking-tight">
            <span className="text-content-primary">ESTILO</span>
            <span className="text-brand-purple">FIT</span>
          </h1>
          <p className="mt-1 text-sm italic text-content-secondary">Moda Fitness</p>
        </div>

        {/* Card de login */}
        <div className="card">
          <h2 className="mb-1 text-xl font-semibold text-content-primary">Entrar</h2>
          <p className="mb-6 text-sm text-content-secondary">
            Acesse o painel de gestão da loja.
          </p>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
            {/* E-mail */}
            <div>
              <label htmlFor="email" className="mb-1.5 block text-sm font-medium text-content-secondary">
                E-mail
              </label>
              <div className="relative">
                <Mail className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-content-muted" />
                <input
                  id="email"
                  type="email"
                  autoComplete="email"
                  placeholder="voce@estilofit.com.br"
                  className="input-base pl-9"
                  {...register("email")}
                />
              </div>
              {errors.email && (
                <p className="mt-1 text-xs text-state-danger">{errors.email.message}</p>
              )}
            </div>

            {/* Senha */}
            <div>
              <label htmlFor="password" className="mb-1.5 block text-sm font-medium text-content-secondary">
                Senha
              </label>
              <div className="relative">
                <Lock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-content-muted" />
                <input
                  id="password"
                  type="password"
                  autoComplete="current-password"
                  placeholder="••••••••"
                  className="input-base pl-9"
                  {...register("password")}
                />
              </div>
              {errors.password && (
                <p className="mt-1 text-xs text-state-danger">{errors.password.message}</p>
              )}
            </div>

            <button type="submit" className="btn-primary w-full" disabled={submitting}>
              {submitting ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Entrando...
                </>
              ) : (
                <>
                  <LogIn className="h-4 w-4" />
                  Entrar
                </>
              )}
            </button>
          </form>
        </div>

        <p className="mt-6 text-center text-xs text-content-muted">
          EstiloFit Manager · Gestão de estoque e vendas
        </p>
      </div>
    </div>
  );
}
