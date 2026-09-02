-- V10: Alertas de promoção
CREATE TABLE promotion_alerts (
    id                UUID          NOT NULL DEFAULT gen_random_uuid(),
    variant_id        UUID          NOT NULL,
    days_without_sale INTEGER       NOT NULL,
    current_price     DECIMAL(10,2) NOT NULL,
    suggested_price   DECIMAL(10,2) NOT NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    snoozed_until     DATE,
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    resolved_at       TIMESTAMP,

    CONSTRAINT pk_promotion_alerts PRIMARY KEY (id),
    CONSTRAINT fk_promotion_alerts_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),
    CONSTRAINT ck_promotion_alerts_status  CHECK (status IN ('ACTIVE', 'DISMISSED', 'RESOLVED'))
);

CREATE INDEX idx_promotion_alerts_variant_id ON promotion_alerts (variant_id);
CREATE INDEX idx_promotion_alerts_status     ON promotion_alerts (status);
CREATE INDEX idx_promotion_alerts_created_at ON promotion_alerts (created_at);
