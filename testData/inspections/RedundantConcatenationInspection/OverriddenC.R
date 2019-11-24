c <- function(...) {
    base::c(...)
}

# overridden c
c()
c('a')

# base paste
<weak_warning descr="'c' call is redundant">base::c()</weak_warning>
<weak_warning descr="'c' call is redundant">base::c('a')</weak_warning>