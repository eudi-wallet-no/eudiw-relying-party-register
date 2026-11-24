-- This removes all stored credential display names. Reconcile Flyway history before reapplying V42.
ALTER TABLE wallet_relying_party_service_intended_use_credential
    DROP COLUMN display_name;
