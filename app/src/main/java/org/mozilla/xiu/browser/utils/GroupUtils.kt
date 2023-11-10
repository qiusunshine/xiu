package org.mozilla.xiu.browser.utils

import org.mozilla.xiu.browser.database.bookmark.Bookmark
import org.mozilla.xiu.browser.database.history.History

class GroupUtils{
    var list:List<Bookmark?>
    private lateinit var list2:List<History?>
    private var mList= ArrayList<Bookmark>()
    private var group: String? = null
    var tags= ArrayList<String>()

    constructor(list: List<Bookmark?>) {
        this.list = list
        if(!list.isNullOrEmpty())
            retrieval()

    }

    fun groupBookmark():List<Bookmark?>{
        return mList.toList()
    }
    fun groupTagBookmark():ArrayList<String>{
        return tags
    }
    fun groupHistory():List<History?>{
        return list2
    }

    fun retrieval(){
        group= list[0]?.file
        group?.let { tags.add(it) }
        for (i in list)
        {
            if (i != null) {
                if (i.file==group)
                    mList.add(i)
                if (list.lastIndexOf(i)==list.size-1){
                    list=list.toMutableList().apply { removeAll(mList) }
                    if (list.isNotEmpty())
                        retrieval()
                }
            }
        }
    }
}