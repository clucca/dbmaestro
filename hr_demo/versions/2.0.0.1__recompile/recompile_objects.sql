
SET SERVEROUTPUT ON
SET DEFINE OFF
SET BLOCKTERMINATOR OFF
SET SQLBLANKLINES ON

PROMPT Fetching invalid objects before deployment actions
PROMPT ****************************************************************************
SELECT OBJECT_NAME, OBJECT_TYPE, STATUS
FROM   ALL_OBJECTS
WHERE  STATUS <> 'VALID' AND OWNER = USER;


-- Step 3: Recompiling invalid objects
EXECUTE DBMS_OUTPUT.PUT_LINE('Step 3: Recompiling invalid objects');
BEGIN
DBMS_UTILITY.COMPILE_SCHEMA(USER, FALSE, FALSE);
END;
/