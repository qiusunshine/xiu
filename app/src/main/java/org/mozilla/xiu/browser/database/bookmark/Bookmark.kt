package org.mozilla.xiu.browser.database.bookmark

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
class Bookmark(
    @field:ColumnInfo(name = "url_info") var url: String = "",
    @field:ColumnInfo(name = "title_info") var title: String,
    @field:ColumnInfo(name = "file_name") var file: String = "",
    @field:ColumnInfo(name = "show_info") var show: Boolean = true,
    @field:ColumnInfo(name = "parentId", defaultValue = "0") var parentId: Int = 0,
    @field:ColumnInfo(name = "icon", defaultValue = "") var icon: String = "",
    @field:ColumnInfo(name = "dir", defaultValue = "0") var dir: Int = 0
) {
    @PrimaryKey(autoGenerate = true)
    var id = 0

    @ColumnInfo(name = "mix")
    var mix: String = url + title

    @Ignore
    var parent: Bookmark? = null

    @Ignore
    var group: String? = null

    @Ignore
    fun isDir(): Boolean {
        return dir > 0
    }

    @Ignore
    fun setDir(dir: Boolean) {
        this.dir = if (dir) 1 else 0
    }
}