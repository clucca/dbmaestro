echo off
echo #----------------------------#
echo # Bamboo Groovy Processor #
echo #----------------------------#
java -cp "C:\Automation\dbm_demo\devops\lib\*;C:\Program Files (x86)\Jenkins\war\WEB-INF\lib\*" groovy.ui.GroovyMain C:\Automation\dbm_demo\devops\dbm_bamboo.groovy %*
IF %ERRORLEVEL% NEQ 0 ( 
   echo Groovy Script failed code: %ERRORLEVEL%
   EXIT /b %ERRORLEVEL% 
)