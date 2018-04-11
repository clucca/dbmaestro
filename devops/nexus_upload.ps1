# BJB - DBmaestro 2018
# PS to compress a folder and push to Nexus
$version = "$env:VERSION"
$base_path = "C:\Automation\source\n8"
$user = "admin"  
$pwd = ConvertTo-SecureString "Trial123!" -AsPlainText -Force  
$cred = New-Object System.Management.Automation.PSCredential ($user, $pwd)  
$uri = "http://52.224.182.43:7085/repository/dbm_devops/quest_perf/"
$imagePath = "dbm_api_1.0.6.zip"
# Locate the zip from the version







$upload= Invoke-RestMethod -Uri $uri -Method Post -InFile $imagePath -Credential $cred -ContentType 'multipart/form-data'




function ZipFiles( $zipfilename, $sourcedir )
{
   Add-Type -Assembly System.IO.Compression.FileSystem
   $compressionLevel = [System.IO.Compression.CompressionLevel]::Optimal
   [System.IO.Compression.ZipFile]::CreateFromDirectory($sourcedir,
        $zipfilename, $compressionLevel, $false)
}