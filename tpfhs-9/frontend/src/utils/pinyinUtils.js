/**
 * 拼音编码生成工具
 * 
 * 用于根据中文名称自动生成拼音首字母编码
 * 支持英文字母、数字和汉字
 * 
 * 使用 pinyin-pro 库处理汉字拼音转换（准确率 99.846%）
 */

import { pinyin } from 'pinyin-pro'

/**
 * 根据名称生成拼音首字母编码
 * @param {string} name 中文名称
 * @returns {string} 拼音首字母编码（大写）
 */
export function generatePinyinCode(name) {
    if (!name || name.length === 0) {
        return ""
    }
    
    let result = ""
    
    // 分离汉字和非汉字字符
    let chineseChars = ''
    let nonChineseMap = {} // 记录非汉字字符的位置和值
    
    for (let i = 0; i < name.length; i++) {
        const c = name[i]
        if (isChineseChar(c)) {
            chineseChars += c
        } else {
            nonChineseMap[i] = c
        }
    }
    
    // 如果没有汉字，直接转换
    if (!chineseChars) {
        for (const c of name) {
            if (c >= 'A' && c <= 'Z') {
                result += String.fromCharCode(c.charCodeAt(0) + 32)
            } else {
                result += c
            }
        }
        return result.toUpperCase()
    }
    
    // 使用 pinyin-pro 批量获取汉字首字母
    let initials = []
    try {
        initials = pinyin(chineseChars, { pattern: 'first', type: 'array' })
    } catch (e) {
        // 逐字处理作为降级方案
        initials = []
        for (const c of chineseChars) {
            try {
                initials.push(pinyin(c, { pattern: 'first' }))
            } catch (err) {
                initials.push('')
            }
        }
    }
    
    // 按原顺序组装结果
    let chineseIdx = 0
    for (let i = 0; i < name.length; i++) {
        if (nonChineseMap[i]) {
            const c = nonChineseMap[i]
            if (c >= 'A' && c <= 'Z') {
                result += String.fromCharCode(c.charCodeAt(0) + 32)
            } else {
                result += c
            }
        } else {
            result += initials[chineseIdx] || ''
            chineseIdx++
        }
    }
    
    return result.toUpperCase()
}

/**
 * 判断是否为中文字符（包括扩展区）
 */
function isChineseChar(c) {
    const code = c.charCodeAt(0)
    // CJK统一汉字及扩展
    return (code >= 0x4E00 && code <= 0x9FA5) || 
           (code >= 0x3400 && code <= 0x4DBF) ||
           (code >= 0x20000 && code <= 0x2A6DF)
}
