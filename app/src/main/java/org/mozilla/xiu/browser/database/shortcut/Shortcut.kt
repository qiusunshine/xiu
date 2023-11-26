package org.mozilla.xiu.browser.database.shortcut

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Shortcut(
    @field:ColumnInfo(name = "url_info") var url: String,
    @field:ColumnInfo(name = "title_info") var title: String,
    @field:ColumnInfo(name = "time_info") var time: Int,
    @field:ColumnInfo(name = "icon", defaultValue = "") var icon: String = "",
) {
    @PrimaryKey(autoGenerate = true)
    var id = 0

    @ColumnInfo(name = "mix")
    var mix: String = url + title

}