package org.mozilla.xiu.browser.utils

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.LabeledIntent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File

/**
 * 作者：By 15968
 * 日期：On 2022/1/19
 * 时间：At 14:24
 */
object UriTool {
    @SuppressLint("Range")
    fun uriToFileName(uri: Uri, context: Context): String {
        return when (uri.scheme) {
            ContentResolver.SCHEME_FILE -> File(uri.path!!).name
            ContentResolver.SCHEME_CONTENT -> {
                try {
                    val cursor = context.contentResolver.query(uri, null, null, null, null, null)
                    cursor?.let {
                        it.moveToFirst()
                        val displayName =
                            it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        cursor.close()
                        displayName
                    } ?: "${System.currentTimeMillis()}.${
                        MimeTypeMap.getSingleton()
                            .getExtensionFromMimeType(context.contentResolver.getType(uri))
                    }}"
                } catch (e: Exception) {
                    "${System.currentTimeMillis()}.${
                        MimeTypeMap.getSingleton()
                            .getExtensionFromMimeType(context.contentResolver.getType(uri))
                    }}"
                }

            }
            else -> "${System.currentTimeMillis()}.${
                MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(context.contentResolver.getType(uri))
            }}"
        }
    }

    fun getIntentChooser(
        context: Context,
        intent: Intent,
        chooserTitle: CharSequence? = null,
        filter: ComponentNameFilter
    ): Intent? {
        val resolveInfos = context.packageManager.queryIntentActivities(intent, 0)
//        Log.d("AppLog", "found apps to handle the intent:")
        val excludedComponentNames = HashSet<ComponentName>()
        resolveInfos.forEach {
            val activityInfo = it.activityInfo
            val componentName = ComponentName(activityInfo.packageName, activityInfo.name)
//            Log.d("AppLog", "componentName:$componentName")
            if (filter.shouldBeFilteredOut(componentName))
                excludedComponentNames.add(componentName)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Intent.createChooser(intent, chooserTitle)
                .putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, excludedComponentNames.toTypedArray())
        }
        if (resolveInfos.isNotEmpty()) {
            val targetIntents: MutableList<Intent> = ArrayList()
            for (resolveInfo in resolveInfos) {
                val activityInfo = resolveInfo.activityInfo
                if (excludedComponentNames.contains(
                        ComponentName(
                            activityInfo.packageName,
                            activityInfo.name
                        )
                    )
                )
                    continue
                val targetIntent = Intent(intent)
                targetIntent.setPackage(activityInfo.packageName)
                targetIntent.component = ComponentName(activityInfo.packageName, activityInfo.name)
                // wrap with LabeledIntent to show correct name and icon
                val labeledIntent = LabeledIntent(
                    targetIntent,
                    activityInfo.packageName,
                    resolveInfo.labelRes,
                    resolveInfo.icon
                )
                // add filtered intent to a list
                targetIntents.add(labeledIntent)
            }
            if(targetIntents.isEmpty()) {
                return null
            }
            // deal with M list seperate problem
            val chooserIntent: Intent = Intent.createChooser(intent, chooserTitle)
            // add initial intents
            chooserIntent.putExtra(
                Intent.EXTRA_INITIAL_INTENTS,
                targetIntents.toTypedArray<Parcelable>()
            )
            return chooserIntent
        }
        return null
    }
}

interface ComponentNameFilter {
    fun shouldBeFilteredOut(componentName: ComponentName): Boolean
}