-- Update eaa_provider and pid_provider CAs to new CA structure

UPDATE entitlement SET ca_id = 'eaa_provider2' where ca_id = 'eaa_provider';
UPDATE entitlement SET ca_id = 'pid_provider2' where ca_id = 'pid_provider';
