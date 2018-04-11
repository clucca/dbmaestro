-- Add 401k columns
alter table employees add (has_401k char(1), amount_401k varchar(30));
exit;