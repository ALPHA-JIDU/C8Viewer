package com.jihad.chatviewer.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

object SafUriHelper {

    fun resolveMediaUri(context: Context, relativePath: String): Uri? {
        val sp = context.getSharedPreferences("chatviewer", Context.MODE_PRIVATE)
        val tree = sp.getString("tree", null) ?: return null
        val base = sp.getString("base", null) ?: return null
        return try {
            DocumentsContract.buildDocumentUriUsingTree(Uri.parse(tree), "$base/$relativePath")
        } catch (_: Exception) {
            null
        }
    }
}
