# Tooltip for file argument
<info descr="R_FUNCTION_CALL">source</info>("<info descr="Open the file in editor (ACTION_GOTO_DECLARATION)">R/A.R</info>")
<info descr="R_FUNCTION_CALL">source</info>("<info descr="Open the file in editor (ACTION_GOTO_DECLARATION)">/home/user/R/B.R</info>")

# Tooltip for named file argument
<info descr="R_FUNCTION_CALL">source</info>(<info descr="R_NAMED_ARGUMENT">file</info> = "<info descr="Open the file in editor (ACTION_GOTO_DECLARATION)">R/A.R</info>")

# No tooltip for another arguments & string literals & empty string
<info descr="R_FUNCTION_CALL">source</info>(<info descr="R_NAMED_ARGUMENT">deparseCtrl</info> = "R/A.R")
<info descr="R_FUNCTION_CALL">source</info>(smth, "R/A.R")
<info descr="R_FUNCTION_CALL">source</info>(<info descr="R_FUNCTION_CALL">foo</info>("R/A.R"))
"R/C.R"
<info descr="R_FUNCTION_CALL">source</info>("")

# Tooltip for links
<info descr="R_FUNCTION_CALL">source</info>("<info descr="Open in browser (ACTION_GOTO_DECLARATION)">https://mysite.com/someRFile.R</info>")
"<info descr="Open in browser (ACTION_GOTO_DECLARATION)">https://mysite.com/someRFile.R</info>"
