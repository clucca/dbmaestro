CREATE TABLE BB_EMPLOYEES (
id number,
first_name varchar2(40),
last_name varchar2(40),
title varchar2(50)
);

ALTER TABLE BB_EMPLOYEES 
ADD CONSTRAINT pk_BB_EMPLOYEES PRIMARY KEY (id);