package org.jetbrains.r.rinterop.rstudioapi

import com.intellij.r.psi.rinterop.RObject
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.toRBoolean
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.toRString
import java.awt.Color
import javax.swing.UIManager

object ThemeUtils {
  fun getThemeInfo(): RObject {
    return RObject.newBuilder()
      .setNamedList(RObject.NamedList.newBuilder()
                      .addRObjects(0, RObject.KeyValue.newBuilder().setKey("editor").setValue(UIManager.getLookAndFeel().name.toRString()))
                      .addRObjects(1, RObject.KeyValue.newBuilder().setKey("global").setValue("Modern".toRString()))
                      .addRObjects(2, RObject.KeyValue.newBuilder().setKey("dark").setValue(UIUtil.isUnderDarcula().toRBoolean()))
                      .addRObjects(3, RObject.KeyValue.newBuilder().setKey("foreground").setValue(
                        colorHelper(JBColor.foreground()).toRString())
                      )
                      .addRObjects(4, RObject.KeyValue.newBuilder().setKey("background").setValue(
                        colorHelper(JBColor.background()).toRString())
                      )
      ).build()
  }

  private fun colorHelper(color: Color): String {
    return "rgb(${color.red}, ${color.green}, ${color.blue})"
  }
}