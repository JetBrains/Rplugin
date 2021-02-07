Accumulator <- R6Class("Accumulator", list(
  summary = 0,
  add = function(x = 1) {
    self$summary <- self$summary + x
    invisible(self)
  })
)

x <- Accumulator$new()
x$summary$add(4)$summary$add(4)$summary
x$summary
