package org.mozilla.geckoview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import org.mozilla.xiu.browser.utils.DisplayUtil
import org.mozilla.xiu.browser.utils.ScreenUtil

/**
 * 作者：By 15968
 * 日期：On 2023/11/14
 * 时间：At 13:17
 */

fun GeckoSession.loadAppBarColors(
    context: Context,
    checkBottom: Boolean,
    defaultColor: Int,
    callback: (arr: IntArray) -> Unit
) {
    this.capture(context)
        .accept({ bitmap ->
            val colors = IntArray(2)
            colors[0] = defaultColor
            colors[1] = defaultColor
            if (bitmap != null) {
                try {
                    //最上面20个像素
                    val map: MutableMap<Int, Int> = HashMap()
                    for (i in 0 until bitmap.width) {
                        for (j in 0 until Math.min(20, bitmap.height)) {
                            val color = bitmap.getPixel(i, j)
                            var count = map[color]
                            if (count == null) {
                                count = 0
                            }
                            count += 1
                            map[color] = count
                        }
                    }
                    val topColor: Int = getMostColor(map, defaultColor)
                    colors[0] = topColor
                    if (checkBottom) {
                        //底部20个像素
                        val map2: MutableMap<Int, Int> = HashMap()
                        for (i in 0 until bitmap.width) {
                            for (j in bitmap.height - 1 downTo Math.max(0, bitmap.height - 20)) {
                                val color = bitmap.getPixel(i, j)
                                var count = map2[color]
                                if (count == null) {
                                    count = 0
                                }
                                count += 1
                                map2[color] = count
                            }
                        }
                        val bottomColor: Int = getMostColor(map2, defaultColor)
                        colors[1] = bottomColor
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            callback(colors)
        }, { e ->
            e?.printStackTrace()
            val colors = IntArray(2)
            colors[0] = defaultColor
            colors[1] = defaultColor
            callback(colors)
        })
}

private fun getMostColor(map: Map<Int, Int>, defaultColor: Int): Int {
    val keys: List<Int> = ArrayList(map.keys)
    var max = 0
    var color = defaultColor
    for (key in keys) {
        if (max < map[key]!!) {
            max = map[key]!!
            color = key
        }
    }
    return color
}

fun GeckoSession.capture(context: Context): GeckoResult<Bitmap> {
    val result = GeckoResult<Bitmap>()
    val rect = Rect()
    this.getSurfaceBounds(rect)
    val mSrcWidth = rect.width()
    val mSrcHeight = rect.height()
    val p = if (mSrcWidth > 0) mSrcHeight.toFloat() / mSrcWidth else 320f / 180
    var mOutHeight: Int = DisplayUtil.dp2px(context, 320f)
    var mOutWidth = (mOutHeight / p).toInt()

    if (mSrcWidth <= 0 && ScreenUtil.isOrientationLand(context)) {
        //横屏模式
        val temp = mOutWidth
        mOutWidth = mOutHeight
        mOutHeight = temp
    }
    val target = try {
        Bitmap.createBitmap(mOutWidth, mOutHeight, Bitmap.Config.ARGB_8888)
    } catch (e: Throwable) {
        return if (e is NullPointerException || e is OutOfMemoryError) {
            GeckoResult.fromException(
                OutOfMemoryError("Not enough memory to allocate for bitmap")
            )
        } else GeckoResult.fromException(Throwable("Failed to create bitmap", e))
    }
    this.mCompositor.requestScreenPixels(
        result, target, 0, 0, mSrcWidth, mSrcHeight, mOutWidth, mOutHeight
    )
    return result
}