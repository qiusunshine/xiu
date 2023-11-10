package org.mozilla.xiu.browser.utils;

import android.text.TextUtils;


import androidx.annotation.Nullable;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 作者：By hdy
 * 日期：On 2018/11/12
 * 时间：At 12:03
 */
public class StringUtil {

    public final static String[] LOWER_CASES = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};
    public final static String[] UPPER_CASES = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
    public final static String[] NUMS_LIST = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    public final static String[] SYMBOLS_ARRAY = {"!", "~", "^", "_", "*"};

    public final static String SCHEME_DOWNLOAD = "download://";
    public final static String SCHEME_EDIT_FILE = "editFile://";
    public final static String SCHEME_OPEN_FILE = "openFile://";
    public final static String SCHEME_COPY = "copy://";
    public final static String SCHEME_SELECT = "select://";
    public final static String SCHEME_CONFIRM = "confirm://";
    public final static String SCHEME_INPUT = "input://";
    public final static String SCHEME_WEB = "web://";
    public final static String SCHEME_TOAST = "toast://";
    public final static String SCHEME_SHARE = "share://";
    public final static String SCHEME_FILE_SELECT = "fileSelect://";

    private static final String[] SCHEME_CANT_HANDLE = new String[]{"ftp:", "ed2k:", "magnet:", "thunder:", "xg:"};

    private final static String[] innerScheme = {"http", "hiker", "file", "/", "content", "{", "[", "x5:", SCHEME_FILE_SELECT, SCHEME_TOAST, SCHEME_WEB, SCHEME_INPUT,
            SCHEME_CONFIRM, SCHEME_SELECT, SCHEME_COPY, "x5WebView:", SCHEME_EDIT_FILE, "rtmp:", "rtsp:", "rule:", "code:", "海阔视界", "x5Play:", "func:",
            SCHEME_DOWNLOAD, SCHEME_SHARE, SCHEME_OPEN_FILE, "webview://"};

    public static boolean isCannotHandleScheme(String url) {
        for (String s : SCHEME_CANT_HANDLE) {
            if (url.startsWith(s)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isScheme(String lowUrl) {
        if (lowUrl.startsWith("video://")) {
            return false;
        }
        for (String s : innerScheme) {
            if (lowUrl.startsWith(s)) {
                return false;
            }
        }
        String[] urls = lowUrl.split("://");
        if (urls.length < 2) {
            return false;
        }
        String scheme = urls[0];
        if (scheme.length() > 20) {
            return false;
        }
        return isOnlyEn(scheme);
    }

    public static String genRandomPwd(int pwd_len) {
        return genRandomPwd(pwd_len, false);
    }

    /**
     * 生成随机密码
     *
     * @param pwd_len 密码长度
     * @param simple  简单模式
     * @return 密码的字符串
     */
    public static String genRandomPwd(int pwd_len, boolean simple) {
        if (pwd_len < 2 || pwd_len > 20) {
            return "";
        }
        int lower, upper, num = 0, symbol = 0;
        lower = pwd_len / 2;

        if (simple) {
            upper = pwd_len - lower;
        } else {
            upper = (pwd_len - lower) / 2;
            num = (pwd_len - lower) / 2;
            symbol = pwd_len - lower - upper - num;
        }

        StringBuilder pwd = new StringBuilder();
        Random random = new Random();
        int position = 0;
        while ((lower + upper + num + symbol) > 0) {
            if (lower > 0) {
                position = random.nextInt(pwd.length() + 1);

                pwd.insert(position, LOWER_CASES[random.nextInt(LOWER_CASES.length)]);
                lower--;
            }
            if (upper > 0) {
                position = random.nextInt(pwd.length() + 1);

                pwd.insert(position, UPPER_CASES[random.nextInt(UPPER_CASES.length)]);
                upper--;
            }
            if (num > 0) {
                position = random.nextInt(pwd.length() + 1);

                pwd.insert(position, NUMS_LIST[random.nextInt(NUMS_LIST.length)]);
                num--;
            }
            if (symbol > 0) {
                position = random.nextInt(pwd.length() + 1);

                pwd.insert(position, SYMBOLS_ARRAY[random.nextInt(SYMBOLS_ARRAY.length)]);
                symbol--;
            }

            System.out.println(pwd.toString());
        }
        return pwd.toString();
    }

    public static String arrayToString(String[] list, int fromIndex, String cha) {
        return arrayToString(list, fromIndex, list == null ? 0 : list.length, cha);
    }

    public static String arrayToString(String[] list, int fromIndex, int endIndex, String cha) {
        return StrUtil.INSTANCE.arrayToString(list, fromIndex, endIndex, cha);
    }

    public static String listToString(List<String> list, String cha) {
        return StrUtil.INSTANCE.listToString(list, cha);
    }

    public static String listToString(List<String> list, int fromIndex, String cha) {
        return StrUtil.INSTANCE.listToString(list, fromIndex, cha);
    }

    public static String listToString(List<String> list) {
        return listToString(list, "&&");
    }

    public static String replaceBlank(String str) {
        try {
            String dest = "";
            if (str != null) {
                Pattern p = Pattern.compile("\\s*|\t|\r|\n");
                Matcher m = p.matcher(str);
                dest = m.replaceAll("");
            }
            return dest;
        } catch (Exception e) {
            return str;
        }
    }

    public static String replaceLineBlank(String str) {
        try {
            return str.replaceAll("\n", "");
        } catch (Exception e) {
            return str;
        }
    }

    public static String trimBlanks(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        int len = str.length();
        int st = 0;

        while ((st < len) && (str.charAt(st) == '\n' || str.charAt(st) == '\r' || str.charAt(st) == '\f' || str.charAt(st) == '\t')) {
            st++;
        }
        while ((st < len) && (str.charAt(len - 1) == '\n' || str.charAt(len - 1) == '\r' || str.charAt(len - 1) == '\f' || str.charAt(len - 1) == '\t')) {
            len--;
        }
        return ((st > 0) || (len < str.length())) ? str.substring(st, len) : str;
    }

    public static String trimAll(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        int len = str.length();
        int st = 0;
        //中文空格
        while ((st < len) && (str.charAt(st) == '\n' || str.charAt(st) == '\r' || str.charAt(st) == '\f' || str.charAt(st) == '\t' || str.charAt(st) == '　' || str.charAt(st) == ' ')) {
            st++;
        }
        while ((st < len) && (str.charAt(len - 1) == '\n' || str.charAt(len - 1) == '\r' || str.charAt(len - 1) == '\f' || str.charAt(len - 1) == '\t' || str.charAt(len - 1) == '　' || str.charAt(len - 1) == ' ')) {
            len--;
        }
        return ((st > 0) || (len < str.length())) ? str.substring(st, len) : str;
    }

    public static String clearLine(String str) {
        String[] s = str.split("\n");
        int st = 0;
        for (int i = 0; i < s.length; i++) {
            if (i == 0) {
                while ((st < s[0].length()) && str.charAt(st) == ' ') {
                    st++;
                }
            }
            if (st > 0 && st < s[i].length()) {
                s[i] = s[i].substring(st);
            }
        }
        return arrayToString(s, 0, "\n");
    }

    public static boolean equalsDomUrl(String url1, String url2) {
        if (url1 == null) {
            return url2 == null;
        }
        if (url2 == null) {
            return false;
        }
        String pUrl = url1;
        if (pUrl.endsWith("/")) {
            pUrl = pUrl.substring(0, pUrl.length() - 1);
        }
        String sUrl = url2;
        if (sUrl.endsWith("/")) {
            sUrl = sUrl.substring(0, sUrl.length() - 1);
        }
        return pUrl.equals(sUrl);
    }

    public static String getHomeUrl(String url) {
        return getHome(url);
    }


    public static boolean isHexStr(String str) {
        boolean flag = false;
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        if (!str.startsWith("#")) {
            str = "#" + str;
        }
        if (str.length() != 7 && str.length() != 9) {
            return false;
        }
        for (int i = 1; i < str.length(); i++) {
            char cc = str.charAt(i);
            if (cc == '0' || cc == '1' || cc == '2' || cc == '3' || cc == '4' || cc == '5' || cc == '6' || cc == '7' || cc == '8' || cc == '9' || cc == 'A' || cc == 'B' || cc == 'C' ||
                    cc == 'D' || cc == 'E' || cc == 'F' || cc == 'a' || cc == 'b' || cc == 'c' || cc == 'd' || cc == 'e' || cc == 'f') {
                flag = true;
            }
        }
        return flag;
    }

    // 判断一个字符是否是中文
    private static boolean isChinese(char c) {
        return c >= 0x4E00 && c <= 0x9FA5;// 根据字节码判断
    }

    // 判断一个字符串是否含有中文
    public static boolean containsChinese(String str) {
        if (str == null)
            return false;
        for (char c : str.toCharArray()) {
            if (isChinese(c))
                return true;
        }
        return false;
    }

    // 判断一个字符串是否全是中文
    public static boolean isAllChinese(String str) {
        if (str == null) {
            return true;
        }
        for (char c : str.toCharArray()) {
            if (!isChinese(c)) {
                return false;
            }
        }
        return true;
    }

    public static String decodeConflictStr(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.replace("？？", "?").replace("＆＆", "&").replace("；；", ";");
    }

    public static boolean isUrl(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        if (isWebUrl(str)) {
            return true;
        }
        return !containsChinese(str) && str.contains(".") && !str.contains(" ");
    }

    public static String getRealUrl(String pageUrl, String url) {
        url = getRealUrl(url);
        if (StringUtil.isEmpty(url) || !url.startsWith("http")) {
            return pageUrl;
        }
        return url;
    }

    public static String getRealUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        try {
            String maxUrl = getMaxItem(url, "\\$\\$\\$");
            if (isNotEmpty(maxUrl)) {
                return maxUrl;
            }
            maxUrl = getMaxItem(url, "\\$\\$");
            if (isNotEmpty(maxUrl)) {
                return maxUrl;
            }
            maxUrl = getMaxItem(url, "##");
            if (isNotEmpty(maxUrl)) {
                return maxUrl;
            }
            return url;
        } catch (Exception e) {
            e.printStackTrace();
            return url;
        }
    }

    private static String getMaxItem(String url, String sep) {
        String[] a = url.split(sep);
        String maxUrl = null;
        int max = 0;
        for (String s : a) {
            if (s.startsWith("http")) {
                if (s.length() > max) {
                    max = s.length();
                    maxUrl = s;
                }
            }
        }
        return maxUrl;
    }

    public static String getDom(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        return getDom0(getRealUrl(url));
    }

    private static String getDom0(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        if (url.startsWith("thunder://") || url.startsWith("magnet:") || url.startsWith("ftp://") || url.startsWith("ed2k:") || url.startsWith("file://")) {
            return url;
        }
        try {
            String[] s = url.split("://");
            if (s.length > 1) {
                return s[1].split("/")[0];
            }
            String[] s2 = url.split("//");
            if (s2.length > 1) {
                return s2[1].split("/")[0];
            }
            url = url.replaceFirst("http://", "").replaceFirst("https://", "");
            String[] urls = url.split("/");
            if (urls.length > 0) {
                return urls[0];
            }
        } catch (Exception e) {
            return null;
        }
        return url;
    }

    public static String getSecondDom(String url) {
        if (isEmpty(url)) {
            return url;
        }
        String dom = getDom(url);
        String[] doms = dom.split("\\.");
        if (doms.length >= 3) {
            //二级域名
            return StringUtil.arrayToString(doms, doms.length - 2, ".");
        }
        return dom;
    }

    public static String getHome(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        url = getRealUrl(url);
        String dom = getDom0(url);
        return url.split("://")[0] + "://" + dom;
    }

    public static String removeDom(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        try {
            url = url.replaceFirst("http://", "").replaceFirst("https://", "");
            String[] urls = url.split("/");
            if (urls.length > 1) {
                return arrayToString(urls, 1, "/");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }

    public static String getSimpleDom(String url) {
        String dom = getDom(url);
        if (StringUtil.isEmpty(url) || StringUtil.isEmpty(dom) || url.equals(dom)) {
            return url;
        }
        String[] s = dom.split("\\.");
        if (s.length < 3) {
            return dom;
        }
        return dom.substring(dom.indexOf(".", s.length - 2) + 1);
    }

    /**
     * 多关键字查询表红,避免后面的关键字成为特殊的HTML语言代码
     *
     * @param str    检索结果
     * @param inputs 关键字集合
     * @param resStr 表红后的结果
     */
    public static StringBuilder spannableString(String str, List<String> inputs, StringBuilder resStr) {
        int index = str.length();//用来做为标识,判断关键字的下标
        String next = "";//保存str中最先找到的关键字
        for (int i = inputs.size() - 1; i >= 0; i--) {
            String theNext = inputs.get(i);
            int theIndex = str.indexOf(theNext);
            if (theIndex == -1) {//过滤掉无效关键字
                inputs.remove(i);
            } else if (theIndex < index) {
                index = theIndex;//替换下标
                next = theNext;
            }
        }
        //如果条件成立,表示串中已经没有可以被替换的关键字,否则递归处理
        if (index == str.length()) {
            resStr.append(str);
        } else {
            resStr.append(str.substring(0, index));
            resStr.append("<font color='#FF0000'>").append(str.substring(index, index + next.length())).append("</font>");
            String str1 = str.substring(index + next.length(), str.length());
            spannableString(str1, inputs, resStr);//剩余的字符串继续替换
        }
        return resStr;
    }

    /**
     * 转义正则特殊字符 （$()*+.[]?\^{},|）
     *
     * @param keyword
     * @return keyword
     */
    public static String escapeExprSpecialWord(String keyword) {
        if (!TextUtils.isEmpty(keyword)) {
            String[] fbsArr = {"\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|"};
            for (String key : fbsArr) {
                if (keyword.contains(key)) {
                    keyword = keyword.replace(key, "\\" + key);
                }
            }
        }
        return keyword;
    }

    /**
     * 删除正则特殊字符 （$()*+.[]?\^{},|）
     *
     * @param keyword
     * @return keyword
     */

    public static String removeSpecialWord(String keyword) {
        if (!TextUtils.isEmpty(keyword)) {
            String[] fbsArr = {"\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|"};
            for (String key : fbsArr) {
                if (keyword.contains(key)) {
                    keyword = keyword.replace(key, "");
                }
            }
        }
        return keyword;
    }

    private static Pattern FilePattern = Pattern.compile("[\\\\/:*?\"<>|]");

    public static String filenameFilter(String str) {
        if (StringUtil.isNotEmpty(str) && str.startsWith("http")) {
            return md5(str);
        }
        String s = str == null ? null : FilePattern.matcher(str).replaceAll("_")
                .replace(File.separator, "_")
                .replace("...", "_")
                .replace("\n", "_")
                .replace("\r", "_");
        return s == null ? "" : (s.length() > 85 ? s.substring(0, 42) + "-" + s.substring(s.length() - 42) : s);
    }

    public static String getBaseUrl(String url) {
        if (StringUtil.isEmpty(url)) {
            return url;
        }
        String baseUrls = url.replace("http://", "").replace("https://", "");
        String baseUrl2 = baseUrls.split("/")[0];
        String baseUrl;
        if (url.startsWith("https")) {
            baseUrl = "https://" + baseUrl2;
        } else {
            baseUrl = "http://" + baseUrl2;
        }
        return baseUrl;
    }

    public static boolean isEmpty(@Nullable CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static boolean isNotEmpty(@Nullable CharSequence str) {
        return !isEmpty(str);
    }

    public static boolean isUTF8(String str) {
        try {
            str.getBytes("utf-8");
            return true;
        } catch (UnsupportedEncodingException e) {
            return false;
        }

    }


    public static String convertBlankToTagP(String content) {
        try {
            if (StringUtil.isEmpty(content)) {
                return content;
            } else if (!content.contains("\n")) {
                return content;
            } else {
                return content.replace("\n", "<br>");
            }
        } catch (Exception e) {
            return content;
        }
    }


    public static String simplyGroup(String title) {
        if (isEmpty(title)) {
            return title;
        }
        return StrUtil.INSTANCE.simplyGroup(title);
    }

    /**
     * 判读是否是emoji
     *
     * @param codePoint
     * @return
     */
    public static boolean getIsEmoji(char codePoint) {
        return StrUtil.INSTANCE.getIsEmoji(codePoint);
    }


    public static boolean getIsSp(char codePoint) {
        return Character.getType(codePoint) > Character.LETTER_NUMBER;
    }

    /**
     * 判断搜索框内容是否包含特殊字符
     *
     * @param str
     * @return
     */
    public static boolean hasSpWord(String str) {
        String limitEx = "[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@①#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Pattern pattern = Pattern.compile(limitEx);
        Matcher m = pattern.matcher(str);
        return m.find();
    }

    /**
     * 判断是否只含英文数字
     *
     * @param str
     * @return
     */
    public static boolean isLetterDigit(String str) {
        String regex = "^[a-z0-9A-Z\\-._]+$";
        return str.matches(regex);
    }


    public static boolean isWebUrl(String str) {
        if (isEmpty(str)) {
            return false;
        }
        if (!isEmpty(str) && (str.startsWith("http://") || str.startsWith("https://")) && !str.contains(" ")) {
            return true;
        }
        String url = str.toLowerCase();
        return url.startsWith("http") || url.startsWith("file://") || url.startsWith("ftp")
                || url.startsWith("rtmp://") || url.startsWith("rtsp://") || url.startsWith("magnet:?") || url.startsWith("thunder:");
    }

    /**
     * 判断是否只含英文字母
     *
     * @param str
     * @return
     */
    public static boolean isOnlyEn(String str) {
        String regex = "^[a-zA-Z]+$";
        return str.matches(regex);
    }

    public static String autoFixUrl(String bUrl, String url) {
        if (isEmpty(bUrl) || isEmpty(url)) {
            return url;
        }
        bUrl = bUrl.split(";")[0];
        String baseUrl = getBaseUrl(bUrl);
        String lowUrl = url.toLowerCase();
        if (lowUrl.startsWith("http") || lowUrl.startsWith("hiker") || lowUrl.startsWith("pics") || lowUrl.startsWith("code")) {
            return url;
        } else if (url.startsWith("//")) {
            return "http:" + url;
        } else if (url.startsWith("magnet") || url.startsWith("thunder") || url.startsWith("ftp") || url.startsWith("ed2k")) {
            return url;
        } else if (url.startsWith("/")) {
            if (baseUrl.endsWith("/")) {
                return baseUrl.substring(0, baseUrl.length() - 1) + url;
            } else {
                return baseUrl + url;
            }
        } else if (url.startsWith("./")) {
            String[] protocolUrl = bUrl.split("://");
            if (protocolUrl.length < 1) {
                return url;
            }
            String[] c = protocolUrl[1].split("/");
            if (c.length <= 1) {
                if (baseUrl.endsWith("/")) {
                    return baseUrl.substring(0, baseUrl.length() - 1) + url.replace("./", "");
                } else {
                    return baseUrl + url.replace("./", "");
                }
            }
            String sub = protocolUrl[1].replace(c[c.length - 1], "");
            return protocolUrl[0] + "://" + sub + url.replace("./", "");
        } else if (url.startsWith("?")) {
            return bUrl + url;
        } else {
            return url;
        }
    }

    public static String[] splitUrlByQuestionMark(String url) {
        if (isEmpty(url)) {
            return new String[]{url};
        } else {
            String[] urls = url.split("\\?");
            if (urls.length <= 1) {
                return urls;
            } else {
                String[] res = new String[2];
                res[0] = urls[0];
                res[1] = arrayToString(urls, 1, "?");
                return res;
            }
        }
    }

    public static byte[] hexToBytes(String hex) {
        if (hex.length() < 1) {
            return null;
        } else {
            byte[] result = new byte[hex.length() / 2];
            int j = 0;
            for (int i = 0; i < hex.length(); i += 2) {
                result[j++] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
            }
            return result;
        }
    }

    public static String md5(String source) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(source.getBytes());
            return convertByte2String(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return source;
    }

    private static String convertByte2String(byte[] byteResult) {
        char[] hexDigits =
                {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        // 4位代表一个16进制，所以长度需要变为原来2倍
        char[] result = new char[byteResult.length * 2];
        int index = 0;
        for (byte b : byteResult) {
            // 先转换高4位
            result[index++] = hexDigits[(b >>> 4) & 0xf];
            result[index++] = hexDigits[b & 0xf];
        }
        return new String(result);
    }


    public static boolean searchContains(String text, String key, boolean ignoreCase) {
        if (isEmpty(text)) return false;
        if (isEmpty(key)) return true;
        if (ignoreCase) {
            text = text.toLowerCase();
            key = key.toLowerCase();
        }
        String[] tmp2 = key.split(" ");
        if (tmp2.length >= 2) {
            String u = text;
            for (String s : tmp2) {
                int i = u.indexOf(s);
                if (i < 0) {
                    return false;
                }
                u = u.substring(i + s.length());
            }
            return true;
        } else {
            return text.contains(key);
        }
    }


    public static String replaceContains(String text, String key,
                                         String wrapStart, String wrapEnd,
                                         String wrapStart2, String wrapEnd2) {
        if (isEmpty(text)) return text;
        if (isEmpty(key)) return text;
        String[] tmp2 = key.split(" ");
        if (tmp2.length >= 1) {
            String u = text;
            List<String> list = new ArrayList<>();
            for (String s : tmp2) {
                int i = u.indexOf(s);
                if (i < 0) {
                    break;
                }
                String pre = u.substring(0, i);
                if (isNotEmpty(wrapStart2)) {
                    list.add(wrapStart2);
                }
                list.add(pre);
                if (isNotEmpty(wrapEnd2)) {
                    list.add(wrapEnd2);
                }
                list.add(wrapStart);
                list.add(s);
                list.add(wrapEnd);
                u = u.substring(i + s.length());
            }
            if (isNotEmpty(u)) {
                if (isNotEmpty(wrapStart2)) {
                    list.add(wrapStart2);
                }
                list.add(u);
                if (isNotEmpty(wrapEnd2)) {
                    list.add(wrapEnd2);
                }
            }
            return CollectionUtil.listToString(list, "");
        } else {
            return text;
        }
    }

    public static String keepNums(String text) {
        if (isEmpty(text)) return text;
        return text.replaceAll("[^\\d]", "");
    }

    /**
     * 莱文斯坦距离，又称 Levenshtein 距离，是编辑距离的一种。指两个字串之间，由一个转成另一个所需的最少编辑操作次数。
     *
     * @param a
     * @param b
     * @return
     */
    public static float levenshtein(String a, String b) {
        if (a == null && b == null) {
            return 1f;
        }
        if (a == null || b == null) {
            return 0F;
        }
        int editDistance = editDis(a, b);
        return 1 - ((float) editDistance / Math.max(a.length(), b.length()));
    }

    private static int editDis(String a, String b) {

        int aLen = a.length();
        int bLen = b.length();

        if (aLen == 0) return aLen;
        if (bLen == 0) return bLen;

        int[][] v = new int[aLen + 1][bLen + 1];
        for (int i = 0; i <= aLen; ++i) {
            for (int j = 0; j <= bLen; ++j) {
                if (i == 0) {
                    v[i][j] = j;
                } else if (j == 0) {
                    v[i][j] = i;
                } else if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    v[i][j] = v[i - 1][j - 1];
                } else {
                    v[i][j] = 1 + Math.min(v[i - 1][j - 1], Math.min(v[i][j - 1], v[i - 1][j]));
                }
            }
        }
        return v[aLen][bLen];
    }
}
