package com.krish.systemsync.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ClipboardHelper(private val context: Context) {
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun copyToClipboard(text: String, label: String = "Sensitive Data", clearAfterSeconds: Int = 30) {
        val clip = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clip)
        
        if (clearAfterSeconds > 0) {
            CoroutineScope(Dispatchers.Main).launch {
                delay(clearAfterSeconds * 1000L)
                if (clipboardManager.primaryClip?.getItemAt(0)?.text == text) {
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
                }
            }
        }
    }
}
