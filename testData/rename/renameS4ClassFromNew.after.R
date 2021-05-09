setClass('Fruit', slots = c(f = 'numeric'))
setClass('Banana', contains = 'Fruit')

f5 <- new('Fruit', f = 5)
f6 <- new('Fruit', f = 6)
f5@f
