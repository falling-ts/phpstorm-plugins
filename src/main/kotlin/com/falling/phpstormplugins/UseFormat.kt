package com.falling.phpstormplugins

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.util.regex.Pattern

class UseFormat : AnAction() {

    private var project: Project? = null

    override fun actionPerformed(event: AnActionEvent) {
        project = event.project
        try {
            execAction(event)
        } catch (error: AssertionError) {
            Messages.showMessageDialog(
                project,
                "未打开代码文件或鼠标焦点未在代码文件上",
                "错误提示",
                Messages.getInformationIcon()
            )
            return
        }
    }

    private fun execAction(event: AnActionEvent) {

        // 获取当前活动代码
        val editor: Editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val document: Document = editor.document
        val code: String = document.text

        // 定义正则匹配规则
        val pattern = "(<\\?php[\\s\\S]*?\\n)(use\\b.*?;([\\s\\S]*?\\nuse\\b.*?;)+)([\\s\\S]*)"
        val r = Pattern.compile(pattern)
        val m = r.matcher(code)
        if (!m.find()) {
            Messages.showMessageDialog(
                project,
                "无 use 导入操作",
                "错误提示",
                Messages.getInformationIcon()
            )
            return
        }

        // 捕获数据
        val headRaw = m.group(1)
        val useBodyRaw = m.group(2)
        val foot = m.group(4).trim()

        // 头部 `// 注释` 删除
        val headR = Pattern.compile("//.*?\\n")
        val headM = headR.matcher(headRaw)
        val head = headM.replaceAll("").trim()

        // use 导入处理
        val bodyR = Pattern.compile(";[\\s\\S]*?\\nuse\\b")
        val bodyM = bodyR.matcher(useBodyRaw)
        val useBody = bodyM.replaceAll(";\nuse").trim()

        // 冒泡排序
        val uses = useBody.split("\n").toTypedArray()
        for (i in uses.indices.reversed()) {
            for (j in 1..i) {
                if (uses[j - 1].length > uses[j].length) {
                    val temp = uses[j]
                    uses[j] = uses[j - 1]
                    uses[j - 1] = temp
                }
            }
        }

        // 拼接字符串
        val use = StringBuilder("\n\n")
        for (useLine in uses) {
            use.append(useLine).append("\n")
        }
        use.append("\n")

        // 写入操作
        val runnable = Runnable {
            document.setText(head + use.toString() + foot + "\n")
        }
        WriteCommandAction.runWriteCommandAction(project, runnable)
    }
}
