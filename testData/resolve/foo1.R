foo.bar <- function() { print(1) }
baz.bar <- function () { print(2) }

local <- function() {
  baz.bar() # here
}