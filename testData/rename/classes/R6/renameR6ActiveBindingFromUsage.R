Accumulator <- R6Class("Accumulator", list(
  summary = 0,
  add = function(x = 1) {
    self$summary <- self$summary + x
    invisible(self)
  },
  random = function(value) {
    if (missing(value)) {
      runif(1)
    } else {
      stop("Can't set `$random`", call. = FALSE)
    }
  })
)

x <- Accumulator$new()
x$<caret>random