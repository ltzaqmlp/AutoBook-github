package com.autobook.lingxi.logic

import java.util.regex.Pattern

data class BillInfo(
    val amount: Double,
    val merchant: String,
    val type: String = "未分类"
)

object BillParser {

    // 匹配金额：支持 "￥100.00", "-100.00", "+100.00", 或者纯 "100.00"
    // 解释：
    // (?:￥|[+\-])?  -> 前面可能有人民币符号，或者加减号，也可能没有
    // \s* -> 可能有空格
    // (\d+\.\d{2})   -> 核心数字部分 (比如 100.00)
    private val GENERAL_AMOUNT_REGEX = Pattern.compile("(?:￥|[+\\-])?\\s*(\\d+\\.\\d{2})")

    fun parse(text: String): BillInfo? {
        // 1. 优先尝试微信/支付宝特定规则 (之前的逻辑保持不变)
        if (text.contains("微信支付")) {
            return parseWechat(text)
        } else if (text.contains("支付宝")) {
            return parseAlipay(text)
        }

        // 2. 【新增】通用兜底规则：只要有像“金额”的数字，就提取出来
        return parseGeneral(text)
    }

    private fun parseWechat(text: String): BillInfo {
        // ... (保持你之前的代码不变，或者复制下面的通用逻辑)
        val matcher = GENERAL_AMOUNT_REGEX.matcher(text)
        if (matcher.find()) {
            val amount = matcher.group(1)?.toDoubleOrNull() ?: 0.0
            return BillInfo(amount, "微信商户", "支出")
        }
        return BillInfo(0.0, "微信(解析失败)", "未知")
    }

    private fun parseAlipay(text: String): BillInfo {
        val matcher = GENERAL_AMOUNT_REGEX.matcher(text)
        if (matcher.find()) {
            val amount = matcher.group(1)?.toDoubleOrNull() ?: 0.0
            return BillInfo(amount, "支付宝商户", "支出")
        }
        return BillInfo(0.0, "支付宝(解析失败)", "未知")
    }

    // 【修改】智能通用解析逻辑 (替换原来的 parseGeneral)
    private fun parseGeneral(text: String): BillInfo? {
        val matcher = GENERAL_AMOUNT_REGEX.matcher(text)
        if (matcher.find()) {
            val amountStr = matcher.group(1)
            val amount = amountStr?.toDoubleOrNull() ?: 0.0

            // --- 智能筛选商户名 ---
            val lines = text.split("\n")
            var merchantCandidate = "未知商户"

            for (line in lines) {
                val str = line.trim()

                // 1. 跳过空行或太短的字 (比如 ">")
                if (str.length < 2) continue

                // 2. 跳过看起来像时间的 (比如 "08:17", "2026-01-08")
                if (str.matches(Regex(".*\\d{2}:\\d{2}.*"))) continue

                // 3. 跳过包含“支付”、“银行”、“详情”、“成功”等功能性废话的
                if (str.contains("支付") || str.contains("银行") ||
                    str.contains("详情") || str.contains("成功") ||
                    str.contains("账单")) continue

                // 4. 跳过单纯是金额的那一行 (比如 "¥100.00")
                if (str.contains(amountStr ?: "9999999")) continue

                // 5. 跳过运营商状态栏 (针对全屏截图)
                if (str.contains("中国移动") || str.contains("中国电信") || str.contains("中国联通")) continue

                // 🏆 恭喜，如果通过了上面所有关卡，它大概率就是商户名！
                merchantCandidate = str
                break
            }

            return BillInfo(amount, merchantCandidate, "自动提取")
        }
        return null
    }
}