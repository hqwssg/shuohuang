-- Compatibility update for databases created before factor precision was fixed.
-- Keep six decimal places so electricity and other emission factors are not rounded
-- to two decimal places when persisted.
ALTER TABLE emission_default_factor
    MODIFY COLUMN factor_value DECIMAL(18,6) DEFAULT NULL;
