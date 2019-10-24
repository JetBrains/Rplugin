function(x){
  if (!is.list(x)){
    stop("")
  }
  if (!all(unlist(lapply(x, function(df){all(df$values == x[[1]]$values)})))){
    stop("")
  }
}