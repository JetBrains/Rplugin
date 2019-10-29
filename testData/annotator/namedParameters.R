<info descr="R_FUNCTION_DECLARATION">my_func</info> <- function(<info descr="R_PARAMETER">x</info>, <info descr="R_PARAMETER">y</info> = 2, <info descr="R_PARAMETER">z</info> = 3) {
  <info descr="R_FUNCTION_DECLARATION">another_func</info> <- function(<info descr="R_CLOSURE"><info descr="R_PARAMETER">x</info></info>, <info descr="R_CLOSURE"><info descr="R_PARAMETER">y</info></info>) {
      <info descr="R_FUNCTION_CALL">my_func</info>(12321, <info descr="R_NAMED_ARGUMENT">z</info> = 3124)
      <info descr="R_FUNCTION_CALL">print</info>(<info descr="R_CLOSURE">z</info>)
  }
}



