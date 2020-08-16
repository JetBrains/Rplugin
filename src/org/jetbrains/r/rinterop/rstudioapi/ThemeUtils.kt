package org.jetbrains.r.rinterop.rstudioapi

import com.intellij.util.ui.UIUtil
import org.jetbrains.r.rinterop.RObject
import javax.swing.UIManager

fun getThemeInfo(): RObject {
  // TODO global, foreground, background
  return RObject.newBuilder()
    .setNamedList(RObject.NamedList.newBuilder()
                    .addRObjects(0, RObject.KeyValue.newBuilder().setKey("editor").setValue(UIManager.getLookAndFeel().name.toRString()))
                    .addRObjects(1, RObject.KeyValue.newBuilder().setKey("global").setValue(getRNull()))
                    .addRObjects(2, RObject.KeyValue.newBuilder().setKey("dark").setValue(UIUtil.isUnderDarcula().toRBoolean()))
                    .addRObjects(3, RObject.KeyValue.newBuilder().setKey("foreground").setValue(getRNull()))
                    .addRObjects(4, RObject.KeyValue.newBuilder().setKey("background").setValue(getRNull()))
    ).build()
}