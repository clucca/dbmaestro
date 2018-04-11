echo off
echo #----------------------------#
echo # Groovy Test Processor #
echo #----------------------------#
java -cp "C:\Automation\dbm_demo\devops\lib\*;C:\Program Files (x86)\Jenkins\war\WEB-INF\lib\*" groovy.ui.GroovyMain C:\Automation\dbm_demo\devops\tester.groovy %*
