package com.terfess.samp_launcher.ui.home

import android.text.InputFilter
import android.text.Spanned

class PortFilter : InputFilter {
    private var Min: Int
    private var Max: Int

    constructor() {
        Min = 0
        Max = 65535
    }

    constructor(min: Int, max: Int) {
        Min = min
        Max = max
    }

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dStart: Int,
        dEnd: Int
    ): String? {
        try {
            val Value = (dest.toString() + source.toString()).toInt()
            if (IsInRange(Value, Min, Max)) {
                return null
            }
        } catch (nfe: NumberFormatException) {
        }
        return ""
    }

    companion object {
        fun IsInRange(Number: Int, Min: Int, Max: Int): Boolean {
            return Number >= Min && Number <= Max
        }
    }
}