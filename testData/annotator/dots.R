
<info descr="R_FUNCTION_DECLARATION">simple</info> <- function(<info descr="R_PARAMETER">...</info>) <info descr="R_PARAMETER">...</info> + 1

<info descr="R_FUNCTION_DECLARATION">nested_test</info> <- function (<info descr="R_PARAMETER">...</info>) {
   <info descr="R_FUNCTION_DECLARATION">nested</info> <- function(<info descr="R_PARAMETER">a</info>, <info descr="R_PARAMETER">b</info>) {
       <info descr="R_PARAMETER">a</info> + <info descr="R_PARAMETER">b</info> + <info descr="R_CLOSURE">...</info>
   }
   <info descr="R_FUNCTION_CALL">nested</info>(10, 20)
}

#No real error here
<warning descr="'...' used in an incorrect context"><info descr="R_LOCAL_VARIABLE">...</info></warning> <- 10

#But we can't use it anywhere
<warning descr="'...' used in an incorrect context"><info descr="R_LOCAL_VARIABLE">...</info></warning> + 1

#Such method could be defined but its evaluation will have an error
<info descr="R_FUNCTION_DECLARATION">no_way</info> <- function() <warning descr="'...' used in an incorrect context">...</warning> + 1

#We can define a function
<info descr="R_FUNCTION_DECLARATION">...</info> <- function() 10

#And even use it
<info descr="R_FUNCTION_CALL">...</info>()
#But it is strange inconsistency