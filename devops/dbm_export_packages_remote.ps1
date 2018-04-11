#  Copy files and validate
# Must run as administrator
Enable-PsRemoting -Force
# Build a credential - you can do this in another file for security
$localuser = "dbmguest"
$server = "10.0.0.12"
$domain = "DBMTemplate"
$user = "$env:bamboo_dbm_username"
$username = "$domain\$user"
$password = "$env:bamboo_dbm_password"
$source_pipeline = "$env:bamboo_Source_Pipeline"
$target_pipeline = "$env:bamboo_Target_Pipeline"
$export_packages = "$env:bamboo_Export_Packages"
$auto_path = "C:\Automation\dbm_demo\devops"
$script_file = "dbm_api.bat"

$cred = New-Object System.Management.Automation.PSCredential -ArgumentList @($username,(ConvertTo-SecureString -String $password -AsPlainText -Force))
#$sess = New-PSSession -ComputerName $server -Credential $cred

write-Host "#----- Remote call to DBMconnect -----#"
write-Host "# Listing Packages"
write-Host "# Conn: \\$($server):$($auto_path)\$script_file"
[String]$dbm_cmd = @"
C:;
cd $auto_path
set TARGET_PIPELINE=$target_pipeline
set EXPORT_PACKAGES=$export_packages
$auto_path\$script_file action=packages ARG1=$source_pipeline
"@
# invoke
write-Host "#=> Running: $dbm_cmd"
[ScriptBlock]$script = [ScriptBlock]::Create($dbm_cmd)
Invoke-Command -ComputerName $server -Credential $cred -ScriptBlock $script -ErrorVariable errmsg

# Now run the export packages command
# Build with here string
[String]$dbm_cmd = @"
C:;
cd "$auto_path" 
set TARGET_PIPELINE=$target_pipeline
set EXPORT_PACKAGES=$export_packages
$auto_path\$script_file action=package_export ARG1=$source_pipeline
"@
# invoke
write-Host "#=> Running: $dbm_cmd"
[ScriptBlock]$script = [ScriptBlock]::Create($dbm_cmd)
Invoke-Command -ComputerName $server -Credential $cred -ScriptBlock $script -ErrorVariable errmsg
#if ($LASTEXITCODE -ne 0) {
#  exit(1)
#}

# Now run the packaging command
# Build with here string
[String]$dbm_cmd = @"
C:;
cd "$auto_path" 
set TARGET_PIPELINE=$target_pipeline
$auto_path\$script_file action=dbm_package
"@
# invoke
write-Host "#=> Running: $dbm_cmd"
[ScriptBlock]$script = [ScriptBlock]::Create($dbm_cmd)
Invoke-Command -ComputerName $server -Credential $cred -ScriptBlock $script -ErrorVariable errmsg
