package org.mozilla.xiu.browser.database.history

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class History(
    @field:ColumnInfo(name = "url_info") var url: String, @field:ColumnInfo(
        name = "title_info"
    ) var title: String, @field:ColumnInfo(name = "time_info") var time: Int
) {
    @PrimaryKey(autoGenerate = true)
    var id = 0

    @ColumnInfo(name = "mix")
    var mix: String = url + title

}