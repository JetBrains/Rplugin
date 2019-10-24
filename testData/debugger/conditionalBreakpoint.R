foo <- function(x) {
  return(x * 2) # BREAKPOINT(condition = x > 10)
}

bar <- function() {
  foo(1) # BREAKPOINT
  foo(11)
  foo(2)
}

bar()