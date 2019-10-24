foo <- function() {
  x <- 1 # BREAKPOINT
  x <- 2
  x <- 3
}

foo()
foo()
foo()