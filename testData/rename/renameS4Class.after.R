setClass('YourClass', slots = c(f = 'numeric'))
setClass('Banana', contains = 'YourClass')

f5 <- new('YourClass', f = 5)
f6 <- new('YourClass', f = 6)
f5@f
