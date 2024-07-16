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

class ImportFormat : AnAction() {

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
        val pattern = "^((import\\b.*?\\n)+)([\\s\\S]*)$"
        val r = Pattern.compile(pattern)
        val m = r.matcher(code)
        if (!m.find()) {
            Messages.showMessageDialog(
                project,
                "import 必须在最顶上，不可以有缩进；或者没有 import 导入",
                "错误提示",
                Messages.getInformationIcon()
            )
            return
        }

        // 捕获数据
        val importRaw = m.group(1)
        val foot = m.group(3).trim()

        // 冒泡排序
        val imports = importRaw.split("\n").toTypedArray()
        for (i in imports.indices.reversed()) {
            for (j in 1..i) {
                if (imports[j - 1].length > imports[j].length) {
                    val temp = imports[j]
                    imports[j] = imports[j - 1]
                    imports[j - 1] = temp
                }
            }
        }

        // 拼接字符串
        val importString = StringBuilder()
        for (importLine in imports) {
            importString.append(importLine).append("\n")
        }
        importString.append("\n")

        // 写入操作
        val runnable = Runnable {
            document.setText(importString.toString() + foot + "\n")
        }
        WriteCommandAction.runWriteCommandAction(project, runnable)
    }
}