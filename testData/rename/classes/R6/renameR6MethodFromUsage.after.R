Accumulator <- R6Class("Accumulator", list(
  sum = 0,
  additiveOperator = function(x = 1) {
    self$sum <- self$sum + x
    invisible(self)
  })
)

x <- Accumulator$new()
x$additiveOperator(4)$sum
x$sum
