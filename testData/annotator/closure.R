<info descr="R_FUNCTION_DECLARATION">xxx</info> <- function(<info descr="R_PARAMETER">y</info>) {
  <info descr="R_FUNCTION_DECLARATION">zzz</info> <- function(<info descr="R_PARAMETER">z</info>) {
    <info descr="R_FUNCTION_CALL">print</info>(<info descr="R_CLOSURE">y</info> + <info descr="R_PARAMETER">z</info>)
    <info descr="R_CLOSURE">y</info> <<- 321321
    <info descr="R_FUNCTION_CALL">print</info>(<info descr="R_CLOSURE">y</info> + <info descr="R_PARAMETER">z</info>)
    <info descr="R_LOCAL_VARIABLE">y</info> <- 312321
    <info descr="R_FUNCTION_CALL">print</info>(<info descr="R_LOCAL_VARIABLE">y</info> + <info descr="R_PARAMETER">z</info>)
  }
}