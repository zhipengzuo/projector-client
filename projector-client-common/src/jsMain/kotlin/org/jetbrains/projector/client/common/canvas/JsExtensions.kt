package org.jetbrains.projector.client.common.canvas

import org.w3c.dom.HTMLCanvasElement


object JsExtensions {

  fun Long.argbIntToRgbaString(): String {
    val argb = this.toInt()
    return argb.argbIntToRgbaString();
  }

  val rgbStrCache = IntRange(0,255)
    .map { i -> if(i < 0x10) "0" + i.toString(16) else i.toString(16)  }
    .toTypedArray()

  /**
   * ARGB -> RGBA
   */
  @Suppress("UNUSED_VARIABLE")
  fun Int.argbIntToRgbaString(): String {
    val argb = this
    val strCache = rgbStrCache
    return js("""
        "#" + strCache[(argb >>> 16) & 0xff] + strCache[(argb >>> 8) & 0xff] + strCache[argb & 0xff] + strCache[(argb >>> 24) & 0xff];
      """).unsafeCast<String>()
  }


}
