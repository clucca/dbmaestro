def source_dir = "C:\\Automation\\dbm_demo\\devops"
def git_message = ""
def script_file = "${source_dir}\\bamboo_deploy.ps1"
java_cmd = "java -jar \"C:\\Program Files (x86)\\DBmaestro\\TeamWork\\TeamWorkOracleServer\\Automation\\DBmaestroAgent.jar\""
def ps_cmd = "cd ${source_dir} && powershell.exe -noprofile -executionpolicy bypass -file $script_file"
cmds = ["set bamboo_dbm_username=dbmguest",
"set bamboo_dbm_java_cmd=$java_cmd",
"set bamboo_dbm_password=Trial123!",
"set",
ps_cmd
]
println "Executing: ${cmds.join(" && ")}"
git_message = "cmd /c ${cmds.join(" && ")} 2>&1".execute().text
println "OUTPUT\r\n${git_message}"
