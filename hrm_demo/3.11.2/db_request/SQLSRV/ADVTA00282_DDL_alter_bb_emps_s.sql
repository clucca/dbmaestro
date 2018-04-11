-- Alter and add 
alter table ZZ_EMPLOYEES alter column title VARCHAR(100)
GO

CREATE INDEX IDX_LAST_NAME ON ZZ_EMPLOYEES(last_name)
GO
