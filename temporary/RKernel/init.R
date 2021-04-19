#  Rkernel is an execution kernel for R interpreter
#  Copyright (C) 2019 JetBrains s.r.o.
#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program.  If not, see <https:#www.gnu.org/licenses/>.


if (".jetbrains" %in% ls(globalenv(), all.names = TRUE)) {
  rm(".jetbrains", envir = globalenv())
}

# Set hook to record previous plot before new vanilla graphics are created
setHook(hookName = "before.plot.new",
        value = function() {
          .Call(".jetbrains_ther_device_record", FALSE)
        },
        action = "append")

# Set hook to record previous plot before new ggplot2 graphics are created
setHook(hookName = "before.grid.newpage",
        value = function() {
          .Call(".jetbrains_ther_device_record", TRUE)  # Pass TRUE to indicate it was triggered by ggplot2
        },
        action = "append")

# Force interpreter to restart custom graphics device
options(device = function() {
  .Call(".jetbrains_ther_device_restart")
})

.jetbrains$defaultPlatformGUI <<- .Platform$GUI

# Some packages might be not available as binaries.
# The default behaviour of interpreter in such a case
# is to ask user whether he wants to install it from source instead.
# This is not desirable that's why interpreter will be forced to install packages from source
# **when necessary** without user permission
options(install.packages.compile.from.source = "always")

.jetbrains$init <<- function(rsession.path, project.dir) {
  current.wd <- getwd()
  tryCatch({
    tools.path <- file.path(rsession.path, "Tools.R")
    options.path <- file.path(rsession.path, "Options.R")
    api.path <- file.path(rsession.path, "Api.R")
    modules.path <- file.path(rsession.path, "modules")
    sessionJobs.path <- file.path(rsession.path, "modules/SessionJobs.R")
    setwd(modules.path)
    source(tools.path, local = TRUE)
    source(sessionJobs.path, local = TRUE)
    source(api.path, local = TRUE)
    source(options.path, local = TRUE)
    sapply(Filter(function(s) s != "SessionCompileAttributes.R" & s != "SessionPlots.R",
                  list.files(modules.path, pattern = ".*\\.r$", ignore.case = TRUE)),
                  function(x) { source(file.path(modules.path, x), local = TRUE) } )
    .rs.getProjectDirectory <<- function() project.dir
    options(BuildTools.Check = NULL)
  }, finally = {
    setwd(current.wd)
  })
}

.jetbrains$setRStudioAPIEnabled <<- function(isEnabled) {
  unlockBinding(".Platform", baseenv())
  if (isEnabled) {
    .Platform$GUI <<- "RStudio"
  } else {
    .Platform$GUI <<- .jetbrains$defaultPlatformGUI
  }
  lockBinding(".Platform", baseenv())
}

.jetbrains$updatePackageEvents <<- function() {
  packageNames <- base::list.dirs(.libPaths(), full.names = FALSE, recursive = FALSE)
  sapply(packageNames, function(name) {
    if (!(name %in% .rs.jbHookedPackages)) {
      loadEventName = packageEvent(name, "onLoad")
      onPackageLoaded <- function(name, ...) {
        .rs.reattachS3Overrides()
      }
      setHook(loadEventName, onPackageLoaded, action = "append")
      .rs.setVar("jbHookedPackages", append(.rs.jbHookedPackages, name))
    }
  })
}

.jetbrains$toSystemIndependentPath <<- function(path) {
  gsub("\\\\", "/", path)
}

.jetbrains$createTempDirectory <<- function(suffix) {
  pattern <- paste0("jetbrains_", suffix)
  path <- tempfile(pattern = pattern)
  dir.create(path)
  .jetbrains$toSystemIndependentPath(path)
}

# Fallback values
.jetbrains$chunkOutputDir <<- "."
.jetbrains$externalImageDir <<- "."
.jetbrains$externalImageCounter <<- 0

# Note: used in "NotebookHtmlWidgets.R" of RSession
.jetbrains$getNextExternalImagePath <<- function(path) {
  snapshot.count <- .Call(".jetbrains_ther_device_snapshot_count")
  if (is.null(snapshot.count)) {
    snapshot.count <- 0
  }
  .jetbrains$externalImageCounter <<- .jetbrains$externalImageCounter + 1  # Also ensure it's > 0
  base.name <- paste0("image_", snapshot.count - 1, "_", .jetbrains$externalImageCounter, ".", tools::file_ext(path))
  file.path(.jetbrains$externalImageDir, base.name)
}

.jetbrains$runBeforeChunk <<- function(report.text, chunk.text) {
  .rs.evaluateRmdParams(report.text)
  opts <- .rs.evaluateChunkOptions(chunk.text)
  output.dir <- .jetbrains$createTempDirectory("chunk_outputs")
  .jetbrains$chunkOutputDir <<- output.dir
  data.dir <- file.path(output.dir, "data")
  html.lib.dir <- file.path(output.dir, "lib")
  image.dir <- file.path(output.dir,  "images")
  external.image.dir <- file.path(output.dir, "external-images")
  dir.create(image.dir, showWarnings = FALSE)
  dir.create(external.image.dir, showWarnings = FALSE)
  dir.create(html.lib.dir, showWarnings = FALSE)
  dir.create(data.dir, showWarnings = FALSE)

  .jetbrains$externalImageDir <<- external.image.dir
  .jetbrains$externalImageCounter <<- 0
  .rs.initHtmlCapture(output.dir, html.lib.dir, opts)
  .rs.initDataCapture(data.dir, opts)
  if (!.rs.hasVar("jbHookedPackages")) {
    .rs.setVar("jbHookedPackages", character())
  }
  .jetbrains$updatePackageEvents()  # Note: supposed to be useless when packages are getting installed within chunk but for my machine it's OK
}

.jetbrains$runAfterChunk <<- function() {
  .rs.releaseHtmlCapture()
  .rs.releaseDataCapture()
  unlink(.jetbrains$chunkOutputDir, recursive = TRUE)
}

.jetbrains$getChunkOutputPaths <<- function() {
  relative.paths <- list.files(.jetbrains$chunkOutputDir, recursive = TRUE, include.dirs = FALSE, full.names = FALSE)
  c(.jetbrains$chunkOutputDir, relative.paths)
}

.jetbrains$getChunkOutputFullPath <<- function(relative.path) {
  file.path(.jetbrains$chunkOutputDir, relative.path)
}

.jetbrains$findInheritorNamedArguments <<- function(x) {
  ignoreErrors <- function(expr) {
    as.list(tryCatch(expr, error = function(e) { }))
  }

  defenv <- if (!is.na(w <- .knownS3Generics[x])) {
    asNamespace(w)
  } else {
    genfun <- get(x, mode = "function")
    if (.isMethodsDispatchOn() && methods::is(genfun, "genericFunction")) {
      genfun <- methods::finalDefaultMethod(genfun@default)
    }
    if (typeof(genfun) == "closure") environment(genfun)
    else .BaseNamespaceEnv
  }
  s3_table <- get(".__S3MethodsTable__.", envir = defenv)

  getS3Names <- function(row) {
    functionName <- row[["functionName"]]
    from <- row[["from"]]
    envir <- tryCatch(as.environment(from), error = function(e) NULL)
    if (startsWith(from, "registered S3method")) {
      envir <- s3_table
    }
    if (is.null(envir)) {
      envir <- tryCatch(as.environment(paste0("package:", from)), error = function(e) .GlobalEnv)
    }
    names(formals(get(functionName, mode = "function", envir = envir)))
  }

  s4 <- ignoreErrors(names(formals(getMethod(x))))
  if (utils:::findGeneric(x, parent.frame(), warnS4only = FALSE) == "") {
    s3 <- NULL
  }
  else {
    s3Info <- attr(.S3methods(x), "info")
    s3Info[, "functionName"] <- rownames(s3Info)
    s3 <- ignoreErrors(apply(s3Info[, c("functionName", "from")], 1, getS3Names))
  }
  unique(unlist(c(s4, s3, names(formals(x)))))
}

.jetbrains$printAllPackagesToFile <<- function(repo.urls, output.path) {
  remove.newlines <- function(s) {
    gsub("\r?\n|\r", " ", s)
  }

  old.repos <- getOption("repos")
  options(repos = repo.urls)
  sink(output.path)
  p <- available.packages()[, c("Package", "Repository", "Version", "Depends")]
  p <- as.data.frame(p)
  p$Depends <- sapply(p$Depends, remove.newlines)
  with(p, cat(paste(paste(Package, Repository, Version, sep = " "), Depends, sep = "\t"), sep = "\n"))
  sink()
  options(repos = old.repos)
}

.jetbrains$getDefaultRepositories <<- function() {
  p <- file.path(Sys.getenv("HOME"), ".R", "repositories")
  if (!file.exists(p))
    p <- file.path(R.home("etc"), "repositories")
  a <- tools:::.read_repositories(p)
  a[, "URL"]
}

.jetbrains$printCranMirrorsToFile <<- function(output.path) {
  sink(output.path)
  mirrors <- getCRANmirrors()[, c('Name', 'URL')]
  with(mirrors, cat(paste(Name, URL, sep = "\t"), sep = "\n"))
  sink()
}

.jetbrains$printInstalledPackagesToFile <<- function(output.path) {
  sink(output.path)
  versions <- as.data.frame(installed.packages()[, c("Package", "Version", "Priority", "LibPath")])
  with(versions, cat(paste(LibPath, Package, Version, Priority, sep = "\t"), sep = "\n"))
  sink()
}

.jetbrains$initGraphicsDevice <<- function(width, height, resolution, in.memory) {
  path <- .jetbrains$createSnapshotGroup()
  .Call(".jetbrains_ther_device_init", path, width, height, resolution, in.memory)
  path
}

.jetbrains$findStoredSnapshot <<- function(directory, number) {
  pattern <- paste0("^snapshot_normal_", number, "_")  # Note: trailing underscore will cut off remaining digits if any
  snapshots <- list.files(directory, pattern = pattern, full.names = FALSE)
  if (length(snapshots) > 0) {
    return(snapshots[1])
  } else {
    return(NULL)
  }
}

.jetbrains$createSnapshotGroup <<- function() {
  .jetbrains$createTempDirectory("snapshot_group")
}

.jetbrains$shutdownGraphicsDevice <<- function() {
  path <- .Call(".jetbrains_ther_device_shutdown")
  if (!is.null(path)) {
    unlink(path, recursive = TRUE)
  }
  NULL
}

.jetbrains$saveRecordedPlotToFile <<- function(snapshot, output.path) {
  .jetbrains.recorded.snapshot <- snapshot
  save(.jetbrains.recorded.snapshot, file = output.path)
}

.jetbrains$replayPlotFromFile <<- function(input.path) {
  load(input.path)
  plot <- .jetbrains.recorded.snapshot

  # restore native symbols for R >= 3.0
  rVersion <- getRversion()
  if (rVersion >= "3.0") {
    for (i in 1:length(plot[[1]])) {
      # get the symbol then test if it's a native symbol
      symbol <- plot[[1]][[i]][[2]][[1]]
      if ("NativeSymbolInfo" %in% class(symbol)) {
        # determine the dll that the symbol lives in
        name = if (!is.null(symbol$package)) symbol$package[["name"]] else symbol$dll[["name"]]
        pkgDLL <- getLoadedDLLs()[[name]]

        # reconstruct the native symbol and assign it into the plot
        nativeSymbol <- getNativeSymbolInfo(name = symbol$name, PACKAGE = pkgDLL, withRegistrationInfo = TRUE);
        plot[[1]][[i]][[2]][[1]] <- nativeSymbol;
      }
    }
  }

  # Replay obtained plot
  suppressWarnings(grDevices::replayPlot(plot, reloadPkgs=TRUE))
}

.jetbrains$dropRecordedSnapshots <<- function(device.number, from, to) {
  for (i in from:to) {
    name <- paste0("recordedSnapshot_", device.number, "_", i)
    if (exists(name, envir = .jetbrains)) {
      assign(name, NULL, .jetbrains)
    }
  }
}

.jetbrains$getLoadedS4ClassInfos <<- function() {
  classTable <- methods:::.classTable
  infos <- lapply(names(classTable), function(className) {
    class <- classTable[[className]]
    if (inherits(class, "classRepresentation")) class
  })
  Filter(function(x) !is.null(x), infos)
}

.jetbrains$isObjectFromR6 <<- function(object) {
  return(is.R6(object))
}

.jetbrains$getR6ClassName <<- function(x) {
  return(class(x)[1])
}

.jetbrains$getR6ClassInheritanceTree <<- function(x) {
  return(head(class(x), -1)[-1])
}

.jetbrains$getR6ClassDefMembers <<- function(x) {
  return(names(x)[-1])
}

.jetbrains$getSysEnv <<- function(env_name, flags) {
  s <- Sys.getenv(env_name)
  s <- strsplit(s, .Platform$path.sep)[[1]]
  if ("--normalize-path" %in% flags) {
    s <- sapply(s, function (p) {
      normalized <- normalizePath(p)
      .jetbrains$toSystemIndependentPath(normalized)
    })
  }
  s
}

.jetbrains$loadLibraryPath <<- function() {
  res <- NULL
  for (path in .libPaths()) {
    res <- c(res, list(path, file.access(path, 2) == 0))
  }
  res
}

.jetbrains$loadInstalledPackages <<- function() {
  versions <- as.data.frame(installed.packages()[, c("Package", "Version", "Priority", "LibPath")])
  canonicalPackagePaths <- data.frame(CanonicalPath = apply(versions, 1, function(row) {
    normalizePath(file.path(row["LibPath"], row["Package"]))
  }))
  description <- data.frame("Title" = I(lapply(versions[, "Package"], function(x) packageDescription(x, fields = "Title"))),
                            "URL" = I(lapply(versions[, "Package"], function(x) packageDescription(x, fields = "URL"))))
  cbind(versions, canonicalPackagePaths, description)
}

.jetbrains$unloadLibrary <<- function(package.name, with.dynamic.library) {
  resource.name <- paste0("package:", package.name)
  detach(resource.name, unload = TRUE, character.only = TRUE)
  if (with.dynamic.library) {
    .jetbrains$unloadDynamicLibrary(package.name)
  }
}

.jetbrains$unloadDynamicLibrary <<- function(package.name) {
  if (.jetbrains$isDynamicLibraryLoaded(package.name)) {
    pd.file <- attr(packageDescription(package.name), "file")
    lib.path <- sub("/Meta.*", "", pd.file)
    library.dynam.unload(package.name, libpath = lib.path)
  }
}

.jetbrains$isDynamicLibraryLoaded <<- function(package.name) {
  for (lib in .dynLibs()) {
    name <- lib[[1]]
    if (name == package.name) {
      return(TRUE)
    }
  }
  FALSE
}

.jetbrains$previewDataImportResult <<- NULL

.jetbrains$previewDataImport <<- function(path, mode, row.count, importOptions) {
  result <- if (mode != "base") {
    .jetbrains$previewAdvancedDataImport(path, mode, row.count, importOptions)
  } else {
    .jetbrains$previewBaseDataImport(path, row.count, importOptions)
  }
  .jetbrains$previewDataImportResult <<- result
  if (is.null(result)) {
    return(NULL)
  }
  result$parsingErrors
}

.jetbrains$commitDataImport <<- function(path, mode, importOptions) {
  .jetbrains$previewDataImport(path, mode, NULL, importOptions)
  result <- .jetbrains$previewDataImportResult
  .jetbrains$previewDataImportResult <<- NULL
  if (is.null(result)) {
    return(NULL)
  }
  result$data
}

.jetbrains$previewAdvancedDataImport <<- function(path, mode, row.count, importOptions) {
  importOptions$openDataViewer <- FALSE
  importOptions$importLocation <- path
  importOptions$modelLocation <- NULL
  importOptions$mode <- mode
  if (!is.null(row.count)) {
    importOptions$maxRows <- row.count

    # Excel's special
    if (is.null(importOptions$nMax) || importOptions$nMax > row.count) {
      importOptions$nMax <- row.count
    }
  }
  .rs.previewDataImport(importOptions)
}

.jetbrains$previewBaseDataImport <<- function(path, row.count, importOptions) {
  if (!is.null(row.count)) {
    importOptions$nrows <- row.count
  }
  importOptions$file <- path
  tryCatch({
    # try to use read.csv directly if possible (since this is a common case
    # and since LibreOffice spreadsheet exports produce files unparsable
    # by read.table). check Workspace.makeCommand if we want to deduce
    # other more concrete read calls.
    data <- if (identical(importOptions$sep, ",") && identical(importOptions$dec, ".") && identical(importOptions$quote, "\"")) {
      importOptions$sep <- NULL
      importOptions$dec <- NULL
      importOptions$quote <- NULL
      do.call(read.csv, importOptions)
    } else {
      do.call(read.table, importOptions)
    }
    return(list(data = data, parsingErrors = 0))
  }, error=function(e) {
    data <- data.frame(Error = e$message)
    parsingErrors <- if (!is.null(row.count)) row.count else 0
    return(list(data = data, parsingErrors = parsingErrors))
  })
}

.jetbrains$convertRoxygenToHTML <<- function(functionName, text) {
  text <- format(
    roxygen2:::roclet_process.roclet_rd(, roxygen2:::parse_text(text), base_path = ".")[[paste0(functionName, ".Rd")]])
  links = gsub("^\\.\\./\\.\\./", "/library/", tools::findHTMLlinks())
  text <- utils::capture.output(tools::Rd2HTML(textConnection(text), Links = links))
  return(paste(text, collapse = "\n"))
}

local({
  handlersEnv <- tools:::.httpd.handlers.env
  handlersEnv$jb_get_file <- function(path, query, ...) {
    prefix <- "/custom/jb_get_file/"
    file <- substr(path, nchar(prefix) + 1, nchar(path))
    if (!file.exists(file)) {
      return(list(payload = paste0("No such file ", file)))
    }
    mimeType <- function(path) {
      ext <- strsplit(path, ".", fixed = TRUE)[[1]]
      if (n <- length(ext)) ext <- ext[n] else ""
      switch(ext, css = "text/css", gif = "image/gif", jpg = "image/jpeg", png = "image/png", svg = "image/svg+xml",
             html = "text/html", pdf = "application/pdf", eps = , ps = "application/postscript",
             sgml = "text/sgml", xml = "text/xml", "text/plain")
    }
    return(list(file = file, `content-type` = mimeType(path)))
  }
})

local({
  env <- as.environment("package:utils")
  unlockBinding("View", env)
  env$View <- function(x, title = paste(deparse(substitute(x)), collapse = " "))
    invisible(.Call(".jetbrains_View", substitute(x), parent.frame(), title))
  lockBinding("View", env)
})

if (.Platform$OS.type == "unix" && !("UTF-8" %in% localeToCharset(Sys.getlocale("LC_CTYPE")))) {
  if (grepl("^darwin", R.version$os)) {
    Sys.setlocale("LC_CTYPE", "UTF-8")
  } else {
    Sys.setlocale("LC_CTYPE", "C.UTF-8")
  }
}

options(warn = 1)
options(demo.ask = TRUE);
assign(".Last.sys", function() .Call(".jetbrains_quitRWrapper"), envir = baseenv())
