echo Uploading To Artifactory
%bamboo_dbm_automation_dir%\bamboo_groovy.bat action=upload_artifact
IF %ERRORLEVEL% NEQ 0 ( 
   echo Groovy Script failed code: %ERRORLEVEL%
   EXIT /b %ERRORLEVEL% 
)