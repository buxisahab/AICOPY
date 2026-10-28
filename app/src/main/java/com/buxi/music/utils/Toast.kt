package com.buxi.music.utils

import android.content.Context

object Toast {
    const val LENGTH_SHORT = 0
    const val LENGTH_LONG = 1

    fun makeText(context: Context, text: CharSequence, duration: Int): ToastInstance {
        return ToastInstance()
    }
    
    fun makeText(context: Context, resId: Int, duration: Int): ToastInstance {
        return ToastInstance()
    }

    class ToastInstance {
        fun show() {
            // Do nothing! Toast messages are intentionally disabled to improve UX as requested.
        }
    }
}
