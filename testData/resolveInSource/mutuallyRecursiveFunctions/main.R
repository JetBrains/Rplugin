# This case is not supported. The test is needed to make sure that there are no errors or stack overflow. Behavior is undefined
a <- function(i) {
  if (i != 0) {
    source("A.R")
    b(i - 1)
    fu<caret>n()
  }
}

b <- function(j) {
  if (j != 0) {
    a(j - 1)
    source("A.R")
  }
}
