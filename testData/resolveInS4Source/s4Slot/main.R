source('B.R')
new('MyClass')

loadA <- function() source('A.R')
loadA()
new('MyClass', da<caret>ta = 5)