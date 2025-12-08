package com.intellij.r.psi.visualization.inlays.components

import com.intellij.util.messages.Topic

val CHANGE_DARK_MODE_TOPIC = Topic.create("Graphics Panel Dark Mode Topic", DarkModeNotifier::class.java)

interface DarkModeNotifier {
  fun onDarkModeChanged(isEnabled: Boolean)
}