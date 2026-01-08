package com.autobook.lingxi.logic

import java.util.regex.Pattern

data class BillInfo(
    val amount: Double,
    val merchant: String,
    val date: String,
    val type: String = "自动提取"
)

object BillParser {

    // 匹配金额
    private val AMOUNT_REGEX = Pattern.compile("(?:￥|[+\\-])?\\s*(\\d+\\.\\d{2})")

    // 匹配日期
    private val DATE_REGEX = Regex(".*(\\d{1,2}月\\d{1,2}日|\\d{4}-\\d{2}-\\d{2}|\\d{1,2}:\\d{2}|昨天|今天).*")

    // 垃圾词黑名单
    private val BLACK_LIST = listOf(
        "支付", "银行", "详情", "成功", "账单", "退款", "入账",
        "报销", "开票", "查看", "更多", "服务", "余额",
        "当前状态", "交易", "商品", "商户", "全称",
        "中国移动", "中国电信", "中国联通"
    )

    // 【新增】金额干扰词（看到这些词的行，里面的数字通常不是实付）
    private val AMOUNT_NOISE = listOf(
        "原价", "优惠", "已省", "折扣", "划线", "立减", "抵扣"
    )

    fun parse(text: String): List<BillInfo> {
        val results = ArrayList<BillInfo>()

        val rawLines = text.split("\n")
            .map { it.trim() }
            .filter { it.length >= 2 }

        if (rawLines.isEmpty()) return results

        // 按时间切分块 (保持之前的逻辑)
        val transactionBlocks = ArrayList<List<String>>()
        var currentBlock = ArrayList<String>()

        for (line in rawLines) {
            if (line.matches(DATE_REGEX)) {
                if (currentBlock.isNotEmpty()) transactionBlocks.add(ArrayList(currentBlock))
                currentBlock.clear()
                currentBlock.add(line)
            } else {
                if (currentBlock.isNotEmpty()) currentBlock.add(line)
            }
        }
        if (currentBlock.isNotEmpty()) transactionBlocks.add(currentBlock)

        for (block in transactionBlocks) {
            val bill = parseBlock(block)
            if (bill != null) results.add(bill)
        }

        return results
    }

    private fun parseBlock(lines: List<String>): BillInfo? {
        if (lines.size < 2) return null

        val dateStr = lines[0]

        // 1. 提取商户 (默认取时间下的第一行)
        var merchantIndex = 1
        var merchant = lines[merchantIndex]

        // 如果第一行是垃圾词，往下顺延一行
        if (isGarbage(merchant) && lines.size > 2) {
            merchantIndex = 2
            merchant = lines[merchantIndex]
        }
        if (isGarbage(merchant)) merchant = "未知商户"

        // 2. 【核心升级】提取金额
        // 你的建议：离商户更近的那个金额，通常是实付！
        // 之前的策略是找"详情"上面，容易找到原价。现在改为：从商户下面开始往下扫，找到的第一个"干净"的金额就是它！

        var amount = 0.0

        // 从商户行的下一行开始遍历
        for (i in (merchantIndex + 1) until lines.size) {
            val lineText = lines[i]

            // 提取这一行里的数字
            val valCandidate = tryExtractAmount(lineText)

            if (valCandidate > 0) {
                // 【关键过滤】如果这一行包含“原价”、“优惠”等字样，说明这个数字是干扰项，跳过！
                if (isAmountNoise(lineText)) continue

                // 找到了！这就是离商户最近、且不是原价的第一个金额
                amount = valCandidate
                break
            }
        }

        if (amount > 0.0) {
            return BillInfo(amount, merchant, dateStr, "智能定位")
        }
        return null
    }

    private fun tryExtractAmount(text: String): Double {
        val matcher = AMOUNT_REGEX.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.toDoubleOrNull() ?: 0.0
        }
        return 0.0
    }

    private fun isGarbage(text: String): Boolean {
        if (text.contains("交易") && text.length > 6) return false
        if (text.matches(Regex("^[0-9.\\-+: ]+$"))) return true
        for (bad in BLACK_LIST) {
            if (text.contains(bad)) return true
        }
        return false
    }

    // 【新增】判断是否是金额干扰行
    private fun isAmountNoise(text: String): Boolean {
        for (noise in AMOUNT_NOISE) {
            if (text.contains(noise)) return true
        }
        return false
    }
}