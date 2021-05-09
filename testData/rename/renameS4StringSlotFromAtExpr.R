setClass('People', slots = c('city'))
setClass('Employee', contains = 'People')

john <- new('Employee', city = 'LA')
john@ci<caret>ty
