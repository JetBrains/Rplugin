setClass('People', slots = c('address'))
setClass('Employee', contains = 'People')

john <- new('Employee', address = 'LA')
john@address
