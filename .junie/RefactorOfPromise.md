# Examples for converting Promise-based code to Kotlin coroutines

How to await Promise in coroutine

```
import org.jetbrains.concurrency.await

val result = Promise.await()
```

How to convert Deferred<T> to Promise<T>

```
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.asPromise

val promise = deferred.asCompletableFuture().asPromise()
```

How to run code on EDT (event dispatch thread):

```
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.intellij.openapi.application.EDT

withContext(Dispatchers.EDT) {
  // code on EDT
}
// code executed after
```

How to launch coroutine from non-coroutine code

```
import org.jetbrains.r.RPluginCoroutineScope

RPluginCoroutineScope.getScope(project).launch(ModalityState.defaultModalityState().asContextElement()) {
  // coroutine with default dispatchers
}
// this code executed right after launching coroutine, coroutine is executed on it's own thread

RPluginCoroutineScope.getScope(project).launch(Dispatchers.EDT + ModalityState.defaultModalityState().asContextElement()) {
  // code executed on EDT
}
```

How to get Promise with the result from coroutine

```
val promiseOfT = RPluginCoroutineScope.getScope(project).async{ 
  computeT() 
}.asCompletableFuture().asPromise()
```

The main goal is to refactor Promise-based code into Kotlin coroutines.
Sometimes it's hard to fix all at once. So changes could be done partially with the help of converting methods between promises and
coroutines.

## Example of refactoring:

Before changes:

```
  fun navigateToFile(rInterop: RInterop, args: RObject): Promise<RObject> {
    val filePath = args.list.getRObjects(0).rString.getStrings(0)
    val line = args.list.getRObjects(1).rInt.getInts(0).toInt() - 1
    val column = args.list.getRObjects(1).rInt.getInts(1).toInt() - 1
    val promise = AsyncPromise<RObject>()
    val filePromise = findFileByPathAtHostHelper(rInterop, filePath)
    filePromise.then {
      it ?: promise.setResult(rError("$filePath does not exist."))
      runInEdt {
        FileEditorManager.getInstance(rInterop.project)
          .openTextEditor(OpenFileDescriptor(rInterop.project, it!!, line, column), true)
        promise.setResult(getRNull())
      }
    }
    return promise
  }
```

After:

```
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.concurrency.await
...

  suspend fun navigateToFile(rInterop: RInterop, args: RObject): RObject {
    val filePath = args.list.getRObjects(0).rString.getStrings(0)
    val line = args.list.getRObjects(1).rInt.getInts(0).toInt() - 1
    val column = args.list.getRObjects(1).rInt.getInts(1).toInt() - 1
    val file = findFileByPathAtHostHelper(rInterop, filePath).await()
    if (file == null) return rError("$filePath does not exist.")

    withContext(Dispatchers.EDT) {
      FileEditorManager.getInstance(rInterop.project)
        .openTextEditor(OpenFileDescriptor(rInterop.project, file, line, column), true)
    }

    return getRNull()
  }
```

## Another example of refactoring:

Note that Swing and UI elements should be created and accessed in EDT.
Because of this `withContext(Dispatchers.EDT)` was used for whole function body

Before:

```
  fun askForPassword(args: RObject): Promise<RObject> {
    val message = args.rString.getStrings(0)
    lateinit var password: JBPasswordField
    val panel = panel {
      row { label(message) }
      row {
        password = passwordField()
          .focused()
          .columns(30)
          .addValidationRule(RBundle.message("rstudioapi.show.dialog.password.not.empty")) { it.password.isEmpty() }
          .component
      }
    }
    val promise = AsyncPromise<RObject>()
    runInEdt {
      val result = if (dialog("", panel).showAndGet()) {
        password.password.joinToString("").toRString()
      }
      else {
        getRNull()
      }
      promise.setResult(result)
    }
    return promise
  }
```

After:

```
  suspend fun askForPassword(args: RObject): RObject =
    withContext(Dispatchers.EDT) {
      val message = args.rString.getStrings(0)
      lateinit var password: JBPasswordField
      val panel = panel {
        row { label(message) }
        row {
          password = passwordField()
            .focused()
            .columns(30)
            .addValidationRule(RBundle.message("rstudioapi.show.dialog.password.not.empty")) { it.password.isEmpty() }
            .component
        }
      }

      if (dialog("", panel).showAndGet()) {
        password.password.joinToString("").toRString()
      }
      else {
        getRNull()
      }
    }
```

## Here, or example, EDT block could be shrunk:

Before:

```
  fun showDialog(args: RObject): Promise<RObject> {
    val (title, message, url) = args.rString.stringsList
    val msg = RBundle.message("rstudioapi.show.dialog.message", message, url)
    val promise = AsyncPromise<RObject>()
    runInEdt {
      Messages.showInfoMessage(msg, title)
      promise.setResult(getRNull())
    }
    return promise
  }
```

After:

```
  suspend fun showDialog(args: RObject): RObject {
    val (title, message, url) = args.rString.stringsList
    val msg = RBundle.message("rstudioapi.show.dialog.message", message, url)
    withContext(Dispatchers.EDT) {
      Messages.showInfoMessage(msg, title)
    }
    return getRNull()
  }
```
