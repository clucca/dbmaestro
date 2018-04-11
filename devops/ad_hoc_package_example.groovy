import groovy.json.*
/*
##  Example of how to insert the adhoc command into an existing groovy pipeline script
##  The system should have a working setup of dbm_api (which also needs an update for ad_hoc)
*/

// N8 Deployment Pipeline
// Set this variable to choose between Dev1 and Dev2 landscape
def landscape = "hrm"
def live = false // FIXME just for demo
def flavor = 0
sep = "\\"
def base_path = "C:\\Automation\\jenkins_pipe"

rootJobName = "$env.JOB_NAME";

// Add a properties for Platform and Skip_Packaging
properties([
	parameters([
		//choice(name: 'Landscape', description: "Develop/Release to specify deployment target", choices: 'MP_Dev\nMP_Dev2,MP_Release'),
		string(name: 'PackageName', description: "Name for AD-Hoc package, not must have the __ separating version from name", defaultValue: 'V9.0.0__YourPackageName')
	])
])

// Outboard Local Settings
def settings_file = "local_settings_hrm.json"
def local_settings = [:]
def pipeline = ""
def buildNumber = "$env.BUILD_NUMBER"
local_settings = get_settings("${base_path}${sep}${settings_file}")

def server = local_settings["general"]["server"]
def java_cmd = local_settings["general"]["java_cmd"]
def dbmNode = ""
def staging_path = local_settings["general"]["staging_path"]
// note key off of landscape variable
pipeline = local_settings["branch_map"][landscape][flavor]["pipeline"]
source_dir = local_settings["branch_map"][landscape][flavor]["source_dir"]
local_settings = null // must null to avoid serialization error

 echo "Working with: ${rootJobName}\n - Branch: ${branchName}\n - Pipe: ${pipeline}\n - Env: ${base_env}\n - Schema: ${base_schema}"
staging_dir = "${staging_path}${sep}${pipeline}${sep}${base_schema}"
def full_package_name = env.PackageName
def parts = full_package_name.split("__")
def package_name = parts.length == 2 ? parts[1] : full_package_name
version = parts[0]
/*
#-----------------------------------------------#
#  Stages
*/

//echo message_box("${env.Database_Platform} Deployment", "title")
echo message_box("${pipeline} Package Build", "title")
environment = environments[0]
stage(environment) {
  node (dbmNode) {
    //Copy from source to version folder
    echo "#------------------- Copying files for ${full_package_name} ---------#"
    bat "if exist ${staging_dir} del /q ${staging_dir}\\*"
    bat "if not exist \"${staging_dir}${sep}${version}\" mkdir \"${staging_dir}${sep}${full_package_name}\""
    bat "copy \"${source_dir}${sep}${version}*.sql\" \"${staging_dir}${sep}${full_package_name}\""
  // trigger packaging
    echo "#----------------- Packaging Files for ${version} -------#"
    bat "${java_cmd} -Package -ProjectName ${pipeline} -Server ${server}"
    echo "#----------------- Converting to AD-Hoc ${package_name} -------#"
    adhoc_package(full_package_name)
  }
}


def adhoc_package(full_package_name){
	echo "Converting to AD-HOC package"
	def parts = full_package_name.split("__")
	def package_name = parts.length == 2 ? parts[1] : full_package_name
	def version = parts[0]
	def dbm_cmd = "cd C:\\Automation\\dbm_demo\\devops\r\ndbm_api.bat action=adhoc_package"
	bat "${dbm_cmd} ARG1=${full_package_name}"
	return package_name
}

@NonCPS
def ensure_dir(pth){
  folder = new File(pth)
  if ( !folder.exists() ){
    if( folder.mkdirs()){
        return true
    }else{
        return false
    }
  }else{
    return true
  }
}

def get_settings(file_path) {
	def jsonSlurper = new JsonSlurper()
	def settings = [:]
	println "JSON Settings Document: ${file_path}"
	def json_file_obj = new File( file_path )
	if (json_file_obj.exists() ) {
	  settings = jsonSlurper.parseText(json_file_obj.text)  
	}
	return settings
}

def message_box(msg, def mtype = "sep") {
  def tot = 80
  def start = ""
  def res = ""
  msg = (msg.size() > 65) ? msg[0..64] : msg
  def ilen = tot - msg.size()
  if (mtype == "sep"){
    start = "#${"-" * (ilen/2).toInteger()} ${msg} "
    res = "${start}${"-" * (tot - start.size() + 1)}#"
  }else{
    res = "#${"-" * tot}#\n"
    start = "#${" " * (ilen/2).toInteger()} ${msg} "
    res += "${start}${" " * (tot - start.size() + 1)}#\n"
    res += "#${"-" * tot}#\n"   
  }
  //println res
  return res
}

def separator( def ilength = 82){
  def dashy = "-" * (ilength - 2)
  //println "#${dashy}#"
}
