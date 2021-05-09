setClass('People', slots = c(c<caret>ity = 'character'))
setClass('Employee', contains = 'People')

john <- new('Employee', city = 'LA')
john@city
