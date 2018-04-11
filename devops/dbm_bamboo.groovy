/*
 #-------- Bamboo routines with Groovy -------------#
 - Setup: 
 - 1/10/18 BJB

*/
// @ExecutionModes({ON_SINGLE_NODE})
import groovy.util.AntBuilder ;
import groovy.json.*
import java.io.File
import java.text.SimpleDateFormat
def base_path = new File(getClass().protectionDomain.codeSource.location.path).parent
def jsonSlurper = new JsonSlurper()
def settings_file = "local_settings.json"
def pipeline = "HumanResources"
app_name = "hr_demo"
arg_map = [:]
file_contents = [:]
contents = [:]
local_settings = [:]
java_cmd = "java -jar \"C:\\Program Files (x86)\\DBmaestro\\TeamWork\\TeamWorkOracleServer\\Automation\\DBmaestroAgent.jar\""
automation_dir = "C:\\Automation\\dbm_demo\\devops"
curl_cmd = "${automation_dir}\\lib\\curl.exe"
sep = "\\" //FIXME Reset for windows
version_file = "${automation_dir}\\${pipeline}_version.txt"
package_version_path = ""
for (arg in this.args) {
  //println arg
  pair = arg.split("=")
  if(pair.size() == 2) {
    arg_map[pair[0].trim()] = pair[1].trim()
  }else{
    arg_map[arg] = ""
  }
}
separator()
println "loading..."
println "JSON Settings Document: ${base_path}${sep}${settings_file}"
def json_file_obj = new File( base_path, settings_file )
if (json_file_obj.exists() ) {
  local_settings = jsonSlurper.parseText(json_file_obj.text)  
}else{
	println "local_settings.json file not found"
}

if (arg_map.containsKey("action")) {
  switch (arg_map["action"].toLowerCase()) {
    case "package_version":
      package_version_folder()
      break
    case "upload_artifact":
      upload_artifact()
      break
    default:
      println "ERROR: no action found"
	  System.exit(1)
      break
  }
}

def package_version_folder(){
	def git_info = System.getenv("bamboo_repository_git_branch")
	def pipeline = System.getenv("bamboo_dbm_pipeline")
  def build_info = System.getenv("bamboo_buildPlanName")
  def dbm_base_path = System.getenv("bamboo_dbm_base_path")
  def dbm_base_schema = System.getenv("bamboo_dbm_base_schema")
	def version = System.getenv("bamboo_dbm_version")
	def source_dir = System.getenv("bamboo_working_directory")
  def git_message = ""
  // message looks like this "Adding new tables [Version: V2.3.4] "
  message_box("Packaging Version from Bamboo")
  println "# Inputs:"
  println "#   bamboo_dbm_pipeline: ${pipeline}"
  println "#   bamboo_buildPlanName: ${build_info}"
  println "#   bamboo_repository_git_branch: ${git_info}"
  println "#   bamboo_working_directory: ${source_dir}"
	version = get_git_version(source_dir)
  if(version == "NONE") {
    println "ERROR - no version specified in git message"
    System.exit(1)
  }
  set_version_properties(version)
  def zip_file = package_path_from_version(version, source_dir)
  create_zip_file(zip_file, "${source_dir}${sep}${app_name}${sep}versions${sep}${version}")
  dbm_package_and_deploy(zip_file)
}

def upload_artifact() {
	def pipeline = System.getenv("bamboo_dbm_pipeline")
	def source_dir = System.getenv("bamboo_working_directory")
  def cmd = ""
  def artifact_server = [
    "url" : "http://pocintegration:8081",
    "platform" : "artifactory",
    "project" : "artifactory/dbm_devops/",
    "user" : "admin",
    "pwd" : "Trial123!"]
  message_box("Uploading Build Version to ${artifact_server["platform"]}")
  println "# Inputs:"
  println "#   bamboo_dbm_pipeline: ${pipeline}"
  println "#   bamboo_working_directory: ${source_dir}"
	def version = get_git_version(source_dir)
  if(version == "NONE") {
    println "ERROR - no version specified in git message"
    System.exit(1)
  }
  def zip_file = package_path_from_version(version, source_dir)
  if(artifact_server["platform"] == "nexus"){
    cmd = "${curl_cmd} -v --user \"${artifact_server["user"]}:${artifact_server["pwd"]}\" --upload-file ${zip_file} ${artifact_server["url"]}/${artifact_server["project"]}/${pipeline}/"
  }else{
    cmd = "${curl_cmd} -v --user \"${artifact_server["user"]}:${artifact_server["pwd"]}\" --upload-file ${zip_file} ${artifact_server["url"]}/${artifact_server["project"]}/${pipeline}/"
  }
  def result = "cmd /c ${cmd}".execute().text
	println "git OUTPUT\r\n${result}"
}

def dbm_package_and_deploy(file_path){
  // Package for the CI deploy
	def pipeline = System.getenv("bamboo_dbm_ci_pipeline")
  def dbm_base_schema = System.getenv("bamboo_dbm_ci_base_schema")
  def script_file = "${automation_dir}\\copy_remote_file.ps1"
  def ps_cmd = "cd ${automation_dir} && powershell.exe -executionpolicy bypass -file $script_file $file_path $dbm_base_schema"
  message_box("Deploying to CI Pipeline")
  println "Executing: ${ps_cmd}"
  def outtxt = "cmd /c ${ps_cmd} 2>&1".execute().text
  println "Powershell Output:\r\n${outtxt}"
}

def scp_file_xfer(file_path, target_path){
  // Uses scp installed on pocintegration
  def scp = "C:\\Automation\\OpenSSH-Win64\\scp.exe"
  
}
// #--------- UTILITY ROUTINES ------------#
def out_vals(val_obj){
  def val = ""
  def siz = 0
  if(val_obj instanceof List) {
    val = val_obj[0]
    siz = val_obj[1]
  }else{
    val = val_obj
    siz = 15
  }
  return [val,siz]
}

def get_target_schema(cur_pipeline){
  def target_schema = ""
  local_settings["branch_map"].each { k,v ->
    def cur_branch = k
    v.each { pipe -> if (pipe["pipeline"] == cur_pipeline){ target_schema = pipe["base_schema"] } }
  }
  return target_schema
}

def add_query_arguments(query){
  def result_stg = query
  if (query.contains("ARG1")){
    if (arg_map.containsKey("ARG1")){
      (0..10).each { 
        def cur_key = "ARG${it}".toString()
        if(arg_map.containsKey(cur_key)){
          //println "Find: ${cur_key} => ${arg_map[cur_key]}"
          result_stg = result_stg.replaceAll(cur_key, arg_map[cur_key])
        }else{
          //println "Find: ${cur_key} => %"
          result_stg = result_stg.replaceAll(cur_key, '%')
        }
      }
    }else{
        println "ERROR - query requires ARG values"
        System.exit(1)
    }
  }
  return result_stg
}

def map_has_key(find_map, match_regex){
  def result = "false"
  for (skey in find_map.ketSet()) {
    if (skey.matches(match_key)) {
      result = skey
    }
  }
  return result
}

def message_box(msg, def mtype = "sep") {
  def tot = 72
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
  println res
  return res
}

def separator( def ilength = 72){
  def dashy = "-" * (ilength - 2)
  println "#${dashy}#"
}

def sql_file_list(dir_txt) {
  // Returns files in ascending date order
  def files=[]
  def src = new File(dir_txt)
  src.eachFile groovy.io.FileType.FILES, { file ->
    if (file.name.contains(".sql")) {
      files << file
    }
  }
  return files.sort{ a,b -> a.lastModified() <=> b.lastModified() }
}

def get_git_version(repo_dir) {
  def git_reg = ~/.*\[Version: (.*)\].*/
  def git_cmd = "@cd ${repo_dir} && @git log -1 HEAD --pretty=format:%s"
  println "Executing: ${git_cmd}"
  def version = "NONE"
  def git_message = "cmd /c ${git_cmd}".execute().text
	println "git OUTPUT\r\n${git_message}"
  def result = git_message.replaceFirst(git_reg, '$1')
	// Both branch version and git version git wins as override!
	if (git_message.length() != result.length()){
		println "# VERSION from git:" + result
		version = result
	}
  return version
}

def set_version_properties(version){
  fil = new File(version_file)
  fil.withWriter('utf-8') { writer -> 
    writer << "version=V${version}"
  } 
}

def package_path_from_version(version, path){
  def zip_file = "V${version}.zip"
  package_version_path = "${path}${sep}package${sep}${zip_file}"  
  return(package_version_path)
}

def path_from_pipeline(pipe_name){
  def query_stg = "select f.FLOWID, f.FLOWNAME, s.SCRIPTOUTPUTFOLDER from TWMANAGEDB.TBL_FLOW f INNER JOIN TWMANAGEDB.TBL_FLOW_SETTINGS s ON f.FLOWID = s.FLOWID WHERE f.FLOWNAME = 'ARG1'"
  def result = ""
  sql.eachRow(query_stg.replaceAll("ARG1", pipe_name))
  { row -> 
    result = row.SCRIPTOUTPUTFOLDER
  }
  return result
}

def password_decrypt(password_enc){
  def slug = "__sj8kl3LM77g903ugbn_KG="
  def result = ""
  byte[] decoded = password_enc.decodeBase64()
  def res = new String(decoded)
  res = res.replaceAll(slug,"")
  result = new String(res.decodeBase64())
  return result
}

def ensure_dir(pth) {
  folder = new File(pth)
  if ( !folder.exists() ) { 
  println "Creating folder: ${pth}"
  folder.mkdirs() }
  return pth
}

def dir_exists(pth) {
  folder = new File(pth)
  return folder.exists()
}

def create_file(pth, name, content){
  def fil = new File(pth,name)
  fil.withWriter('utf-8') { writer -> 
      writer << content
  }
  return "${pth}${sep}${name}"
}

def create_zip_file(name, dir_path){
	//tmp_dir = new File(dir_path).getParent()
	def ant = new AntBuilder()
  println "Creating zip: ${name}"
	ant.zip(destfile: name, basedir: dir_path)
}

def read_file(pth, name){
  def fil = new File(pth,name)
  return fil.text
}
