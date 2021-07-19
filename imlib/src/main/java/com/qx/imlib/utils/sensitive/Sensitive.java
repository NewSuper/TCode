package com.qx.imlib.utils.sensitive;



import com.github.stuxuhai.jpinyin.ChineseHelper;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 关键词匹配过滤，替换关键词，判断关键词是否存在，提取关键词
 * 忽略大小写，忽略简体繁体
 * @since	JDK 1.8
 * @author	hechuan
 */
public class Sensitive {

    // 敏感词集合，成员变量，用于算法执行过程中交换数据
    private HashMap<String, HashMap<String, String>> exchangeMap = new HashMap<>();

    /**
     * 设置敏感词集合
     * 注意：
     * 	1. 如果存在多个敏感词，且每个敏感词匹配上后，替换的字符不一样，请使用不同的实例，每个实例设置一个敏感词
     * @param words
     * @author	hechuan
     */
    public Sensitive(Collection<String> words) {
        exchangeMap = new HashMap<>(words.size());
        addSensitiveWords(words);
    }

    public Sensitive(String word) {
        exchangeMap = new HashMap<>(1);
        addSensitiveWord(word);
    }

    public Sensitive() {
        exchangeMap = new HashMap<>();
    }

    public Sensitive setSensitiveWord(String word) {
        addWord(word, true);
        return this;
    }

    public Sensitive setSensitiveWords(Collection<String> words) {
        clearWords();
        for (String word : words) {
            addSensitiveWord(word);
        }
        return this;
    }

    public Sensitive addSensitiveWord(String word) {
        addWord(word, false);
        return this;
    }

    /**
     * 初始化敏感词库，构建DFA算法模型
     * @param sensitiveWordSet 敏感词库
     */
    public Sensitive addSensitiveWords(Collection<String> words) {
        for (String word : words) {
            addSensitiveWord(word);
        }
        return this;
    }

    private void clearWords() {
        this.exchangeMap.clear();
    }

    private void addWord(String word, boolean reset) {
        word = stopWord(word); // 敏感词元数据转小写并简体转繁体，用于匹配时统一输入
        if (reset) {
            exchangeMap.clear();
        }
        Map nowMap = exchangeMap;
        Map<String, String> newWorMap;
        for (int i = 0; i < word.length(); i++) {
            // 转换成char型
            char c = word.charAt(i);
            // 库中获取关键字
            Object wordMap = nowMap.get(c);
            // 如果存在该key，直接赋值，用于下一个循环获取
            if (wordMap != null) {
                nowMap = (Map) wordMap;
            } else {
                // 不存在则，则构建一个map，同时将isEnd设置为0，因为他不是最后一个
                newWorMap = new HashMap<>();
                // 不是最后一个
                newWorMap.put("isEnd", "0");
                nowMap.put(c, newWorMap);
                nowMap = newWorMap;
            }

            if (i == word.length() - 1) {
                // 最后一个
                nowMap.put("isEnd", "1");
            }
        }
    }

    /**
     * 将字符串转小写+繁体转简体
     * @param sensitiveWordSet
     * @return
     * @author	hechuan
     */
    private String stopWord(String txt) {
        String s = txt.toLowerCase(); // 转小写
        s = ChineseHelper.convertToSimplifiedChinese(s);
        return s;
    }

    /**
     * 判断文字是否包含敏感字符
     * @param txt       文字
     * @param matchType 匹配规则 1：最小匹配规则，2：最大匹配规则
     * @return 若包含返回true，否则返回false
     */
    public boolean contains(String txt, MatchType matchType) {
        for (int i = 0; i < txt.length(); i++) {
            if (checkSensitiveWord(txt, i, matchType) > 0) {// 判断是否包含敏感字符。大于0存在，返回true
                return true;
            }
        }
        return false;
    }

    /**
     * 判断文字是否包含敏感字符
     * @param txt 文字
     * @return 若包含返回true，否则返回false
     */
    public boolean contains(String txt) {
        return contains(txt, MatchType.MAX);
    }

    /**
     * 获取敏感词在字符串中的位置列表，用于对原始字符串进行替换
     * @param txt       文字
     * @param matchType 匹配规则 1：最小匹配规则，2：最大匹配规则
     * @return
     */
    private TreeSet<Position> getSensitivePositions(String txt, MatchType matchType) {
        // 降序排列，实现字符串截取时从后向前，避免多次截取发生索引变更导致截取错误
        TreeSet<Position> pisitions = new TreeSet<>(new Comparator<Position>() {

            @Override
            public int compare(Position o1, Position o2) {
                return o2.begin - o1.begin;
            }
        });
        for (int i = 0; i < txt.length(); i++) {
            // 判断是否包含敏感字符
            int length = checkSensitiveWord(txt, i, matchType);
            if (length > 0) {// 存在,加入list中
                Position position = new Position();
                position.begin = i;
                position.end = i + length;
                pisitions.add(position);
                i = i + length - 1;// 减1的原因，是因为for会自增
            }
        }
        return pisitions;
    }

    /**
     * 获取匹配到的敏感词列表
     * @param txt
     * @return
     * @date:	2021年1月27日 下午4:00:35
     * @author	hechuan
     */
    public Set<String> getSensitiveWords(String txt, MatchType matchType) {
        TreeSet<Position> positions = getSensitivePositions(txt, matchType);
        Set<String> set = new HashSet<>();
        for (Position position : positions) { // 根据每个匹配的敏感词的位置，获取敏感词内容
            char[] chars = new char[position.end - position.begin];
            int index = 0;
            for (int i = position.begin; i < position.end; i++) {
                chars[index++] = txt.charAt(i);
            }
            String s = new String(chars);
            set.add(s);
        }

        return set;
    }

    /**
     * 获取匹配到的敏感词
     * @param txt
     * @return
     * @date:	2021年1月27日 下午4:00:35
     * @author	hechuan
     */
    public Set<String> getSensitiveWords(String txt) {
        return getSensitiveWords(txt, MatchType.MAX);
    }

    /**
     * 替换敏感字字符
     * @param txt         文本
     * @param replaceChar 替换的字符，匹配的敏感词以字符逐个替换，如 语句：我爱中国人 敏感词：中国人，替换字符：*， 替换结果：我爱***
     * @param matchType   敏感词匹配规则
     * @return
     */
    public String replaceSensitiveWord(String txt, char replaceChar, MatchType matchType) {
        String s = replaceChar + "";
        return replaceSensitiveWord(txt, s, matchType);
    }

    /**
     * 替换敏感字字符
     * @param txt         文本
     * @param replaceChar 替换的字符，匹配的敏感词以字符逐个替换，如 语句：我爱中国人 敏感词：中国人，替换字符：*， 替换结果：我爱***
     * @return
     */
    public String replaceSensitiveWord(String txt, char replaceChar) {
        return replaceSensitiveWord(txt, replaceChar, MatchType.MAX);
    }

    /**
     * 替换敏感字字符
     * @param txt        文本
     * @param replaceStr 替换的字符串，匹配的敏感词以字符逐个替换，如 语句：我爱中国人 敏感词：中国人，替换字符串：[屏蔽]，替换结果：我爱[屏蔽]
     * @param matchType  敏感词匹配规则
     * @return
     */
    public String replaceSensitiveWord(String txt, String replaceStr, MatchType matchType) {
        String resultTxt = txt;
        // 获取所有的敏感词
        TreeSet<Position> set = getSensitivePositions(txt, matchType);
        Iterator<Position> iterator = set.iterator();
        while (iterator.hasNext()) {
            Position position = iterator.next();
            resultTxt = resultTxt.substring(0, position.begin) + replaceStr + resultTxt.substring(position.end, resultTxt.length());
        }
        return resultTxt;
    }

    /**
     * 替换敏感字字符
     * @param txt        文本
     * @param replaceStr 替换的字符串，匹配的敏感词以字符逐个替换，如 语句：我爱中国人 敏感词：中国人，替换字符串：[屏蔽]，替换结果：我爱[屏蔽]
     * @return
     */
    public String replaceSensitiveWord(String txt, String replaceStr) {
        return replaceSensitiveWord(txt, replaceStr, MatchType.MAX);
    }

    /**
     * 获取替换字符串
     * @param replaceChar
     * @param length
     * @return
     */
    private String getReplaceChars(char replaceChar, int length) {
        String resultReplace = String.valueOf(replaceChar);
        for (int i = 1; i < length; i++) {
            resultReplace += replaceChar;
        }

        return resultReplace;
    }

    /**
     * 检查文字中是否包含敏感字符，检查规则如下：<br>
     * @param txt
     * @param beginIndex
     * @param matchType
     * @return 如果存在，则返回敏感词字符的长度，不存在返回0
     */
    private int checkSensitiveWord(String txt, int beginIndex, MatchType matchType) {
        // 将匹配的字符串转小写，并繁体转简体，保持与init时一致
        String stopTxt = stopWord(txt);
        // 敏感词结束标识位：用于敏感词只有1位的情况
        boolean flag = false;
        // 匹配标识数默认为0
        int matchFlag = 0;
        char word;
        Map nowMap = exchangeMap;
        for (int i = beginIndex; i < stopTxt.length(); i++) {
            word = stopTxt.charAt(i);

            if (SPECIAL_CHARS.contains(word) && matchFlag > 0) {
                matchFlag++;
                continue;
            }

            // 获取指定key
            nowMap = (Map) nowMap.get(word);

            if (nowMap != null) {// 存在，则判断是否为最后一个
                // 找到相应key，匹配标识+1
                matchFlag++;
                // 如果为最后一个匹配规则,结束循环，返回匹配标识数
                if ("1".equals(nowMap.get("isEnd"))) {
                    // 结束标志位为true
                    flag = true;
                    // 最小规则，直接返回,最大规则还需继续查找
                    if (MatchType.MIN == matchType) {
                        break;
                    }
                }
            } else {// 不存在，直接返回
                break;
            }
        }
        if (matchFlag < 2 || !flag) {// 长度必须大于等于1，为词
            matchFlag = 0;
        }
        return matchFlag;
    }

    public static void main(String[] args) {
        replace();
        // exists();
    }

    private static void exists() {
        Set<String> sets = new HashSet<>();
        sets.add("反动");
        sets.add("马云");
        sets.add("馬化腾");
        sets.add("Av");
        Sensitive sensitive = new Sensitive(sets);
        System.out.println(sensitive.contains("他是反{}#%-*+=动反动派馬~~~~~。馬云你SB。马{}云，小，马云。A(v"));
        System.out.println(sensitive.contains("马2化3腾"));
        System.out.println(sensitive.contains("马[[[化]]]腾"));
        System.out.println(sensitive.contains("一起看A*V吧"));
    }

    private static void replace() {
        String origin = "他是反{}#%-*+=动反动派馬~~~~~。馬云你SB。马{}云，小，马云。A(v";
        Map<String, String> map = new HashMap<String, String>();
        map.put("反动", "*");
        map.put("馬云", "@");
        map.put("AV", "&");

        Set<Map.Entry<String, String>> entries = map.entrySet();
        Sensitive sensitive = new Sensitive();
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue();
            origin = sensitive.setSensitiveWord(key).replaceSensitiveWord(origin, value);
        }
        System.out.println(origin);
    }


    /**
     * 特殊字符集合
     * 敏感词匹配忽略的特殊字符，如：
     * 设置了特殊字符包含#$
     * 则在敏感词中间包含相关字符时，匹配会进行skip，如关键词设置了“中国人”，则原始字符串包含“中#$国##$#人”时，也会被替换或命中
     */
    protected static final Set<Character> SPECIAL_CHARS = new HashSet<>();

    static {
//		char[] chars = " ！、＂＃（）《》，「」／【】✔—‖‘’：；“＜”＞？ •\\\"#$%¥&'()*+€-./:<=>@[\\]～^_￥{|}~\"".toCharArray();
        char[] chars = "".toCharArray();
        for (char c : chars) {
            SPECIAL_CHARS.add(c);
        }
    }

    /**
     * 记录关键词匹配命中的每一个段文本在原始字符串中的索引起始位置
     * @since	JDK 1.8
     * @author	hechuan
     */
    class Position {
        private int begin;
        private int end;

        @Override
        public String toString() {
            return "begin=" + begin + ", end=" + end;
        }
    }

    /**
     * 敏感词匹配规则类型
     * @since	JDK 1.8
     * @author	hechuan
     */
    public enum MatchType {
        MIN, // 最小匹配规则，如：敏感词库["中国","中国人"]，语句："我是中国人"，匹配结果：我是[中国]人
        MAX // 最大匹配规则，如：敏感词库["中国","中国人"]，语句："我是中国人"，匹配结果：我是[中国人]
    }

}
