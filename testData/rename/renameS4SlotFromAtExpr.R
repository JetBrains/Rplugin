setClass('People', slots = c(city = 'character'))
setClass('Employee', contains = 'People')

john <- new('Employee', city = 'LA')
john@ci<caret>ty
