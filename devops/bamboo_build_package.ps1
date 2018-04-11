write-Host "Retrieve and Package artifacts from git"
$current_dir = $(get-location).Path;
write-Host "Working in: $current_dir"
dir
write-Host "Performing Build operations"
write-Host "Whew, that was quick!"
$action = "package_version"
$automation_dir = $Env:bamboo_dbm_automation_dir
$version = $Env:bamboo_dbm_version
$cmd = "$automation_dir\\bamboo_groovy.bat"

& $cmd action=$action
if ($LASTEXITCODE -ne 0) {
  exit(1)
}