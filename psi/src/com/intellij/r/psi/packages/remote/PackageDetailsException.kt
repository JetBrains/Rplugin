package com.intellij.r.psi.packages.remote

sealed class PackageDetailsException(message: String) : RuntimeException(message)

/**
 * This error is thrown when package manager is unable to gather list of available packages.
 * This can be caused by networking issues
 */
class MissingPackageDetailsException(message: String) : PackageDetailsException(message)

/**
 * This error is thrown when package manager cannot resolve particular package name.
 * This can be caused by missing repository or package name misspelling
 */
class UnresolvedPackageDetailsException(message: String) : PackageDetailsException(message)