# Project Guidelines

It is an IntelliJ plugin for support of R language and RMarkdown.

## Code structure

Code of R plugin is in the "rplugin" directory.
Don't change code outside, it is related to other projects.
The main programming language is Kotlin.

Directory "rplugin/rwrapper" contains a wrapper for R interpreter written in C++ and R.
This part communicated with Kotlin code via GRPC.
It is a low-level part, usually it doesn't require changes.
Don't inspect code here if it is not necessary.

## Code style:

Prefer immutable classes, write declarative code.

Use early return in function, for example:

```
fun f() {
  if (!check) return
  val r = getSmth() ?: return
  finalDo(r)
}
```

This helps to reduce levels of code nesting and shows a 'happy path' of execution.

When then and else is a small one-liners, write code like this:

```
if (cond) codeLine
else codeLine
```

Code must be readable, fill free to add empty line between logically different blocks of code inside one function.

Project contains code with obsolete functions and classes with `Promise`, `invokeLater`, `runAsync`, better to replace them with coroutines.

Do not run tests, just check that the code compiles.

Each non-trivial class should be in its own file, prefer small simple classes over big and complex.

It's hard to fix all problems at once, better to do small atomic code changes where each step is simple and obvious.
For example, rename class, change method signature, propagate constant, etc.

## Helper files:

1. [RefactorOfPromise.md]: examples how Promises could be refactored to kotlin coroutines
