new_source <- function() {
  source("resolveInSource/qualifiedSourceCall/A.R")
}

new_source()
base::source("resolveInSource/qualifiedSourceCall/B.R")
fu<caret>n()
