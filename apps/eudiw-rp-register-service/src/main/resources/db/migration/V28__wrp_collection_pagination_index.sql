CREATE INDEX `idx_wrp_active_created_id`
    ON `wallet_relying_party` (`active`, `created_ms`, `id`);
