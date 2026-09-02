import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { Settings as SettingsIcon, Loader2, Check } from "lucide-react";
import { PageHeader } from "../components/PageHeader";
import { settingsService } from "../services/settingsService";
import { getApiErrorMessage } from "../lib/api";
import type { SystemSetting } from "../types/settings";

function validate(setting: SystemSetting, value: string): string | null {
  const trimmed = value.trim();
  if (trimmed === "") return "Informe um valor.";
  const n = Number(trimmed);
  if (Number.isNaN(n)) return "Deve ser um número.";
  if (setting.type === "INTEGER" && !Number.isInteger(n)) return "Deve ser um número inteiro.";
  if (n < setting.min) return `Mínimo: ${setting.min}.`;
  if (setting.max != null && n > setting.max) return `Máximo: ${setting.max}.`;
  return null;
}

function SettingRow({ setting }: { setting: SystemSetting }) {
  const queryClient = useQueryClient();
  const [value, setValue] = useState(setting.value);

  const error = validate(setting, value);
  const dirty = value.trim() !== setting.value;

  const mutation = useMutation({
    mutationFn: () => settingsService.update(setting.key, value.trim()),
    onSuccess: () => {
      toast.success(`"${setting.label}" atualizado.`);
      queryClient.invalidateQueries({ queryKey: ["settings"] });
    },
    onError: (e) => toast.error(getApiErrorMessage(e, "Não foi possível salvar.")),
  });

  return (
    <div className="card">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="min-w-0 flex-1">
          <label className="block font-medium text-content-primary">{setting.label}</label>
          <p className="mt-0.5 text-xs text-content-muted">
            {setting.description ?? `Chave: ${setting.key}`}
            {setting.updatedByName && (
              <span className="ml-2">· última alteração por {setting.updatedByName}</span>
            )}
          </p>
        </div>
        <div className="flex items-end gap-2">
          <div>
            <input
              type="number"
              className="input-base w-40"
              value={value}
              step={setting.type === "INTEGER" ? "1" : "0.01"}
              min={setting.min}
              max={setting.max ?? undefined}
              onChange={(e) => setValue(e.target.value)}
            />
            {error && dirty && <p className="mt-1 text-xs text-state-danger">{error}</p>}
          </div>
          <button
            className="btn-primary"
            onClick={() => mutation.mutate()}
            disabled={!dirty || error != null || mutation.isPending}
          >
            {mutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Check className="h-4 w-4" />}
            Salvar
          </button>
        </div>
      </div>
    </div>
  );
}

export function SettingsPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["settings"],
    queryFn: () => settingsService.list(),
  });

  return (
    <div>
      <PageHeader
        icon={SettingsIcon}
        title="Configurações"
        description="Parâmetros do sistema usados nos cálculos de preço, estoque, pró-labore e alertas."
      />

      {isLoading ? (
        <div className="flex justify-center py-16">
          <Loader2 className="h-6 w-6 animate-spin text-brand-purple" />
        </div>
      ) : (
        <div className="space-y-4">
          {data?.map((s) => <SettingRow key={s.key} setting={s} />)}
        </div>
      )}
    </div>
  );
}
