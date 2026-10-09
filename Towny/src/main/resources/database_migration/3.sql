-- Change how residents are jailed. Save a unix timestamp of when they're supposed to be released, as opposed to saving their jail sentence in hours
-- fail-off
ALTER TABLE `$PREFIX_RESIDENTS` DROP COLUMN jailHours;
-- fail-on
