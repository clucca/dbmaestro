import groovy.json.*

// N8 Deployment Pipeline
// Set this variable to choose between Dev1 and Dev2 landscape
def landscape = "adventure"
def live = false // FIXME just for demo
def flavor = 0
sep = "\\"
def base_path = "C:\\Automation\\jenkins_pipe"

rootJobName = "$env.JOB_NAME";
//FIXME branchName = rootJobName.replaceFirst('.*/.*/','')
branchName = "master"
branchVersion = ""
// Outboard Local Settings
def settings_file = "local_settings.json"
def local_settings = [:]
// Settings
def git_message = ""
// message looks like this "Adding new tables [Version: V2.3.4] "
//def reg = ~/.*\[Version: (.*)\].*\[DBCR: (.*)\].*/
def reg = ~/.*\[Version: (.*)\].*/
def environment = ""
def environments = []
def approver = ""
def result = ""
def dbcr_result = ""
def pipeline = ""
def staging_dir = ""
def source_dir = ""
def base_env = "Dev"
def base_schema = ""
def version = "3.11.2.1"
def buildNumber = "$env.BUILD_NUMBER"
local_settings = get_settings("${base_path}${sep}${settings_file}")
def server = local_settings["general"]["server"]

// Add a properties for Platform and Skip_Packaging
properties([
	parameters([
		//choice(name: 'Landscape', description: "Develop/Release to specify deployment target", choices: 'MP_Dev\nMP_Dev2,MP_Release'),
		choice(name: 'Skip_Packaging', description: "Yes/No to skip packaging step", choices: 'No\nYes')
	])
])

def java_cmd = local_settings["general"]["java_cmd"]
def dbmNode = ""
def staging_path = local_settings["general"]["staging_path"]
// note key off of landscape variable
base_schema = local_settings["branch_map"][landscape][flavor]["base_schema"]
base_env = local_settings["branch_map"][landscape][flavor]["base_env"]
pipeline = local_settings["branch_map"][landscape][flavor]["pipeline"]
environments = local_settings["branch_map"][landscape][flavor]["environments"]
def approvers = local_settings["branch_map"][landscape][flavor]["approvers"]
source_dir = local_settings["branch_map"][landscape][flavor]["source_dir"]
local_settings = null

 echo "Working with: ${rootJobName}\n - Branch: ${branchName}\n - Pipe: ${pipeline}\n - Env: ${base_env}\n - Schema: ${base_schema}"
staging_dir = "${staging_path}${sep}${pipeline}${sep}${base_schema}"

/*
#-----------------------------------------------#
#  Stages
*/
stage('GitParams') {
  node (dbmNode) {
    echo '#---------------------- Summary ----------------------#'
    echo "#  Validating Git Commit"
    echo "#------------------------------------------------------#"
    echo "# Update git repo..."
    echo "# Reset local path - original:"
    bat "echo %PATH%"
        echo "# Read latest commit..."
        bat "git --version"
        git_message = bat(
          script: "@cd ${source_dir} && @git log -1 HEAD --pretty=format:%%s",
          returnStdout: true
        ).trim()

    //git_message = "This is git message. [VERSION: 2.5.0]"
    echo "# From Git: ${git_message}"
    result = git_message.replaceFirst(reg, '$1')
	//  dbcr_result = git_message.replaceFirst(reg, '$2')
	//git_message = "[Version: 3.11.2.1] using [DBCR: ADVTA00292]"
	//result = "3.11.2.1"
  }
}

if(! branchVersion.equals("")){
	// Both branch version and git version git wins as override!
	if (git_message.length() != result.length()){
		echo "# VERSION from git:" + result
		echo "# VERSION from branch:" + branchVersion
		echo "# git version overrides branch version!"
	}else{
		echo "# VERSION from branch:" + branchVersion
		result = branchVersion
	}
}else if (git_message.length() == result.length()){
	echo "No VERSION found\nResult: ${result}\nGit: ${git_message}"
	currentBuild.result = "UNSTABLE"
	return
}else{
	echo "# VERSION from git:" + result
}

version = "V" + result // + "__" + dbcr_result
if (dbcr_result == "" && env.Skip_Packaging != "No"){
	version = "V" + result
}

echo message_box("${pipeline} Deployment", "title")
echo "# FINAL PACKAGE VERSION: ${version}"
environment = environments[0]
stage(environment) {
  node (dbmNode) {
    //Copy from source to version folder
  if(!env.Skip_Packaging || env.Skip_Packaging == "No"){
    echo "#------------------- Copying files for ${version} ---------#"
    bat "if exist ${staging_dir} del /q ${staging_dir}\\*"
    // This is for when files are prefixed with <dbcr_result>
    //bat "if not exist \"${staging_dir}${sep}${version}\" mkdir \"${staging_dir}${sep}${version}\""
    //bat "copy \"${source_dir}${sep}${dbcr_result}*.sql\" \"${staging_dir}${sep}${version}\""
    // This is for copying a whole directory
    bat "xcopy /s /y /i \"${source_dir}${sep}${result}\" \"${staging_dir}${sep}${version}\""
    // trigger packaging
    echo "#----------------- Packaging Files for ${version} -------#"
    bat "${java_cmd} -Package -ProjectName ${pipeline} -Server ${server}"
    // version = adhoc_package(version)
  }else{
	  echo "#-------------- Skipping packaging step (parameter set) ---------#"
  }
    // Deploy to Dev
    echo "#------------------- Performing Deploy on ${environment} -------------#"
    bat "${java_cmd} -Upgrade -ProjectName ${pipeline} -EnvName ${environment} -PackageName ${version} -Server ${server}"
  }
}
if(environments.size() < 2) {
	currentBuild.result = "SUCCESS"
	return
}
environment = environments[1]
approver = approvers[0]
def pair = environment.split(",")
def do_pair = false
if (pair.size() == 2) {environment = pair[0] }
do_pair = (pair.size() == 2) 
stage(environment) {
	// FIXME checkpoint('Rerun QA')
	input message: "Deploy to ${environment}?", submitter: approver
	node (dbmNode) {
		//  Deploy to QA
		echo '#------------------- Performing Deploy on ${environment} --------------#'
		bat "${java_cmd} -Upgrade -ProjectName ${pipeline} -EnvName ${pair[0]} -PackageName ${version} -Server ${server}"
		if (do_pair) {
			bat "${java_cmd} -Upgrade -ProjectName ${pipeline} -EnvName ${pair[1]} -PackageName ${version} -Server ${server}"
		}
	}   
} 
if(environments.size() < 3) {
	currentBuild.result = "SUCCESS"
	return
}
environment = environments[2]
approver = approvers[1]
stage(environment) {
	// FIXME checkpoint('Rerun QA')
	input message: "Deploy to ${environment}?", submitter: approver
	node (dbmNode) {
		//  Deploy to QA
		echo '#------------------- Performing Deploy on ${environment} --------------#'
		bat "${java_cmd} -Upgrade -ProjectName ${pipeline} -EnvName ${environment} -PackageName ${version} -Server ${server}"
	}   
} 
if(environments.size() < 4) {
	currentBuild.result = "SUCCESS"
	return
}
environment = environments[3]
approver = approvers[2]
stage(environment) {
	// FIXME checkpoint('Rerun QA')
	input message: "Deploy to ${environment}?", submitter: approver
	node (dbmNode) {
		//  Deploy to QA
		echo '#------------------- Performing Deploy on ${environment} --------------#'
		bat "${java_cmd} -Upgrade -ProjectName ${pipeline} -EnvName ${environment} -PackageName ${version} -Server ${server}"
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
