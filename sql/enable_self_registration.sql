-- Enable the public registration flow. Organization assignment remains optional.
SET NAMES utf8mb4;
UPDATE sys_config
SET config_value = 'true', update_by = 'admin', update_time = NOW()
WHERE config_key = 'sys.account.registerUser';
