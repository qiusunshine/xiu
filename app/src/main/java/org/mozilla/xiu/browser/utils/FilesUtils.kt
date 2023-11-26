package org.mozilla.xiu.browser.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import org.mozilla.xiu.browser.App
import org.mozilla.xiu.browser.R
import java.io.File
import java.io.FileInputStream


/**
 * 作者：By 15968
 * 日期：On 2022/10/9
 * 时间：At 11:07
 */

fun clearCacheDir() {
    ThreadTool.async {
        //删除过期的文件
        val dir = File(
            UriUtilsPro.getRootDir(App.application) +
                    File.separator + "_cache"
        )
        if (!dir.exists()) {
            return@async
        }
        var files = dir.listFiles()
        if (files == null || files.isEmpty()) {
            return@async
        }
        val now = System.currentTimeMillis()
        for (file in files) {
            if (now - file.lastModified() > 1800 * 1000) {
                //删除大于半小时的
                if (!file.isDirectory) {
                    file.delete()
                }
            }
        }
        //删除超出100个的
        files = dir.listFiles()
        if (files == null || files.isEmpty() || files.size <= 100) {
            return@async
        }
        val files2 = files.toMutableList()
        files2.sortByDescending { it.lastModified() }
        for (i in 200 until files2.size) {
            files2[i].delete()
        }
    }
}

fun copyToDownloadDir(context: Context, filePath: String): String? {
    val file = File(filePath)
    if (!file.exists()) {
        return null
    }
    val app: String = context.resources.getString(R.string.app_name)
    val dir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        app
    )
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return copyFileToDownloadDir(
                context,
                file.absolutePath,
                app
            )
        } else {
            val o = getOutFileName(dir, file)
            FileUtil.copyFile(
                file.absolutePath,
                o
            )
            return o
        }
    } catch (e: Throwable) {
        e.printStackTrace()
    }
    return null
}

private fun getOutFileName(dir: File, file: File, count: Int = 0): String {
    var out = dir.absolutePath + File.separator + file.name
    if (count >= 8) {
        return out
    }
    if (File(out).exists()) {
        val index: Int = out.lastIndexOf(".")
        if (index >= 0) {
            val n = out.substring(0, index)
            val s = out.substring(index)
            out = dir.absolutePath + File.separator + n + "(1)" + s
            return getOutFileName(dir, File(out), count + 1)
        }
    }
    return out
}

/**
 * 复制私有目录的文件到公有Download目录
 * @param context 上下文
 * @param oldPath 私有目录的文件路径
 * @param targetDirName 公有目录下的目标文件夹名字。比如传test，则会复制到Download/test目录下。另外如果Download目录下test文件夹不存在，会自动创建。
 * @return 公有目录的uri，为空则代表复制失败
 */
@RequiresApi(Build.VERSION_CODES.Q)
fun copyFileToDownloadDir(
    context: Context,
    oldPath: String,
    targetDirName: String
): String? {
    try {
        val oldFile = File(oldPath)
        //设置目标文件的信息
        val values = ContentValues()
        var name = oldFile.name
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            targetDirName
        )
        if (File(dir, name).exists()) {
            //文件已经存在
            val p = getOutFileName(dir, File(dir, name))
            name = File(p).name
        }
        values.put(MediaStore.Files.FileColumns.DISPLAY_NAME, name)
        values.put(MediaStore.Files.FileColumns.TITLE, name)
        values.put(MediaStore.Files.FileColumns.MIME_TYPE, getMimeType(oldPath))
        var relativePath = Environment.DIRECTORY_DOWNLOADS + File.separator + targetDirName
//        if (!film.isNullOrEmpty()) {
//            relativePath = relativePath + File.separator + film
//        }
        values.put(MediaStore.Files.FileColumns.RELATIVE_PATH, relativePath)
        values.put(MediaStore.Files.FileColumns.SIZE, oldFile.length())
        val downloadUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val resolver = context.contentResolver
        val insertUri = resolver.insert(downloadUri, values)
        if (insertUri != null) {
            resolver.openOutputStream(insertUri)?.use {
                val fis = FileInputStream(oldFile)
                fis.copyTo(it)
            }
            return File(dir, name).absolutePath
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

fun getDownloadDir(context: Context): String {
    val app: String = context.resources.getString(R.string.app_name)
    return File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        app
    ).absolutePath
}

/**
 * 扫描本地文件
 */
fun scanLocalFiles(context: Context): List<FileEntity> {
    val app: String = context.resources.getString(R.string.app_name)
    val dir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        app
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return getFiles(context, app).sortedByDescending { it.timestamp }
    }
    if (dir.exists()) {
        return dir.listFiles()?.filter { !it.isDirectory }?.map {
            FileEntity(
                -1,
                it.name,
                it.absolutePath,
                FileUtil.getFormatedFileSize(it.length()),
                TimeUtil.formatTime(it.lastModified()),
                it.lastModified(),
                Uri.fromFile(it).toString(),
                it.length()
            )
        }?.sortedByDescending { it.timestamp } ?: emptyList()
    } else {
        dir.mkdirs()
    }
    return emptyList()
}

/**
 * 分区存储下扫描本地文件
 */
@RequiresApi(Build.VERSION_CODES.Q)
fun getFiles(context: Context, dir: String): List<FileEntity> {
    val files: MutableList<FileEntity> = ArrayList()
    // 扫描files文件库
    var c0: Cursor? = null
    try {
        val mContentResolver = context.contentResolver
        c0 = mContentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            null
        )
        c0?.let { c ->
            val data: Int =
                c.getColumnIndex(MediaStore.Files.FileColumns.DATA)
            val sizeIndex: Int =
                c.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
            val rPathIndex = c.getColumnIndex(MediaStore.Files.FileColumns.RELATIVE_PATH)
            // 更改时间
            val modified: Int =
                c.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
            while (c.moveToNext()) {
                val rPath = c.getStringOrNull(rPathIndex) ?: ""
                if (!rPath.contains(dir)) {
                    continue
                }
                val id = c.getLongOrNull(c.getColumnIndex(MediaStore.MediaColumns._ID)) ?: 0L
                var path: String? = c.getStringOrNull(data)
                if (path.isNullOrEmpty()) {
                    val dir0 =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    path = dir0.absolutePath + File.separator + rPath
                    val n = c.getStringOrNull(
                        c.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    )
                    path = if (path.endsWith(File.separator)) {
                        path + n
                    } else {
                        path + File.separator + n
                    }
                }

                var displayName: String? = c.getStringOrNull(
                    c.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                )
                if (displayName.isNullOrEmpty()) {
                    val positionX = path?.lastIndexOf(File.separator) ?: 0
                    displayName = path?.substring(positionX + 1, path.length)
                }
                val size: Long = c.getLongOrNull(sizeIndex) ?: 0L
                //似乎取到的需要×1000
                var modifiedDate: Long = c.getLongOrNull(modified) ?: 0L
                try {
                    val file = File(path)
                    if (file.exists() && file.lastModified() > 0) {
                        modifiedDate = file.lastModified()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val name = displayName ?: ""
                val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                val paths = rPath.split(dir + File.separator)
//                val film = if (paths.size > 1 && paths[paths.size - 1].isNotEmpty()) {
//                    paths[paths.size - 1].replace(File.separator, "")
//                } else {
//                    null
//                }
                val info = FileEntity(
                    id,
                    name,
                    path ?: "",
                    FileUtil.getFormatedFileSize(size),
                    TimeUtil.formatTime(modifiedDate),
                    modifiedDate,
                    uri.toString(),
                    size,
//                    film
                )
                files.add(info)
            }
        }
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    } finally {
        c0?.close()
    }
    return files
}

/**
 * 删除文件
 */
fun deleteFile(context: Context, fileEntity: FileEntity): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return deleteFile(context, fileEntity.uri)
    }
    return File(fileEntity.path).delete()
}

/**
 * 删除文件
 */
fun deleteFileByPath(context: Context, uri: String): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return deleteFile(context, uri)
    }
    return File(uri.replace("file://", "")).delete()
}

/**
 * 分区存储下删除本地文件
 */
@RequiresApi(Build.VERSION_CODES.Q)
fun deleteFile(context: Context, uri: String): Boolean {
    try {
        val resolver = context.contentResolver
        return resolver.delete(Uri.parse(uri), null, null) > 0
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
    return false
}

/**
 * 重命名
 */
fun renameFileByPath(context: Context, uri: String, name: String): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return renameFile(context, uri, name)
    }
    return File(uri).renameTo(File(File(uri).parentFile, name))
}

/**
 * 重命名
 */
fun renameFile(context: Context, fileEntity: FileEntity, name: String): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return renameFile(context, fileEntity.uri, name)
    }
    return File(fileEntity.path).renameTo(File(File(fileEntity.path).parentFile, name))
}

/**
 * 分区存储下重命名
 */
@RequiresApi(Build.VERSION_CODES.Q)
fun renameFile(context: Context, uri: String, name: String): Boolean {
    try {
        val resolver = context.contentResolver
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, name)
        return resolver.update(Uri.parse(uri), values, null, null) > 0
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
    return false
}

data class FileEntity(
    var id: Long,
    var name: String,
    var path: String,
    var size: String,
    var time: String,
    var timestamp: Long,
    var uri: String,
    var length: Long,
    var film: String? = null,
)

private fun getMimeType(path: String?): String {
    var mime = "*/*"
    path ?: return mime
    val mmr = MediaMetadataRetriever()
    try {
        mmr.setDataSource(path)
        mime = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: mime
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        mmr.release()
    }
    return mime
}

fun getNewFilePath(context: Context, filePath: String, film: String? = ""): String? {
    val file = File(filePath)
    if (!file.exists()) {
        return null
    }
    val app: String = context.resources.getString(R.string.app_name)
    val dir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        app
    )
    var name = file.name
//    if (!film.isNullOrEmpty() && !DownloadChooser.isSystemFilm(film)) {
//        name = "$film@@$name"
//    }
    return File(dir, name).absolutePath
}