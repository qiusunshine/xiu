package org.mozilla.xiu.browser.utils;

/**
 * 作者：By hdy
 * 日期：On 2017/11/6
 * 时间：At 17:01
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Base64;
import android.widget.ImageView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 主要功能：用于存储缓存数据
 *
 * @Prject: CommonUtilLibrary
 * @Package: com.jingewenku.abrahamcaijin.commonutil
 * @author: AbrahamCaiJin
 * @date: 2017年05月04日 14:13
 * @Copyright: 个人版权所有
 * @Company:
 * @version: 1.0.0
 */
public class PreferenceMgr2 {
    /**
     * 保存在手机里面的文件名
     */
    public static String SETTING_CONFIG = "setting_config";

    public static Map<String, ?> all(Context context, String fileName) {
        SharedPreferences sp = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        return sp.getAll();
    }

    /**
     * 保存数据的方法，我们需要拿到保存数据的具体类型，然后根据类型调用不同的保存方法
     */
    public static void put(Context context, String key, Object object) {
        put(context, getFileName(context), key, object);
    }

    private static String getFileName(Context context) {
        return context.getPackageName() + "_preferences";
    }

    public static void put(Context context, String fileName, String key, Object object) {

        SharedPreferences sp = context.getSharedPreferences(fileName,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        if (object instanceof String) {
            editor.putString(key, (String) object);
        } else if (object instanceof Integer) {
            editor.putInt(key, (Integer) object);
        } else if (object instanceof Boolean) {
            editor.putBoolean(key, (Boolean) object);
        } else if (object instanceof Float) {
            editor.putFloat(key, (Float) object);
        } else if (object instanceof Long) {
            editor.putLong(key, (Long) object);
        } else if (object instanceof Collection) {
            editor.putStringSet(key, new HashSet<String>((Collection) object));
        } else {
            editor.putString(key, object.toString());
        }
        SharedPreferencesCompat.apply(editor);
    }

    /**
     * 得到保存数据的方法，我们根据默认值得到保存的数据的具体类型，然后调用相对于的方法获取值
     */
    public static Object get(Context context, String key, Object defaultObject) {
        return get(context, getFileName(context), key, defaultObject);
    }

    public static Object get(Context context, String fileName, String key, Object defaultObject) {
        SharedPreferences sp = context.getSharedPreferences(fileName,
                Context.MODE_PRIVATE);
        if (defaultObject instanceof String) {
            return sp.getString(key, (String) defaultObject);
        } else if (defaultObject instanceof Integer) {
            return sp.getInt(key, (Integer) defaultObject);
        } else if (defaultObject instanceof Boolean) {
            return sp.getBoolean(key, (Boolean) defaultObject);
        } else if (defaultObject instanceof Float) {
            return sp.getFloat(key, (Float) defaultObject);
        } else if (defaultObject instanceof Long) {
            return sp.getLong(key, (Long) defaultObject);
        } else if (defaultObject instanceof Set) {
            return sp.getStringSet(key, (Set<String>) defaultObject);
        }
        return null;
    }

    public static String getString(Context context, String key, String defaultObject) {
        return getString(context, getFileName(context), key, defaultObject);
    }

    public static String getString(Context context, String fileName, String key, String defaultObject) {
        return context.getSharedPreferences(fileName, Context.MODE_PRIVATE).getString(key, defaultObject);
    }

    public static int getInt(Context context, String key, int defaultObject) {
        try {
            return getInt(context, getFileName(context), key, defaultObject);
        } catch (ClassCastException e) {
            return defaultObject;
        }
    }

    public static int getInt(Context context, String fileName, String key, int defaultObject) {
        try {
            return context.getSharedPreferences(fileName, Context.MODE_PRIVATE).getInt(key, defaultObject);
        } catch (ClassCastException e) {
            return defaultObject;
        }
    }

    public static boolean getBoolean(Context context, String key, boolean defaultObject) {
        return getBoolean(context, getFileName(context), key, defaultObject);
    }

    public static boolean getBoolean(Context context, String fileName, String key, boolean defaultObject) {
        return context.getSharedPreferences(fileName, Context.MODE_PRIVATE).getBoolean(key, defaultObject);
    }

    public static float getFloat(Context context, String key, float defaultObject) {
        try {
            return getFloat(context, getFileName(context), key, defaultObject);
        } catch (ClassCastException e) {
            float real = getInt(context, getFileName(context), key, (int) defaultObject) + 0f;
            put(context, getFileName(context), key, real);
            return real;
        }
    }

    public static float getFloat(Context context, String fileName, String key, float defaultObject) {
        try {
            return context.getSharedPreferences(fileName, Context.MODE_PRIVATE).getFloat(key, defaultObject);
        } catch (ClassCastException e) {
            float real = getInt(context, fileName, key, (int) defaultObject) + 0f;
            put(context, fileName, key, real);
            return real;
        }
    }

    public static long getLong(Context context, String key, long defaultObject) {
        try {
            return getLong(context, getFileName(context), key, defaultObject);
        } catch (ClassCastException e) {
            long real = (long) getInt(context, getFileName(context), key, (int) defaultObject);
            put(context, getFileName(context), key, real);
            return real;
        }
    }

    public static long getLong(Context context, String fileName, String key, long defaultObject) {
        try {
            return context.getSharedPreferences(fileName, Context.MODE_PRIVATE).getLong(key, defaultObject);
        } catch (ClassCastException e) {
            long real = (long) getInt(context, fileName, key, (int) defaultObject);
            put(context, fileName, key, real);
            return real;
        }
    }

    /**
     * 移除某个key值已经对应的值
     */
    public static void remove(Context context, String key) {
        SharedPreferences sp = context.getSharedPreferences(getFileName(context),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(key);
        SharedPreferencesCompat.apply(editor);
    }

    /**
     * 移除某个key值已经对应的值
     */
    public static void remove(Context context, String file, String key) {
        SharedPreferences sp = context.getSharedPreferences(file,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(key);
        SharedPreferencesCompat.apply(editor);
    }

    /**
     * 清除所有数据
     */
    public static void clear(Context context) {
        SharedPreferences sp = context.getSharedPreferences(getFileName(context),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        SharedPreferencesCompat.apply(editor);
    }

    /**
     * 查询某个key是否已经存在
     */
    public static boolean contains(Context context, String key) {
        SharedPreferences sp = context.getSharedPreferences(getFileName(context),
                Context.MODE_PRIVATE);
        return sp.contains(key);
    }

    /**
     * 查询某个key是否已经存在
     */
    public static boolean contains(Context context, String file, String key) {
        SharedPreferences sp = context.getSharedPreferences(file,
                Context.MODE_PRIVATE);
        return sp.contains(key);
    }

    /**
     * 返回所有的键值对
     */
    public static Map<String, ?> getAll(Context context) {
        SharedPreferences sp = context.getSharedPreferences(getFileName(context),
                Context.MODE_PRIVATE);
        return sp.getAll();
    }


    /**
     * 保存图片到SharedPreferences
     *
     * @param mContext
     * @param imageView
     */
    public static void putImage(Context mContext, String key, ImageView imageView) {
        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        // 将Bitmap压缩成字节数组输出流
        ByteArrayOutputStream byStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, byStream);
        // 利用Base64将我们的字节数组输出流转换成String
        byte[] byteArray = byStream.toByteArray();
        String imgString = new String(Base64.encodeToString(byteArray, Base64.NO_WRAP));
        // 将String保存shareUtils
        PreferenceMgr2.put(mContext, key, imgString);
    }

    /**
     * 从SharedPreferences读取图片
     *
     * @param mContext
     * @param imageView
     */
    public static Bitmap getImage(Context mContext, String key, ImageView imageView) {
        String imgString = (String) PreferenceMgr2.get(mContext, key, "");
        if (!imgString.equals("")) {
            // 利用Base64将我们string转换
            byte[] byteArray = Base64.decode(imgString, Base64.NO_WRAP);
            ByteArrayInputStream byStream = new ByteArrayInputStream(byteArray);
            // 生成bitmap
            return BitmapFactory.decodeStream(byStream);
        }
        return null;
    }

    /**
     * 创建一个解决SharedPreferencesCompat.apply方法的一个兼容类
     */
    private static class SharedPreferencesCompat {
        private static final Method sApplyMethod = findApplyMethod();

        /**
         * 反射查找apply的方法
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        private static Method findApplyMethod() {
            try {
                Class clz = SharedPreferences.Editor.class;
                return clz.getMethod("apply");
            } catch (NoSuchMethodException e) {
            }

            return null;
        }

        /**
         * 如果找到则使用apply执行，否则使用commit
         */
        public static void apply(SharedPreferences.Editor editor) {
            try {
                if (sApplyMethod != null) {
                    sApplyMethod.invoke(editor);
                    return;
                }
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
            editor.commit();
        }
    }

    public static void registerOnSharedPreferenceChangeListener(Context context, SharedPreferences.OnSharedPreferenceChangeListener listener) {
        context.getSharedPreferences(getFileName(context), Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(listener);
    }

    public static void registerOnSharedPreferenceChangeListener(Context context, String filename, SharedPreferences.OnSharedPreferenceChangeListener listener) {
        context.getSharedPreferences(filename, Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(listener);
    }
}