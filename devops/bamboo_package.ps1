#  Copy files and validate
# Must run as administrator
Enable-PsRemoting -Force
write-Host "#----- Release $env:Bamboo_deploy_version -----#"

# Build a credential - you can do this in another file for security
$localuser = "dbmguest"
$server = "10.0.0.12"
$domain = "DBMTemplate"
$curl = "C:\Automation\dbm_demo\devops\lib\curl.exe"
$artifactory_url =  "$env:bamboo_dbm_artifactory_url"
$artifactory_user =  "$env:bamboo_dbm_artifactory_user"
$username = "$domain\$env:bamboo_dbm_username"
$password = "$env:bamboo_dbm_password"
$pipeline = "$env:bamboo_dbm_pipeline"
$auto_path = "$env:bamboo_dbm_base_path\$pipeline"
$base_schema = "$env:bamboo_dbm_base_schema"
$java_cmd = "$env:bamboo_dbm_java_cmd"
$dbm_task = "Package"
$version = "$env:bamboo_dbm_version"
$env = "$env:bamboo_dbm_environment"

write-host "#=> Changing user to $username"

$cred = New-Object System.Management.Automation.PSCredential -ArgumentList @($username,(ConvertTo-SecureString -String $password -AsPlainText -Force))
#$sess = New-PSSession -ComputerName $server -Credential $cred

write-Host "#----- Release $env:Bamboo_deploy_version -----#"
# Download the package from Artifactory
[String]$dbm_cmd = @"
C:;
cd "$auto_path\$base_schema" 
& $curl -u $($artifactory_user):$($password) -O "$artifactory_url/$pipeline/$($version).zip"
"@
# invoke
write-Host "#=> Running: $dbm_cmd"
[ScriptBlock]$script = [ScriptBlock]::Create($dbm_cmd)
Invoke-Command -ComputerName $server -Credential $cred -ScriptBlock $script -ErrorVariable errmsg

# Now run the automation command
# Build with here string
# java -jar DBmaestroAgent.jar -Validate  -ProjectName human_resources -EnvName integration -Server 10.1.0.15 -PackageName V1.0.1
[String]$dbm_cmd = @"
C:;
cd "$auto_path" 
$java_cmd -$dbm_task -ProjectName $pipeline -Server $server
"@
# invoke
write-Host "#=> Running: $dbm_cmd"
[ScriptBlock]$script = [ScriptBlock]::Create($dbm_cmd)
Invoke-Command -ComputerName $server -Credential $cred -ScriptBlock $script -ErrorVariable errmsg