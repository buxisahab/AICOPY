package com.musifly.android.secure

object StringDecode {
    init {
        System.loadLibrary("musifly-secure")
    }

    external fun nSigDecode(nStr: String): String
    external fun sigDecode(inputStr: String): String
}
