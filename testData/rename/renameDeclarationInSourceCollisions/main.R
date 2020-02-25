foo <- function(x, y) {
  x <- 10
  y <- x + x
}

source("rename/renameDeclarationInSourceCollisions/A.R")
fo<caret>o()

