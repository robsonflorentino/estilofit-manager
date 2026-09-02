import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { Truck, Plus, Pencil, Power, Loader2, Search } from "lucide-react";
import { PageHeader } from "../components/PageHeader";
import { DataTable, type Column } from "../components/DataTable";
import { Badge } from "../components/Badge";
import { Modal } from "../components/Modal";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { Pagination } from "../components/Pagination";
import { supplierService } from "../services/supplierService";
import { getApiErrorMessage } from "../lib/api";
import type { Supplier } from "../types/supplier";

const PAGE_SIZE = 10;

const supplierSchema = z.object({
  name: z.string().min(2, "Nome deve ter entre 2 e 200 caracteres").max(200),
  contactPhone: z.string().optional(),
  contactEmail: z.string().email("E-mail inválido").optional().or(z.literal("")),
  whatsapp: z.string().optional(),
  cnpj: z.string().max(18, "CNPJ deve ter no máximo 18 caracteres").optional(),
  address: z.string().optional(),
  notes: z.string().optional(),
});
type SupplierForm = z.infer<typeof supplierSchema>;

export function SuppliersPage() {
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [nameFilter, setNameFilter] = useState("");
  const [activeFilter, setActiveFilter] = useState<"" | "true" | "false">("");

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Supplier | null>(null);
  const [toggling, setToggling] = useState<Supplier | null>(null);

  const filters = {
    name: nameFilter || undefined,
    active: activeFilter === "" ? undefined : activeFilter === "true",
  };

  const { data, isLoading } = useQuery({
    queryKey: ["suppliers", page, filters],
    queryFn: () => supplierService.list(page, PAGE_SIZE, filters),
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["suppliers"] });

  const form = useForm<SupplierForm>({ resolver: zodResolver(supplierSchema) });

  const saveMutation = useMutation({
    mutationFn: (f: SupplierForm) =>
      editing ? supplierService.update(editing.id, f) : supplierService.create(f),
    onSuccess: () => {
      toast.success(editing ? "Fornecedor atualizado." : "Fornecedor criado.");
      setFormOpen(false);
      setEditing(null);
      form.reset();
      invalidate();
    },
    onError: (error) => {
      const message = getApiErrorMessage(error, "Não foi possível salvar o fornecedor.");
      if (message.toLowerCase().includes("cnpj")) {
        form.setError("cnpj", { message });
      } else {
        toast.error(message);
      }
    },
  });

  const statusMutation = useMutation({
    mutationFn: (s: Supplier) => supplierService.updateStatus(s.id, !s.active),
    onSuccess: (updated) => {
      toast.success(updated.active ? "Fornecedor ativado." : "Fornecedor desativado.");
      setToggling(null);
      invalidate();
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error));
      setToggling(null);
    },
  });

  const openCreate = () => {
    setEditing(null);
    form.reset({ name: "", contactPhone: "", contactEmail: "", whatsapp: "", cnpj: "", address: "", notes: "" });
    setFormOpen(true);
  };

  const openEdit = (s: Supplier) => {
    setEditing(s);
    form.reset({
      name: s.name,
      contactPhone: s.contactPhone ?? "",
      contactEmail: s.contactEmail ?? "",
      whatsapp: s.whatsapp ?? "",
      cnpj: s.cnpj ?? "",
      address: s.address ?? "",
      notes: s.notes ?? "",
    });
    setFormOpen(true);
  };

  const columns: Column<Supplier>[] = [
    { header: "Nome", render: (s) => <span className="font-medium">{s.name}</span> },
    { header: "Contato", render: (s) => <span className="text-content-secondary">{s.contactPhone ?? s.whatsapp ?? "—"}</span> },
    { header: "CNPJ", render: (s) => <span className="text-content-secondary">{s.cnpj ?? "—"}</span> },
    {
      header: "Status",
      render: (s) => (s.active ? <Badge variant="success">Ativo</Badge> : <Badge variant="danger">Inativo</Badge>),
    },
    {
      header: "Ações",
      className: "text-right",
      render: (s) => (
        <div className="flex justify-end gap-1">
          <button onClick={() => openEdit(s)} className="rounded-btn p-2 text-content-secondary hover:bg-bg-surface-raised hover:text-brand-purple" title="Editar">
            <Pencil className="h-4 w-4" />
          </button>
          <button onClick={() => setToggling(s)} className="rounded-btn p-2 text-content-secondary hover:bg-bg-surface-raised hover:text-content-primary" title={s.active ? "Desativar" : "Ativar"}>
            <Power className="h-4 w-4" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        icon={Truck}
        title="Fornecedores"
        description="Cadastre os fornecedores de quem a loja compra mercadoria."
        action={
          <button className="btn-primary" onClick={openCreate}>
            <Plus className="h-4 w-4" />
            Novo fornecedor
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
          className="input-base w-40"
          value={activeFilter}
          onChange={(e) => { setActiveFilter(e.target.value as "" | "true" | "false"); setPage(0); }}
        >
          <option value="">Todos os status</option>
          <option value="true">Ativos</option>
          <option value="false">Inativos</option>
        </select>
      </div>

      <DataTable
        columns={columns}
        rows={data?.content ?? []}
        rowKey={(s) => s.id}
        loading={isLoading}
        emptyMessage="Nenhum fornecedor cadastrado."
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

      {/* Modal criar/editar */}
      <Modal
        open={formOpen}
        title={editing ? "Editar fornecedor" : "Novo fornecedor"}
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
            <input className="input-base" autoFocus placeholder="Razão social ou nome" {...form.register("name")} />
            {form.formState.errors.name && <p className="mt-1 text-xs text-state-danger">{form.formState.errors.name.message}</p>}
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1.5 block text-sm font-medium text-content-secondary">Telefone</label>
              <input className="input-base" placeholder="(11) 99999-0000" {...form.register("contactPhone")} />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-content-secondary">WhatsApp</label>
              <input className="input-base" placeholder="(11) 99999-0000" {...form.register("whatsapp")} />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1.5 block text-sm font-medium text-content-secondary">E-mail</label>
              <input className="input-base" type="email" placeholder="contato@fornecedor.com" {...form.register("contactEmail")} />
              {form.formState.errors.contactEmail && <p className="mt-1 text-xs text-state-danger">{form.formState.errors.contactEmail.message}</p>}
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-content-secondary">CNPJ</label>
              <input className="input-base" placeholder="00.000.000/0000-00" {...form.register("cnpj")} />
              {form.formState.errors.cnpj && <p className="mt-1 text-xs text-state-danger">{form.formState.errors.cnpj.message}</p>}
            </div>
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-content-secondary">Endereço</label>
            <input className="input-base" placeholder="Rua, número, cidade/UF" {...form.register("address")} />
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-content-secondary">Observações</label>
            <textarea className="input-base min-h-16" placeholder="Condições de pagamento, etc." {...form.register("notes")} />
          </div>
        </form>
      </Modal>

      {/* Confirmação de status */}
      <ConfirmDialog
        open={toggling !== null}
        title={toggling?.active ? "Desativar fornecedor" : "Ativar fornecedor"}
        message={
          toggling?.active
            ? `Desativar "${toggling?.name}"? Ele deixará de aparecer nas listagens padrão.`
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
