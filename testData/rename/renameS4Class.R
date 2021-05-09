setClass('My<caret>Class', slots = c(f = 'numeric'))
setClass('Banana', contains = 'MyClass')

f5 <- new('MyClass', f = 5)
f6 <- new('MyClass', f = 6)
f5@f
