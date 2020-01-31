# This case is not supported. The test is needed to make sure that there are no errors or stack overflow. Behavior is undefined
a <- function(i) {
  if (i != 0) {
    source("resolveInSource/recursiveFunction/A.R")
    a()
    fu<caret>n(i - 1)
  }
  else {
    source("resolveInSource/recursiveFunction/A.R")
  }
}
