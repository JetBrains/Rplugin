# Tooltip for file argument
<info descr="R_FUNCTION_CALL">source</info>("<info descr="Open file in editor (Ctrl+Click, Ctrl+B)">R/A.R</info>")
<info descr="R_FUNCTION_CALL">source</info>("<info descr="Open file in editor (Ctrl+Click, Ctrl+B)">/home/user/R/B.R</info>")

# Tooltip for named file argument
<info descr="R_FUNCTION_CALL">source</info>(<info descr="R_NAMED_ARGUMENT">file</info> = "<info descr="Open file in editor (Ctrl+Click, Ctrl+B)">R/A.R</info>")

# No tooltip for another arguments & string literals & empty string
<info descr="R_FUNCTION_CALL">source</info>(<info descr="R_NAMED_ARGUMENT">deparseCtrl</info> = "R/A.R")
<info descr="R_FUNCTION_CALL">source</info>(smth, "R/A.R")
<info descr="R_FUNCTION_CALL">source</info>(<info descr="R_FUNCTION_CALL">foo</info>("R/A.R"))
"R/C.R"
<info descr="R_FUNCTION_CALL">source</info>("")

# Tooltip for links
<info descr="R_FUNCTION_CALL">source</info>("<info descr="Open in browser (Ctrl+Click, Ctrl+B)">https://mysite.com/someRFile.R</info>")
"<info descr="Open in browser (Ctrl+Click, Ctrl+B)">https://mysite.com/someRFile.R</info>"
