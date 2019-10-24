foo <- function(x, y) {
  print(x)
  print(y) # BREAKPOINT
  return(x * y)
}

bar <- function(a, b, c) {
  x = 0
  x = x + a
  x = x + b
  x = x + c
  return(x)
}

baz <- function() {
  bar(10, foo(15, 16), 1 + 2)
}

baz()
