import { api } from "../lib/api";
import type { SystemSetting } from "../types/settings";

export const settingsService = {
  async list(): Promise<SystemSetting[]> {
    const { data } = await api.get<SystemSetting[]>("/settings");
    return data;
  },

  async update(key: string, value: string): Promise<SystemSetting> {
    const { data } = await api.put<SystemSetting>(`/settings/${key}`, { value });
    return data;
  },
};
