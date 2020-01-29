/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages

import junit.framework.TestCase
import org.jetbrains.r.RUsefulTestCase

class RPackageVersionTest : RUsefulTestCase() {
  fun testParseValidVersionString() {
    val string2numbers = mapOf(
      "" to emptyList<Int>(),
      "1" to listOf(1),
      "123" to listOf(123),
      "1.2" to listOf(1, 2),
      "12.34" to listOf(12, 34),
      "1.2.3" to listOf(1, 2, 3),
      "1.23-4" to listOf(1, 23, 4),
      "1.2.3.4" to listOf(1, 2, 3, 4),
      "1.23-45.6" to listOf(1, 23, 45, 6),
      "12-345.6789-12345" to listOf(12, 345, 6789, 12345)
    )
    for ((string, expectedNumbers) in string2numbers) {
      val actualNumbers = RPackageVersion.parse(string)
      TestCase.assertNotNull("Cannot parse version string '$string'", actualNumbers)
      TestCase.assertEquals("Incorrect version numbers for string '$string'", expectedNumbers, actualNumbers)
    }
  }

  fun testParseInvalidVersionString() {
    val strings = listOf(
      ".",
      "-",
      ".-",
      "1..2",
      "12.-34",
      "1...2",
      "1.-.23",
      "1.2.e"
    )
    for (string in strings) {
      val numbers = RPackageVersion.parse(string)
      TestCase.assertNull("Incorrectly treated '$string' as a valid version string", numbers)
    }
  }

  fun testCompareEqualValidVersionStrings() {
    val samples = listOf(
      Sample("", "", Order.EQUAL),
      Sample(null, "", Order.EQUAL),
      Sample(null, null, Order.EQUAL),
      Sample("1.2-3", "1.2-3", Order.EQUAL),
      Sample("1.2.3.4", "1.2-3.4", Order.EQUAL)  // Note: dot instead of hyphen
    )
    checkSamplesWithInversion(samples)
  }

  fun testCompareUnequalValidVersionStrings() {
    val samples = listOf(
      Sample(null, "1.2-3", Order.LESS),
      Sample("1.2", "1.2.3", Order.LESS),
      Sample("1.2.3", "1.2.4", Order.LESS),
      Sample("1.9", "1.10.0", Order.LESS),
      Sample("1.9.9", "1.10.0", Order.LESS),
      Sample("1.9.9.9", "1.10.0", Order.LESS),
      Sample("1.27", "2.1", Order.LESS)
    )
    checkSamplesWithInversion(samples)
  }

  fun testCompareInvalidVersionStrings() {
    val samples = listOf(
      Sample("", ".", null),
      Sample(null, ".", null),
      Sample(null, "1...2", null),
      Sample("1.2.3", "1.2.e", null)
    )
    checkSamplesWithInversion(samples)
  }

  private fun checkSamplesWithInversion(samples: List<Sample>) {
    checkSamples(samples.map { it.inverted })
    checkSamples(samples)
  }

  private fun checkSamples(samples: List<Sample>) {
    for (sample in samples) {
      val actualOrder = RPackageVersion.compare(sample.aVersion, sample.bVersion).toOrder()
      TestCase.assertEquals("Incorrect comparison of '${sample.aVersion}' and '${sample.bVersion}'", sample.expectedOrder, actualOrder)
    }
  }

  private fun Int?.toOrder(): Order? {
    return this?.let { value ->
      when {
        value > 0 -> Order.GREATER
        value < 0 -> Order.LESS
        else -> Order.EQUAL
      }
    }
  }

  private val Sample.inverted: Sample
    get() = Sample(bVersion, aVersion, expectedOrder.inverted)

  private val Order?.inverted: Order?
    get() = this?.let { order ->
      when (order) {
        Order.LESS -> Order.GREATER
        Order.GREATER -> Order.LESS
        else -> Order.EQUAL
      }
    }

  private data class Sample(val aVersion: String?, val bVersion: String?, val expectedOrder: Order?)

  private enum class Order {
    GREATER,
    EQUAL,
    LESS,
  }
}
