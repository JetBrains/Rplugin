setClass('A')
setClass('B', contains = 'A')
setClass('C', contains = 'B')

setGeneric('bar', function(a1, a2) standardGeneric('bar'))
setMethod('bar', c('A', 'A'), function(a1, a2) { })
setMethod('bar', c('B', 'C'), function(a1, a2) { })

bar(new('B'), new('B'))
bar(new('C'), new('B'))
bar(new('B'), new('C'))
bar(new('C'), new('C'))
