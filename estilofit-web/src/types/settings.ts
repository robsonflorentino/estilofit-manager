export type SettingType = "INTEGER" | "DECIMAL";

export interface SystemSetting {
  key: string;
  label: string;
  value: string;
  type: SettingType;
  min: number;
  max: number | null;
  description: string | null;
  updatedAt: string | null;
  updatedByName: string | null;
}
