paste <- function(a, b, sep = ' ') {
    base::paste(b, a, sep = sep)
}

# overridden paste
paste('a', 'b', sep = '')

# base paste
<weak_warning descr="'paste' with empty separator can be replaced with 'paste0'">base::paste('a', 'b', sep = '')</weak_warning>