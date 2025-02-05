findExtraNamedArgsForPackage <- function(name, depth) {
  spentTime <- NULL
  env <- as.environment(paste0("package:", name))
  funNames <- names(env)
  funCnt <- 0
  res <- Reduce(c, lapply(funNames, function(x) {
    if (!.jetbrains$safePredicateAny(.jetbrains$safeFormalArgs(list(x)), .jetbrains$safeEq, x = "...")) return() # Ignore fun without ...
    funCnt <<- funCnt + 1
    for (t in 1:3) {
      start.time <- Sys.time()
      ress <- .jetbrains$findExtraNamedArgs(x, depth)
      end.time <- Sys.time()
      timee <- end.time - start.time
      # Sometimes there are freezes that are not related to the performance of the code, but bad influence on the final result
      if (as.numeric(timee) < 0.25) break
    }
    if (as.numeric(timee) > 0.25) {
      write(file = "time.txt", paste(name, ":", x, "; Time:", timee, "; Depth:", depth), append = T) # Log of slow functions
    }
    spentTime <<- c(spentTime, as.numeric(timee))
    ress
  }))

  med <- median(spentTime)
  max <- tryCatch(max(spentTime), warning = function(w) { })
  if (length(res) == 0) {
    assign("data", dplyr::add_row(data, package = name, depth = depth, medianTime = 0, maxTime = 0, funCnt = 0, argCnt = 0), envir = .GlobalEnv)
  }
  else {
    assign("data", dplyr::add_row(data, package = name, depth = depth, medianTime = med, maxTime = max, funCnt = funCnt, argCnt = length(res)), envir = .GlobalEnv)
  }
  unique(res)
}

# Don't pay attention to warnings
detachAllPackages <- function() {
  tryCatch(lapply(names(sessionInfo()$loadedOnly), function(x) { suppressMessages(suppressWarnings(library(x, character.only = TRUE))) }), error = function(e) { })
  while (length(sessionInfo()$otherPkgs) > 0) {
    tryCatch(lapply(paste0('package:', names(sessionInfo()$otherPkgs)), detach, character.only = TRUE, unload = TRUE, force = TRUE), warning = function(w) { })
  }
  while (length(sessionInfo()$loadedOnly) > 0) {
    lapply(names(sessionInfo()$loadedOnly), function(x) {
      tryCatch(unloadNamespace(x), error = function(e) { })
    })
  }
}

# lavaan, circlize, psych, geoR examples of heavy packages for analysis
# However e.g. utils::update.packages not very fast either
allPackages <- sort(.packages(T))
data <- dplyr::tibble(package = character(), depth = numeric(), medianTime = numeric(), maxTime = numeric(), funCnt = numeric(), argCnt = numeric())
progress <- 0
maxDepth <- 2
detachAllPackages()
allResults <- lapply(allPackages, function(x) {
  res <- NULL
  tryCatch({
             print(x)
             suppressMessages(suppressWarnings(library(x, character.only = TRUE)))
             for (i in 1:maxDepth) {
               res <- findExtraNamedArgsForPackage(x, i)
             }
             detachAllPackages()
           }, error = function(e) { print(e) })
  print(progress)
  progress <<- progress + 1
  write.csv(data, "data.csv")
  res
})

analise <- function(file) {
  library(dplyr)
  data <- read.csv(file)
  maxDepth <- max(data[['depth']])
  allFinded <- data %>%
    group_by(depth) %>%
    mutate(argsSum = sum(argCnt), worstCase = max(maxTime), generalMedian = max(medianTime))
  all <- (allFinded %>% filter(depth == maxDepth))[['argsSum']][[1]]
  for (i in 1:maxDepth) {
    cur <- (allFinded %>% filter(depth == i))[['argsSum']][[1]]
    cat(paste0("Depth ", i, ": ", round(cur * 100 / all, digits = 2), "% args\n"))
  }

  cat("\n")
  for (i in 1:maxDepth) {
    worstCase <- (allFinded %>% filter(depth == i))[['worstCase']][[1]]
    cat(paste0("Depth ", i, ": ", round(worstCase * 1000, digits = 0), "ms - worst case\n"))
  }

  cat("\n")
  for (i in 1:maxDepth) {
    median <- (allFinded %>% filter(depth == i))[['generalMedian']][[1]]
    cat(paste0("Depth ", i, ": ", round(median * 1000, digits = 0), "ms - worst median\n"))
  }
}

analise("data.csv")

