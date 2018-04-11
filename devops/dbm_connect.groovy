/*
 #-------- SQL Oracle with Groovy -------------#
 - Setup: Put the oracle ojdbc6.jar in the same folder as this script
 - Update path in script here for the jenkins groovy jar
 - Here is the invocation on the command line
 java -cp ".;ojdbc6.jar;C:\Program Files (x86)\Jenkins\war\WEB-INF\lib\groovy-all-2.4.7.jar" groovy.ui.GroovyMain c:\automation\source\groovy\db_connect.groovy
*/
// @ExecutionModes({ON_SINGLE_NODE})

import java.sql.Connection
import groovy.sql.Sql
//import oracle.jdbc.pool.OracleDataSource
import groovy.json.*
import java.io.File
import java.text.SimpleDateFormat
def base_path = new File(getClass().protectionDomain.codeSource.location.path).parent
def jsonSlurper = new JsonSlurper()
def json_file = "dbm_queries.json"
def settings_file = "local_settings.json"
arg_map = [:]
file_contents = [:]
contents = [:]
local_settings = [:]
sep = "/" //FIXME Reset for windows
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
}

println "JSON Config Document: ${base_path}${sep}${json_file}"
json_file_obj = new File( base_path, json_file )
if (json_file_obj.exists() ) {
  file_contents = jsonSlurper.parseText(json_file_obj.text)  
}
println "... done"

if (arg_map.containsKey("action")) {
  switch (arg_map["action"].toLowerCase()) {
    case "dbm_package":
      dbm_package
      break
    case "adhoc_package":
      adhocify_package()
      break
    case "disable_package":
      disable_package()
      break
    default:
      perform_query()
      break
  }
}else{
  if (arg_map.containsKey("help")) {
    message_box("dbm_api HELP", "title")
    file_contents.each { k,v -> 
      println "${k}: ${v["name"]}"
      println "\tUsage: ${v["usage"]}"
      println " --------- "
    }
  }else{
    println "Error: specify action=<action> as argument"
    System.exit(1)
    
  }
}

def perform_query() {
  if (!file_contents.containsKey(arg_map["action"])) {
    println "Error: Action: ${arg_map["action"]} - not found!"
    println "Available: ${file_contents.keySet()}"
    System.exit(1)
  }
  contents = file_contents[arg_map["action"]]
  message_box("Task: ${arg_map["action"]}")
  println " Description: ${contents["name"]}"
  for (query in contents["queries"]) {
    def post_results = ""
    separator()
    def conn = sql_connection(query["connection"].toLowerCase())
    //println "Raw Query: ${query["query"]}"
    def query_stg = add_query_arguments(query["query"])
    println "Processed Query: ${query_stg}"
    message_box("Results")
    def header = ""
    query["output"].each{arr ->
      def va = out_vals(arr)
      header += "| ${arr[0].padRight(va[1])}"
      }
    println header
    separator(100)
    conn.eachRow(query_stg)
    { row -> 
      query["output"].each{arr ->
        def va = out_vals(arr)
        def val = row.getAt(va[0])
        print "| ${val.toString().trim().padRight(va[1])}"
      }
      println " "
    }
    separator(100)
    println ""
    if (query.containsKey("post_process")) {
      post_process(query["post_process"], query_stg, conn)
    }
    conn.close()

  }
}

def post_process(option, query_string, connection){
  //println "Option: ${option}"
  def result = ""
  //println "Running post-processing: ${option}"
  switch (option.toLowerCase()) {
    case "export_packages":
      export_packages(query_string, connection)
      break
    case "create_control_json":
      create_control_json(query_string, connection)
      break
    case "show_object_ddl":
      show_object_ddl(query_string, connection)
      break
  }
  return result
}

def sql_connection(conn_type) {
  def user = ""
  def password = ""
  def conn = ""
  if (conn_type == "repo" || conn_type == "repository") {
    user = local_settings["connections"]["repository"]["user"]
    if (local_settings["connections"]["repository"].containsKey("password_enc")) {
      password = password_decrypt(local_settings["connections"]["repository"]["password_enc"])
    }else{
      password = local_settings["connections"]["repository"]["password"]
    }
    conn = local_settings["connections"]["repository"]["connect"]
  }else if (conn_type == "remote") {
    // FIXME find instance for named environment and build it
    user = local_settings["connections"]["remote"]["user"]
    if (local_settings["remote"].containsKey("password_enc")) {
      password = password_decrypt(local_settings["connections"]["remote"]["password_enc"])
    }else{
      password = local_settings["connections"]["remote"]["password"]
    }
    conn = local_settings["connections"]["remote"]["connect"]
  }
  // Assign local settings
  println "Querying ${conn_type} Db: ${conn}"
  return Sql.newInstance("jdbc:oracle:thin:@${conn}", user, password)
}

def export_packages(query_string, conn){
  def jsonSlurper = new JsonSlurper()
  def date = new Date()
  def contents = [:]
  sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
  def src = ""
  def cur_ver = ""
  def source_pipeline = ""
  def target_pipeline = System.getenv("TARGET_PIPELINE")
  if(! target_pipeline){
    println "Error: Set TARGET_PIPELINE environment variable or pass path to control.json as 2nd argument"
    System.exit(1)
  }
  def target_schema = get_target_schema(target_pipeline)
  def export_path_temp = get_export_json_file(target_pipeline, true)
  
  message_box("Exporting Versions")
  println "JSON Export config: ${export_path_temp}"
  contents = get_export_json_file(target_pipeline)
  def target_path = contents["export_path"]
  def base_path = new File(target_path).getParent()
  ensure_dir("${base_path}${sep}hold")
  def tmp_path = target_path
  def fil_name = ""
  def hdr = ""
  // Redo query and loop through records
  conn.eachRow(query_string) 
  { rec -> 
    hdr += "-- Exported from pipeline: ${rec.FLOWNAME} on ${sdf.format(date)}\n"
    hdr += "-- Source: Version - ${rec.version}, created: ${rec.created_at}\n"
    cur_ver = "${rec.version}".toString()
    def result = cur_ver
    tmp_path = "${target_path}${sep}${cur_ver}"
    src = new File(rec.script_sorce_data_reference).text
    if (contents["packages"].containsKey(cur_ver)) {
      if (contents["packages"][cur_ver][0]) {
        if (contents["packages"][cur_ver][1] != ""){
          tmp_path = "${target_path}${sep}${contents["packages"][cur_ver][1]}"
        }
        result += " - GO\n"
      }else{
        result += " - HOLD\n"
        tmp_path = "${base_path}${sep}hold${sep}${cur_ver}"
        hdr += "-- (skipped in primary release)\n"
      }
      ensure_dir(tmp_path)
      fil_name = "${rec.excution_order}_${rec.script}"
      src = hdr + src
      //println src
      println "Exporting Script: ${rec.script}, Target: ${tmp_path}"
      create_file(tmp_path, fil_name, src)
    
    }else{
      result += " - GO (missing)\n"
    }
  }
  println result
}

def create_control_json(query_string, conn){
  def result = [:]
  def seed_list = [:]
  def current_dir = new File(".").getAbsolutePath()
  def p_list = ""
  def target_pipeline = System.getenv("TARGET_PIPELINE")
  def source_pipeline = System.getenv("SOURCE_PIPELINE")
  def target_schema = ""
  if ( System.getenv("EXPORT_PACKAGES") != null ) { p_list = System.getenv("EXPORT_PACKAGES") }
  if ( (arg_map["ARG1"] != null) ) { 
    source_pipeline = arg_map["ARG1"]
  }else if(source_pipeline == null){
    println "Source Pipeline must be in ARG1"
    return true
  }
  def export_path_temp = get_export_json_file(target_pipeline, true)
  def ex_path = new File(export_path_temp).getParent()
  if ( ex_path != null ) { 
    println "Destination: ${export_path_temp}"
  } 
  
  if( p_list && p_list != "" ){
    p_list.split(",").each{ 
      if (it.contains("_REMAP_")) {
        def parts = it.split("_REMAP_")
        seed_list[parts[0].trim()] = parts[1].trim()
      }else{
        seed_list[it.trim()] = ""
      }
    }
    println "Packages to Transfer"
    println seed_list
  } 
  
  // Get target packages for comparison
  def target_versions = []
  conn.eachRow(query_string.replaceAll(source_pipeline, target_pipeline)){ rec -> 
    target_versions.add("${rec.version}".toString())
  }
  
  result["packages"] = [:]
  conn.eachRow(query_string){ rec -> 
    def ver = "${rec.version}".toString().trim()
    if (! target_versions.contains(ver)) {
      if (seed_list.size() > 0 ) {
        if (seed_list.keySet().contains(ver)){
          result["packages"][ver] = [true, seed_list[ver]]
        }else{
          result["packages"][ver] = [false, ""]
        }
      }else{
        result["packages"][ver] = [true, ""]
      }
    }
  }
  println "JSON control file: ${export_path_temp}"
  val_file = new File(export_path_temp)
  val_file.withWriter('utf-8') { writer -> 
    writer << JsonOutput.prettyPrint(JsonOutput.toJson(result))
  } 
}

def dbm_package() {
  def java_cmd = local_settings["general"]["java_cmd"]
  def server = local_settings["general"]["server"]
  def target_pipeline = System.getenv("TARGET_PIPELINE")
  def base_path = local_settings["general"]["staging_path"]
  def base_schema = get_target_schema(target_pipeline)
  println "#-------- Performing DBmPackage command ----------#"
  println "# Cmd: ${java_cmd} -Package -ProjectName ${target_pipeline} -Server ${server}"
  def results = "${java_cmd} -Package -ProjectName ${target_pipeline} -Server ${server} ".execute().text
}

def adhocify_package() {
  def package_name = arg_map["ARG1"]
  separator()
  def parts = package_name.split("__")
  def new_name = parts.length == 2 ? parts[1] : package_name
  def query = "update twmanagedb.TBL_SMG_VERSION set NAME = 'ARG_NAME', UNIQ_NAME = 'ARG_NAME', TYPE_ID = 2 where NAME = 'ARG_FULL_NAME'"
  def conn = sql_connection("repository")
  //println "Raw Query: ${query["query"]}"
  def query_stg = query.replaceAll("ARG_FULL_NAME", package_name)
  query_stg = query_stg.replaceAll("ARG_NAME", new_name)
  println "Processed Query: ${query_stg}"
  message_box("Results")
  def res = conn.execute(query_stg) 
  println res
  separator()
  conn.close()
}

def disable_package() {
  def package_name = arg_map["ARG1"]
  separator()
  def query = "update twmanagedb.TBL_SMG_VERSION set IS_ENABLED = 0 where NAME = 'ARG_FULL_NAME'"
  def conn = sql_connection("repository")
  //println "Raw Query: ${query["query"]}"
  def query_stg = query.replaceAll("ARG_FULL_NAME", package_name)
  println "Processed Query: ${query_stg}"
  message_box("Results")
  def res = conn.execute(query_stg) 
  println res
  separator()
  conn.close()
}

def show_object_ddl(query_string, conn) {
  // Redo query and loop through records
  conn.eachRow(query_string) 
  { rec -> 
    message_box("Object DDL Rev: ${rec.COUNTEDREVISION} of ${rec.OBJECT_NAME}")
    java.sql.Clob clob = (java.sql.Clob) rec.OBJECTCREATIONSCRIPT
    bodyText = clob.getAsciiStream().getText()
    println bodyText
  }
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

def get_export_json_file(target, path_only = false){
  def contents = [:]  
  def export_path_temp = "${local_settings["general"]["staging_path"]}${sep}${target}${sep}export_control.json"
  println "JSON Export config: ${export_path_temp}"
  if(path_only){
    return export_path_temp
  }
  def json_file_obj = new File( export_path_temp )
  if (json_file_obj.exists() ) {
    contents = jsonSlurper.parseText(json_file_obj.text)  
  }
  return contents
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
  println res
  return res
}

def separator( def ilength = 82){
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

def read_file(pth, name){
  def fil = new File(pth,name)
  return fil.text
}
