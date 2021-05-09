setClass('MyClass', slots = c(f = 'numeric'))
setClass('Banana', contains = 'MyClas<caret>s')

f5 <- new('MyClass', f = 5)
f6 <- new('MyClass', f = 6)
f5@f
