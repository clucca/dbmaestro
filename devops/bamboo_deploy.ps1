#  Copy files and validate
# Must run as administrator
Enable-PsRemoting -Force
$ci_deploy = $False
write-Host "#----- DBmaestro Deploy Version: $env:bamboo_dbm_version -----#"
if (-not (Test-Path $env:bamboo_buildKey)) { $ci_deploy = $True }
# Build a credential - you can do this in another file for security
$localuser = "dbmguest"
$server = "10.0.0.12"
$domain = "DBMTemplate"
$username = "$domain\$env:bamboo_dbm_username"
$password = "$env:bamboo_dbm_password"
$pipeline = "$env:bamboo_dbm_pipeline"
$base_path = "$env:bamboo_dbm_base_path"
$env = "$env:bamboo_dbm_environment"

if ($ci_deploy){
  $pipeline = "$env:bamboo_dbm_ci_pipeline"
  $env = "$env:bamboo_dbm_ci_environment"
}
$auto_path = "$base_path\$pipeline"
$java_cmd = "$env:bamboo_dbm_java_cmd"
$dbm_task = "Upgrade"
$version = "$env:bamboo_dbm_version"

write-host "#=> Changing user to $username"

$cred = New-Object System.Management.Automation.PSCredential -ArgumentList @($username,(ConvertTo-SecureString -String $password -AsPlainText -Force))
#$sess = New-PSSession -ComputerName $server -Credential $cred

write-Host "#----- Release $env:Bamboo_deploy_version -----#"

# Now run the automation command
# Build with here string
# java -jar DBmaestroAgent.jar -Validate  -ProjectName human_resources -EnvName integration -Server 10.1.0.15 -PackageName V1.0.1
[String]$dbm_cmd = @"
C:;
cd "$auto_path" 
$java_cmd -$dbm_task -ProjectName $pipeline -EnvName $env -Server $server -packageName $version
"@
# invoke
write-Host "#=> Running: $dbm_cmd"
[ScriptBlock]$script = [ScriptBlock]::Create($dbm_cmd)
Invoke-Command -ComputerName $server -Credential $cred -ScriptBlock $script -ErrorVariable errmsg
#if ($LASTEXITCODE -ne 0) {
#  exit(1)
#}