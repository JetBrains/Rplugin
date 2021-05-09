setClass('People', slots = c('c<caret>ity'))
setClass('Employee', contains = 'People')

john <- new('Employee', city = 'LA')
john@city
