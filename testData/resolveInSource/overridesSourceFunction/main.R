new_source <- function() {
  source("resolveInSource/overridesSourceFunction/A.R")
}

new_source()
source("resolveInSource/overridesSourceFunction/B.R")
fu<caret>n()
