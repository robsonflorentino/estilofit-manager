import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { Tag, Plus, Pencil, Power, Loader2 } from "lucide-react";
import { PageHeader } from "../components/PageHeader";
import { DataTable, type Column } from "../components/DataTable";
import { Badge } from "../components/Badge";
import { Modal } from "../components/Modal";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { categoryService } from "../services/categoryService";
import { getApiErrorMessage } from "../lib/api";
import type { Category } from "../types/category";

const categorySchema = z.object({
  name: z
    .string()
    .min(2, "Nome deve ter entre 2 e 100 caracteres")
    .max(100, "Nome deve ter entre 2 e 100 caracteres"),
});
type CategoryForm = z.infer<typeof categorySchema>;

export function CategoriesPage() {
  const queryClient = useQueryClient();
  const [showInactive, setShowInactive] = useState(false);
  const [editing, setEditing] = useState<Category | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [toggling, setToggling] = useState<Category | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<CategoryForm>({ resolver: zodResolver(categorySchema) });

  // ── Query: listagem ────────────────────────────────────────────────────
  const { data: categories = [], isLoading } = useQuery({
    queryKey: ["categories", { showInactive }],
    queryFn: () => categoryService.list(!showInactive),
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["categories"] });

  // ── Mutations ────────────────────────────────────────────────────────────
  const saveMutation = useMutation({
    mutationFn: (form: CategoryForm) =>
      editing
        ? categoryService.rename(editing.id, form)
        : categoryService.create(form),
    onSuccess: () => {
      toast.success(editing ? "Categoria atualizada." : "Categoria criada.");
      closeForm();
      invalidate();
    },
    onError: (error) => {
      // 409 (nome duplicado) vira erro no campo; demais viram toast
      const message = getApiErrorMessage(error, "Não foi possível salvar.");
      if (message.toLowerCase().includes("já existe")) {
        setError("name", { message });
      } else {
        toast.error(message);
      }
    },
  });

  const statusMutation = useMutation({
    mutationFn: (cat: Category) => categoryService.updateStatus(cat.id, !cat.active),
    onSuccess: (updated) => {
      toast.success(updated.active ? "Categoria ativada." : "Categoria desativada.");
      setToggling(null);
      invalidate();
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "Não foi possível alterar o status."));
      setToggling(null);
    },
  });

  // ── Handlers do formulário ───────────────────────────────────────────────
  const openCreate = () => {
    setEditing(null);
    reset({ name: "" });
    setFormOpen(true);
  };

  const openEdit = (cat: Category) => {
    setEditing(cat);
    reset({ name: cat.name });
    setFormOpen(true);
  };

  const closeForm = () => {
    setFormOpen(false);
    setEditing(null);
    reset({ name: "" });
  };

  const onSubmit = (form: CategoryForm) => saveMutation.mutate(form);

  // ── Colunas da tabela ─────────────────────────────────────────────────────
  const columns: Column<Category>[] = [
    { header: "Nome", render: (c) => <span className="font-medium">{c.name}</span> },
    {
      header: "Status",
      render: (c) =>
        c.active ? (
          <Badge variant="success">Ativa</Badge>
        ) : (
          <Badge variant="danger">Inativa</Badge>
        ),
    },
    {
      header: "Ações",
      className: "text-right",
      render: (c) => (
        <div className="flex justify-end gap-1">
          <button
            onClick={() => openEdit(c)}
            className="rounded-btn p-2 text-content-secondary transition-colors hover:bg-bg-surface-raised hover:text-brand-purple"
            title="Renomear"
          >
            <Pencil className="h-4 w-4" />
          </button>
          <button
            onClick={() => setToggling(c)}
            className="rounded-btn p-2 text-content-secondary transition-colors hover:bg-bg-surface-raised hover:text-content-primary"
            title={c.active ? "Desativar" : "Ativar"}
          >
            <Power className="h-4 w-4" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        icon={Tag}
        title="Categorias"
        description="Gerencie as categorias de produtos da loja."
        action={
          <button className="btn-primary" onClick={openCreate}>
            <Plus className="h-4 w-4" />
            Nova categoria
          </button>
        }
      />

      {/* Filtro de status */}
      <label className="mb-4 flex w-fit cursor-pointer items-center gap-2 text-sm text-content-secondary">
        <input
          type="checkbox"
          checked={showInactive}
          onChange={(e) => setShowInactive(e.target.checked)}
          className="accent-brand-purple"
        />
        Mostrar inativas
      </label>

      <DataTable
        columns={columns}
        rows={categories}
        rowKey={(c) => c.id}
        loading={isLoading}
        emptyMessage="Nenhuma categoria cadastrada."
      />

      {/* Modal criar/renomear */}
      <Modal
        open={formOpen}
        title={editing ? "Renomear categoria" : "Nova categoria"}
        onClose={closeForm}
        footer={
          <>
            <button className="btn-secondary" onClick={closeForm} disabled={saveMutation.isPending}>
              Cancelar
            </button>
            <button
              className="btn-primary"
              onClick={handleSubmit(onSubmit)}
              disabled={saveMutation.isPending}
            >
              {saveMutation.isPending ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Salvando...
                </>
              ) : (
                "Salvar"
              )}
            </button>
          </>
        }
      >
        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <label htmlFor="name" className="mb-1.5 block text-sm font-medium text-content-secondary">
            Nome <span className="text-brand-purple">*</span>
          </label>
          <input
            id="name"
            autoFocus
            className="input-base"
            placeholder="Ex: Blusas"
            {...register("name")}
          />
          {errors.name && <p className="mt-1 text-xs text-state-danger">{errors.name.message}</p>}
        </form>
      </Modal>

      {/* Confirmação de ativar/desativar */}
      <ConfirmDialog
        open={toggling !== null}
        title={toggling?.active ? "Desativar categoria" : "Ativar categoria"}
        message={
          toggling?.active
            ? `Desativar "${toggling?.name}"? Ela deixará de aparecer nas listagens padrão.`
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
