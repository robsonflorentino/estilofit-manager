-- V11: Configurações do sistema
CREATE TABLE system_settings (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    key         VARCHAR(100) NOT NULL,
    value       VARCHAR(500) NOT NULL,
    description TEXT,
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by  UUID,

    CONSTRAINT pk_system_settings PRIMARY KEY (id),
    CONSTRAINT uq_system_settings_key UNIQUE (key),
    CONSTRAINT fk_system_settings_user FOREIGN KEY (updated_by) REFERENCES users (id)
);

CREATE INDEX idx_system_settings_key ON system_settings (key);
