setClass('A')
setClass('B', contains = 'A')
setClass('C', contains = 'B')

setGeneric('fo<caret>o', function(a1, a2) standardGeneric('foo'))
setMethod('foo', c('A', 'A'), function(a1, a2) { })
setMethod('foo', c('B', 'C'), function(a1, a2) { })

foo(new('B'), new('B'))
foo(new('C'), new('B'))
foo(new('B'), new('C'))
foo(new('C'), new('C'))
