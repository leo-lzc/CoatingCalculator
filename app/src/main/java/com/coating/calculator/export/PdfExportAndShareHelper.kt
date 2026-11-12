package com.coating.calculator.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.coating.calculator.ui.CalculatorViewModel
import com.coating.calculator.ui.i18n.LocalizationManager
import com.coating.calculator.util.DisplayFormatter // 确保这里的包名与你实际创建的 DisplayFormatter.kt 路径一致
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal

object PdfExportAndShareHelper {

    fun generateAndSharePdf(context: Context, viewModel: CalculatorViewModel) {
        val strings = LocalizationManager.getStrings(viewModel.currentLanguage)
        val projectResult = viewModel.getProjectCostResult()
        val components = viewModel.components

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val sectionPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val headerBgPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.FILL
        }

        val borderPaint = Paint().apply {
            color = Color.GRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val cellTextPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            isAntiAlias = true
        }

        val cellBoldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
        }

        var currentY = 40f
        val margin = 40f

        canvas.drawText("薄膜涂布配方与加工计算单", margin, currentY, titlePaint)
        currentY += 24f

        canvas.drawText("一、基础配方参数", margin, currentY, sectionPaint)
        currentY += 8f

        val activeComps = components.filter { it.enabled && !it.toEngineInput().isCompletelyEmpty() }
        val colWidths1 = floatArrayOf(45f, 138f, 85f, 85f, 85f, 85f)
        val headers1 = listOf("位置", "组分名称", "质量配比", "固含量", "单价(元/kg)", "可调溶剂")
        currentY = drawTableRow(canvas, margin, currentY, colWidths1, headers1, true, headerBgPaint, borderPaint, cellBoldPaint)

        for (c in activeComps) {
            val engineComp = c.toEngineInput()
            val row = listOf(
                "#${c.slotIndex}",
                c.name.ifBlank { "组分${c.slotIndex}" },
                DisplayFormatter.formatRatio(engineComp.ratio),
                DisplayFormatter.formatSolidPercent(engineComp.effectiveSolidContent),
                DisplayFormatter.formatCostInteger(engineComp.unitPrice ?: BigDecimal.ZERO),
                if (c.isAdjustableSolvent) "是" else "否"
            )
            currentY = drawTableRow(canvas, margin, currentY, colWidths1, row, false, headerBgPaint, borderPaint, cellTextPaint)
        }

        currentY += 18f
        canvas.drawText("二、项目加工参数与成本核算", margin, currentY, sectionPaint)
        currentY += 8f

        val summaryRows = listOf(
            listOf("目标干涂布量", "${viewModel.coatingDryWeightText.ifBlank { "--" }} g/m²", "工作液浓度", DisplayFormatter.formatSolidPercent(projectResult.workingSolutionSolid)),
            listOf("薄膜规格 (长×宽)", "${viewModel.filmLengthText.ifBlank { "--" }}m × ${viewModel.filmWidthText.ifBlank { "--" }}m", "${viewModel.coatingLayersText.ifBlank { "--" }}层", ""),
            listOf("每平米总湿重", "${DisplayFormatter.formatWeightOrArea(projectResult.totalWetWeightPerSqm)} g/m²", "每平米总成本", "${DisplayFormatter.formatCostInteger(projectResult.totalCostPerSqm)} 元"),
            listOf("计算状态", projectResult.status.name, "整卷总成本", "${DisplayFormatter.formatCostInteger(projectResult.totalRollCost)} 元")
        )
        val colWidthsSummary = floatArrayOf(120f, 141.5f, 120f, 141.5f)

        for (row in summaryRows) {
            currentY = drawTableRow(canvas, margin, currentY, colWidthsSummary, row, false, headerBgPaint, borderPaint, cellTextPaint)
        }

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "CoatingCalculator_Report.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        sharePdf(context, file)
    }

    private fun drawTableRow(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        colWidths: FloatArray,
        texts: List<String>,
        isHeader: Boolean,
        bgPaint: Paint,
        borderPaint: Paint,
        textPaint: Paint
    ): Float {
        var maxHeight = 20f
        val padding = 6f
        var currentX = startX

        for (i in colWidths.indices) {
            val cellWidth = colWidths[i]
            val text = texts.getOrElse(i) { "" }

            if (isHeader) {
                canvas.drawRect(currentX, startY, currentX + cellWidth, startY + maxHeight, bgPaint)
            }
            canvas.drawRect(currentX, startY, currentX + cellWidth, startY + maxHeight, borderPaint)

            canvas.drawText(text, currentX + padding, startY + 14f, textPaint)
            currentX += cellWidth
        }
        return startY + maxHeight
    }

    private fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "分享计算报告 PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
