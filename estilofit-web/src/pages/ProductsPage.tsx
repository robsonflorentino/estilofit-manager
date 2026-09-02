import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { Shirt, Plus, Pencil, Power, Layers, Loader2, Search } from "lucide-react";
import { PageHeader } from "../components/PageHeader";
import { DataTable, type Column } from "../components/DataTable";
import { Badge } from "../components/Badge";
import { Modal } from "../components/Modal";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { Pagination } from "../components/Pagination";
import { productService } from "../services/productService";
import { categoryService } from "../services/categoryService";
import { getApiErrorMessage } from "../lib/api";
import type { ProductSummary } from "../types/product";
import { VariantsPanel } from "./VariantsPanel";

const PAGE_SIZE = 10;

const productSchema = z.object({
  name: z.string().min(2, "Nome deve ter entre 2 e 200 caracteres").max(200),
  description: z.string().optional(),
  categoryId: z.string().min(1, "Categoria é obrigatória"),
});
type ProductForm = z.infer<typeof productSchema>;

export function ProductsPage() {
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [nameFilter, setNameFilter] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("");

  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [toggling, setToggling] = useState<ProductSummary | null>(null);
  const [variantsOf, setVariantsOf] = useState<ProductSummary | null>(null);

  const filters = {
    name: nameFilter || undefined,
    categoryId: categoryFilter || undefined,
  };

  const { data, isLoading } = useQuery({
    queryKey: ["products", page, filters],
    queryFn: () => productService.list(page, PAGE_SIZE, filters),
  });

  const { data: categories = [] } = useQuery({
    queryKey: ["categories", { showInactive: false }],
    queryFn: () => categoryService.list(true),
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["products"] });

  const form = useForm<ProductForm>({ resolver: zodResolver(productSchema) });

  const saveMutation = useMutation({
    mutationFn: (f: ProductForm) =>
      editingId
        ? productService.update(editingId, { name: f.name, description: f.description, categoryId: f.categoryId })
        : productService.create({ name: f.name, description: f.description, categoryId: f.categoryId }),
    onSuccess: () => {
      toast.success(editingId ? "Produto atualizado." : "Produto criado.");
      setFormOpen(false);
      setEditingId(null);
      form.reset();
      invalidate();
    },
    onError: (error) => toast.error(getApiErrorMessage(error, "Não foi possível salvar o produto.")),
  });

  const statusMutation = useMutation({
    mutationFn: (p: ProductSummary) => productService.updateStatus(p.id, !p.active),
    onSuccess: (updated) => {
      toast.success(updated.active ? "Produto ativado." : "Produto desativado.");
      setToggling(null);
      invalidate();
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error));
      setToggling(null);
    },
  });

  const openCreate = () => {
    setEditingId(null);
    form.reset({ name: "", description: "", categoryId: "" });
    setFormOpen(true);
  };

  const openEdit = async (p: ProductSummary) => {
    // busca o detalhe para preencher descrição
    const detail = await productService.getById(p.id);
    setEditingId(p.id);
    form.reset({
      name: detail.name,
      description: detail.description ?? "",
      categoryId: detail.category.id,
    });
    setFormOpen(true);
  };

  const columns: Column<ProductSummary>[] = [
    { header: "Produto", render: (p) => <span className="font-medium">{p.name}</span> },
    { header: "Categoria", render: (p) => <span className="text-content-secondary">{p.category.name}</span> },
    { header: "Variações", render: (p) => <span>{p.variantCount}</span> },
    { header: "Estoque", render: (p) => <span>{p.totalStock}</span> },
    {
      header: "Status",
      render: (p) =>
        p.active ? <Badge variant="success">Ativo</Badge> : <Badge variant="danger">Inativo</Badge>,
    },
    {
      header: "Ações",
      className: "text-right",
      render: (p) => (
        <div className="flex justify-end gap-1">
          <button onClick={() => setVariantsOf(p)} className="rounded-btn p-2 text-content-secondary hover:bg-bg-surface-raised hover:text-brand-purple" title="Variações">
            <Layers className="h-4 w-4" />
          </button>
          <button onClick={() => openEdit(p)} className="rounded-btn p-2 text-content-secondary hover:bg-bg-surface-raised hover:text-brand-purple" title="Editar">
            <Pencil className="h-4 w-4" />
          </button>
          <button onClick={() => setToggling(p)} className="rounded-btn p-2 text-content-secondary hover:bg-bg-surface-raised hover:text-content-primary" title={p.active ? "Desativar" : "Ativar"}>
            <Power className="h-4 w-4" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        icon={Shirt}
        title="Produtos"
        description="Cadastre produtos e gerencie suas variações (tamanho e cor)."
        action={
          <button className="btn-primary" onClick={openCreate}>
            <Plus className="h-4 w-4" />
            Novo produto
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
            onChange={(e) => { setNameFilter(e.target.value); setPage(0); }}
          />
        </div>
        <select
          className="input-base w-52"
          value={categoryFilter}
          onChange={(e) => { setCategoryFilter(e.target.value); setPage(0); }}
        >
          <option value="">Todas as categorias</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </select>
      </div>

      <DataTable
        columns={columns}
        rows={data?.content ?? []}
        rowKey={(p) => p.id}
        loading={isLoading}
        emptyMessage="Nenhum produto cadastrado."
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

      {/* Modal criar/editar produto */}
      <Modal
        open={formOpen}
        title={editingId ? "Editar produto" : "Novo produto"}
        onClose={() => setFormOpen(false)}
        footer={
          <>
            <button className="btn-secondary" onClick={() => setFormOpen(false)} disabled={saveMutation.isPending}>Cancelar</button>
            <button className="btn-primary" onClick={form.handleSubmit((f) => saveMutation.mutate(f))} disabled={saveMutation.isPending}>
              {saveMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : "Salvar"}
            </button>
          </>
        }
      >
        <form className="space-y-4" onSubmit={form.handleSubmit((f) => saveMutation.mutate(f))} noValidate>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-content-secondary">Nome <span className="text-brand-purple">*</span></label>
            <input className="input-base" autoFocus placeholder="Ex: Blusa Listrada" {...form.register("name")} />
            {form.formState.errors.name && <p className="mt-1 text-xs text-state-danger">{form.formState.errors.name.message}</p>}
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-content-secondary">Descrição</label>
            <textarea className="input-base min-h-20" placeholder="Detalhes do produto..." {...form.register("description")} />
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-content-secondary">Categoria <span className="text-brand-purple">*</span></label>
            <select className="input-base" {...form.register("categoryId")}>
              <option value="">Selecione...</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
            {form.formState.errors.categoryId && <p className="mt-1 text-xs text-state-danger">{form.formState.errors.categoryId.message}</p>}
          </div>
        </form>
      </Modal>

      {/* Painel de variações */}
      {variantsOf && (
        <VariantsPanel
          product={variantsOf}
          onClose={() => { setVariantsOf(null); invalidate(); }}
        />
      )}

      {/* Confirmação de status */}
      <ConfirmDialog
        open={toggling !== null}
        title={toggling?.active ? "Desativar produto" : "Ativar produto"}
        message={
          toggling?.active
            ? `Desativar "${toggling?.name}"? Não será possível adicionar variações enquanto estiver inativo.`
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
