ncol <- function(...) {
    base::ncol(...)
}

# overridden ncol
1:ncol(a)

# base ncol
<warning descr="The sequence may not behave as expected when the right-hand side is non-positive">1:base::ncol(a)</warning>
