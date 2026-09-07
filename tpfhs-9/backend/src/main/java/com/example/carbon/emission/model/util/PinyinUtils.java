package com.example.carbon.emission.model.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/**
 * 拼音编码生成工具类
 * 
 * 用于根据中文名称自动生成拼音首字母编码
 * 支持英文字母、数字和汉字
 */
public class PinyinUtils {
    
    private static final HanyuPinyinOutputFormat format;
    
    static {
        format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
    }
    
    /**
     * 生成拼音首字母编码
     * 支持英文字母、数字和汉字
     * 
     * @param name 名称
     * @return 拼音首字母编码（大写）
     */
    public static String generatePinyinCode(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        
        StringBuilder pinyinCode = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                pinyinCode.append(c);
            } else if (c >= 'A' && c <= 'Z') {
                pinyinCode.append((char)(c + 32));
            } else if (c >= '0' && c <= '9') {
                pinyinCode.append(c);
            } else if (c >= 0x4E00 && c <= 0x9FA5) {
                try {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        pinyinCode.append(pinyinArray[0].charAt(0));
                    }
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    // 忽略无法转换的字符
                }
            }
        }
        return pinyinCode.toString().toUpperCase();
    }
}
