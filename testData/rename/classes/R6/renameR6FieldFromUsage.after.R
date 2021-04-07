Accumulator <- R6Class("Accumulator", list(
  summary = 0,
  add = function(x = 1) {
    self$summary <- self$summary + x
    invisible(self)
  })
)

x <- Accumulator$new()
x$add(4)$<caret>summary
x$summary
