Accumulator <- R6Class("Accumulator", list(
  sum = 0,
  add = function(x = 1) {
    self$sum <- self$sum + x
    invisible(self)
  })
)

x <- Accumulator$new()
x$sum$add(4)$sum$add(4)$<caret>sum
x$sum
