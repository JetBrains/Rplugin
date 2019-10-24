x <- "A"
x <- "BB" # BREAKPOINT(suspend = FALSE, evaluate = (newVar <- paste0("[", x, "]")))
x <- "CCC" # BREAKPOINT(suspend = FALSE, evaluate = (newVar <- "no"), condition = x == "A")