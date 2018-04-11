CREATE TABLE ZZ_EMPLOYEES (
id number,
first_name varchar2(40),
last_name varchar2(40),
title varchar2(50)
);

ALTER TABLE ZZ_EMPLOYEES 
ADD CONSTRAINT pk_ZZ_EMPLOYEES PRIMARY KEY (id);