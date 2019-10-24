<info descr="R_FUNCTION_DECLARATION">my_func</info> <- function(<info descr="R_PARAMETER">x</info>, <info descr="R_PARAMETER">y</info>, <info descr="R_PARAMETER">z</info>) {
  <info descr="R_PARAMETER">x</info> + <info descr="R_PARAMETER">y</info> + <info descr="R_PARAMETER">z</info>
}

<info descr="R_FUNCTION_CALL">bar</info>(<info descr="R_LOCAL_VARIABLE">my_func</info>)

<info descr="R_FUNCTION_CALL">my_func</info>(1, 2, 3)
<info descr="R_FUNCTION_CALL">my_func</info>(2, 4, 5)
