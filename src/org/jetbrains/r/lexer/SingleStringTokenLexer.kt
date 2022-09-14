package org.jetbrains.r.lexer

/**
 * @property tokenString the string to be match
 * @property outputBuffer the buffer to be transmitted symbols then matching [tokenString] fails
 */
class SingleStringTokenLexer(private val tokenString: String, private val outputBuffer: StringBuffer) {
  private var pos: Int = 0
  private val commandBuffer = StringBuffer()

  /**
   * @return `true` if completely match the [tokenString] after consumption [char]. `false` otherwise
   * @throws IllegalStateException if [tokenString] already matched
   */
  fun advanceChar(char: Char): Boolean {
    if (consume(char)) {
      commandBuffer.append(char)
    }
    else {
      outputBuffer.append(commandBuffer)
      commandBuffer.setLength(0)
      if (pos == 1) {
        commandBuffer.append(char)
      }
      else {
        outputBuffer.append(char)
      }
    }
    return commandBuffer.length == tokenString.length
  }

  fun restore() {
    pos = 0
    commandBuffer.setLength(0)
  }

  private fun consume(c: Char): Boolean {
    if (pos >= tokenString.length) throw IllegalStateException("The string '$tokenString' already matched")
    val match = tokenString[pos] == c
    pos = if (match) pos + 1 else (if (tokenString[0] == c) 1 else 0)
    return match
  }
}