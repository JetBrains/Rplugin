setClass('People', slots = c(address = 'character'))
setClass('Employee', contains = 'People')

john <- new('Employee', address = 'LA')
john@address
