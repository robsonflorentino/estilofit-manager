import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { Users, Plus, Pencil, Power, KeyRound, Loader2, Search } from "lucide-react";
import { PageHeader } from "../components/PageHeader";
import { DataTable, type Column } from "../components/DataTable";
import { Badge } from "../components/Badge";
import { Modal } from "../components/Modal";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { Pagination } from "../components/Pagination";
import { userService } from "../services/userService";
import { getApiErrorMessage } from "../lib/api";
import type { Role, UserResponse } from "../types/api";

const PAGE_SIZE = 10;

const ROLE_LABELS: Record<Role, string> = {
  ADMIN: "Administrador",
  MANAGER: "Gestor",
  SELLER: "Vendedor",
};

// ── Schemas ─────────────────────────────────────────────────────────────────
const createSchema = z.object({
  name: z.string().min(2, "Nome deve ter no mínimo 2 caracteres"),
  email: z.string().min(1, "E-mail é obrigatório").email("E-mail inválido"),
  password: z.string().min(8, "Senha deve ter no mínimo 8 caracteres"),
  role: z.enum(["ADMIN", "MANAGER", "SELLER"]),
});
type CreateForm = z.infer<typeof createSchema>;

const editSchema = z.object({
  name: z.string().min(2, "Nome deve ter no mínimo 2 caracteres"),
  email: z.string().min(1, "E-mail é obrigatório").email("E-mail inválido"),
  role: z.enum(["ADMIN", "MANAGER", "SELLER"]),
});
type EditForm = z.infer<typeof editSchema>;

const passwordSchema = z.object({
  newPassword: z.string().min(8, "Senha deve ter no mínimo 8 caracteres"),
});
type PasswordForm = z.infer<typeof passwordSchema>;

export function UsersPage() {
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [nameFilter, setNameFilter] = useState("");
  const [roleFilter, setRoleFilter] = useState<Role | "">("");
  const [activeFilter, setActiveFilter] = useState<"" | "true" | "false">("");

  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<UserResponse | null>(null);
  const [toggling, setToggling] = useState<UserResponse | null>(null);
  const [resetting, setResetting] = useState<UserResponse | null>(null);

  const filters = {
    name: nameFilter || undefined,
    role: roleFilter || undefined,
    active: activeFilter === "" ? undefined : activeFilter === "true",
  };

  const { data, isLoading } = useQuery({
    queryKey: ["users", page, filters],
    queryFn: () => userService.list(page, PAGE_SIZE, filters),
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["users"] });

  const resetToFirstPage = () => setPage(0);

  // ── Forms ────────────────────────────────────────────────────────────────
  const createForm = useForm<CreateForm>({ resolver: zodResolver(createSchema) });
  const editForm = useForm<EditForm>({ resolver: zodResolver(editSchema) });
  const passwordForm = useForm<PasswordForm>({ resolver: zodResolver(passwordSchema) });

  // ── Mutations ──────────────────────────────────────────────────────────────
  const createMutation = useMutation({
    mutationFn: (form: CreateForm) => userService.create(form),
    onSuccess: () => {
      toast.success("Usuário criado.");
      setCreateOpen(false);
      createForm.reset();
      invalidate();
    },
    onError: (error) => {
      const message = getApiErrorMessage(error, "Não foi possível criar o usuário.");
      if (message.toLowerCase().includes("cadastrado")) {
        createForm.setError("email", { message });
      } else {
        toast.error(message);
      }
    },
  });

  const editMutation = useMutation({
    mutationFn: (form: EditForm) => userService.update(editing!.id, form),
    onSuccess: () => {
      toast.success("Usuário atualizado.");
      setEditing(null);
      invalidate();
    },
    onError: (error) => {
      const message = getApiErrorMessage(error, "Não foi possível atualizar.");
      if (message.toLowerCase().includes("uso")) {
        editForm.setError("email", { message });
      } else {
        toast.error(message);
      }
    },
  });

  const statusMutation = useMutation({
    mutationFn: (u: UserResponse) => userService.updateStatus(u.id, !u.active),
    onSuccess: (updated) => {
      toast.success(updated.active ? "Usuário ativado." : "Usuário desativado.");
      setToggling(null);
      invalidate();
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error));
      setToggling(null);
    },
  });

  const passwordMutation = useMutation({
    mutationFn: (form: PasswordForm) => userService.resetPassword(resetting!.id, form.newPassword),
    onSuccess: () => {
      toast.success("Senha redefinida.");
      setResetting(null);
      passwordForm.reset();
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  });

  // ── Handlers ───────────────────────────────────────────────────────────────
  const openCreate = () => {
    createForm.reset({ name: "", email: "", password: "", role: "SELLER" });
    setCreateOpen(true);
  };

  const openEdit = (u: UserResponse) => {
    editForm.reset({ name: u.name, email: u.email, role: u.role });
    setEditing(u);
  };

  const openReset = (u: UserResponse) => {
    passwordForm.reset({ newPassword: "" });
    setResetting(u);
  };

  // ── Colunas ────────────────────────────────────────────────────────────────
  const columns: Column<UserResponse>[] = [
    { header: "Nome", render: (u) => <span className="font-medium">{u.name}</span> },
    { header: "E-mail", render: (u) => <span className="text-content-secondary">{u.email}</span> },
    { header: "Perfil", render: (u) => <Badge variant="purple">{ROLE_LABELS[u.role]}</Badge> },
    {
      header: "Status",
      render: (u) =>
        u.active ? <Badge variant="success">Ativo</Badge> : <Badge variant="danger">Inativo</Badge>,
    },
    {
      header: "Ações",
      className: "text-right",
      render: (u) => (
        <div className="flex justify-end gap-1">
          <button onClick={() => openEdit(u)} className="rounded-btn p-2 text-content-secondary hover:bg-bg-surface-raised hover:text-brand-purple" title="Editar">
            <Pencil className="h-4 w-4" />
          </button>
          <button onClick={() => openReset(u)} className="rounded-btn p-2 text-content-secondary hover:bg-bg-surface-raised hover:text-content-primary" title="Redefinir senha">
            <KeyRound className="h-4 w-4" />
          </button>
          <button onClick={() => setToggling(u)} className="rounded-btn p-2 text-content-secondary hover:bg-bg-surface-raised hover:text-content-primary" title={u.active ? "Desativar" : "Ativar"}>
            <Power className="h-4 w-4" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        icon={Users}
        title="Usuários"
        description="Gerencie os usuários e perfis de acesso ao sistema."
        action={
          <button className="btn-primary" onClick={openCreate}>
            <Plus className="h-4 w-4" />
            Novo usuário
          </button>
        }
      />

      {/* Filtros */}
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-content-muted" />
          <input
            className="input-base w-64 pl-9"
            placeholder="Buscar por nome..."
            value={nameFilter}
            onChange={(e) => {
              setNameFilter(e.target.value);
              resetToFirstPage();
            }}
          />
        </div>
        <select
          className="input-base w-44"
          value={roleFilter}
          onChange={(e) => {
            setRoleFilter(e.target.value as Role | "");
            resetToFirstPage();
          }}
        >
          <option value="">Todos os perfis</option>
          <option value="ADMIN">Administrador</option>
          <option value="MANAGER">Gestor</option>
          <option value="SELLER">Vendedor</option>
        </select>
        <select
          className="input-base w-40"
          value={activeFilter}
          onChange={(e) => {
            setActiveFilter(e.target.value as "" | "true" | "false");
            resetToFirstPage();
          }}
        >
          <option value="">Todos os status</option>
          <option value="true">Ativos</option>
          <option value="false">Inativos</option>
        </select>
      </div>

      <DataTable
        columns={columns}
        rows={data?.content ?? []}
        rowKey={(u) => u.id}
        loading={isLoading}
        emptyMessage="Nenhum usuário encontrado."
      />

      {data && (
        <Pagination
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          onPageChange={setPage}
        />
      )}

      {/* Modal criar */}
      <Modal
        open={createOpen}
        title="Novo usuário"
        onClose={() => setCreateOpen(false)}
        footer={
          <>
            <button className="btn-secondary" onClick={() => setCreateOpen(false)} disabled={createMutation.isPending}>Cancelar</button>
            <button className="btn-primary" onClick={createForm.handleSubmit((f) => createMutation.mutate(f))} disabled={createMutation.isPending}>
              {createMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : "Criar"}
            </button>
          </>
        }
      >
        <form className="space-y-4" onSubmit={createForm.handleSubmit((f) => createMutation.mutate(f))} noValidate>
          <Field label="Nome" error={createForm.formState.errors.name?.message}>
            <input className="input-base" autoFocus {...createForm.register("name")} />
          </Field>
          <Field label="E-mail" error={createForm.formState.errors.email?.message}>
            <input className="input-base" type="email" {...createForm.register("email")} />
          </Field>
          <Field label="Senha" error={createForm.formState.errors.password?.message}>
            <input className="input-base" type="password" {...createForm.register("password")} />
          </Field>
          <Field label="Perfil" error={createForm.formState.errors.role?.message}>
            <select className="input-base" {...createForm.register("role")}>
              <option value="SELLER">Vendedor</option>
              <option value="MANAGER">Gestor</option>
              <option value="ADMIN">Administrador</option>
            </select>
          </Field>
        </form>
      </Modal>

      {/* Modal editar */}
      <Modal
        open={editing !== null}
        title="Editar usuário"
        onClose={() => setEditing(null)}
        footer={
          <>
            <button className="btn-secondary" onClick={() => setEditing(null)} disabled={editMutation.isPending}>Cancelar</button>
            <button className="btn-primary" onClick={editForm.handleSubmit((f) => editMutation.mutate(f))} disabled={editMutation.isPending}>
              {editMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : "Salvar"}
            </button>
          </>
        }
      >
        <form className="space-y-4" onSubmit={editForm.handleSubmit((f) => editMutation.mutate(f))} noValidate>
          <Field label="Nome" error={editForm.formState.errors.name?.message}>
            <input className="input-base" autoFocus {...editForm.register("name")} />
          </Field>
          <Field label="E-mail" error={editForm.formState.errors.email?.message}>
            <input className="input-base" type="email" {...editForm.register("email")} />
          </Field>
          <Field label="Perfil" error={editForm.formState.errors.role?.message}>
            <select className="input-base" {...editForm.register("role")}>
              <option value="SELLER">Vendedor</option>
              <option value="MANAGER">Gestor</option>
              <option value="ADMIN">Administrador</option>
            </select>
          </Field>
        </form>
      </Modal>

      {/* Modal redefinir senha */}
      <Modal
        open={resetting !== null}
        title={`Redefinir senha de ${resetting?.name ?? ""}`}
        onClose={() => setResetting(null)}
        footer={
          <>
            <button className="btn-secondary" onClick={() => setResetting(null)} disabled={passwordMutation.isPending}>Cancelar</button>
            <button className="btn-primary" onClick={passwordForm.handleSubmit((f) => passwordMutation.mutate(f))} disabled={passwordMutation.isPending}>
              {passwordMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : "Redefinir"}
            </button>
          </>
        }
      >
        <form onSubmit={passwordForm.handleSubmit((f) => passwordMutation.mutate(f))} noValidate>
          <Field label="Nova senha" error={passwordForm.formState.errors.newPassword?.message}>
            <input className="input-base" type="password" autoFocus {...passwordForm.register("newPassword")} />
          </Field>
        </form>
      </Modal>

      {/* Confirmação de status */}
      <ConfirmDialog
        open={toggling !== null}
        title={toggling?.active ? "Desativar usuário" : "Ativar usuário"}
        message={
          toggling?.active
            ? `Desativar "${toggling?.name}"? Ele não poderá mais fazer login.`
            : `Ativar "${toggling?.name}"?`
        }
        confirmLabel={toggling?.active ? "Desativar" : "Ativar"}
        danger={toggling?.active}
        loading={statusMutation.isPending}
        onConfirm={() => toggling && statusMutation.mutate(toggling)}
        onCancel={() => setToggling(null)}
      />
    </div>
  );
}

// ── Campo de formulário reutilizável local ──────────────────────────────────
function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <div>
      <label className="mb-1.5 block text-sm font-medium text-content-secondary">
        {label} <span className="text-brand-purple">*</span>
      </label>
      {children}
      {error && <p className="mt-1 text-xs text-state-danger">{error}</p>}
    </div>
  );
}
