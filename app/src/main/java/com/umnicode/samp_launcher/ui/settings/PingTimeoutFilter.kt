package com.umnicode.samp_launcher.ui.settings

import android.text.InputFilter
import android.text.Spanned

class PingTimeoutFilter : InputFilter {
    private var Min: Int

    constructor() {
        Min = 1
    }

    constructor(min: Int) {
        Min = min
    }

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dStart: Int,
        dEnd: Int
    ): CharSequence?{
        try {
            val Value = (dest.toString() + source.toString()).toInt()
            if (Value >= Min) {
                return null
            }
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
        return ""
    }
}