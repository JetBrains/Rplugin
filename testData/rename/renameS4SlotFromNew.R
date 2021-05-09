setClass('People', slots = c(city = 'character'))
setClass('Employee', contains = 'People')

john <- new('Employee', ci<caret>ty = 'LA')
john@city
