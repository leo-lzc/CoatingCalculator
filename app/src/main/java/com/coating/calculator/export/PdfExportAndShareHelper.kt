package com.coating.calculator.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.coating.calculator.engine.*
import com.coating.calculator.ui.ComponentUiState
import com.coating.calculator.ui.utils.DisplayFormatter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportAndShareHelper {

    fun generateDefaultFileName(now: Date = Date()): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "薄膜涂布配方_${sdf.format(now)}"
    }

    private fun formatTimestamp(now: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(now)
    }

    fun writeFormulaPdfToStream(
        outputStream: OutputStream,
        components: List<ComponentUiState>,
        projectResult: ProjectCostResult,
        batchResult: BatchCalculationResult?,
        dryCoatingWeightText: String,
        filmLengthText: String,
        filmWidthText: String,
        layersText: String,
        batchUnit: String,
        exportTime: Date = Date()
    ) {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val margin = 36f
        var currentY = 48f

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            color = Color.parseColor("#0277BD")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#E1F5FE")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#90A4AE")
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }
        val cellTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f
            isAntiAlias = true
        }
        val cellBoldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val footerPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 9f
            isAntiAlias = true
        }

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
                DisplayFormatter.formatCostInteger(engineComp.unitPrice),
                if (c.isAdjustableSolvent) "是" else "否"
            )
            currentY = drawTableRow(canvas, margin, currentY, colWidths1, row, false, headerBgPaint, borderPaint, cellTextPaint)
        }

        currentY += 18f
        canvas.drawText("二、项目加工参数与成本核算", margin, currentY, sectionPaint)
        currentY += 8f

        val summaryRows = listOf(
            listOf("目标干涂布量", "${dryCoatingWeightText.ifBlank { "--" }} g/m²", "工作液浓度", DisplayFormatter.formatSolidPercent(projectResult.workingSolutionSolid)),
            listOf("薄膜规格(长×宽×层)", "${filmLengthText.ifBlank { "-" }}m × ${filmWidthText.ifBlank { "-" }}m × ${layersText.ifBlank { "-" }}层", "加工总面积", "${DisplayFormatter.formatWeightOrArea(projectResult.processingAreaSqm)} m²"),
            listOf("每平米总湿重", "${DisplayFormatter.formatWeightOrArea(projectResult.totalWetWeightPerSqm)} g/m²", "每平米总成本", "${DisplayFormatter.formatCostInteger(projectResult.totalCostPerSqm)} 元/m²" + if (projectResult.isPerSqmCostIncomplete) "(不完整)" else ""),
            listOf("计算状态", projectResult.status.name, "整卷总成本", "${DisplayFormatter.formatCostInteger(projectResult.totalRollCost)} 元" + if (projectResult.isRollCostIncomplete) "(不完整)" else "")
        )
        val colWidthsSummary = floatArrayOf(120f, 141.5f, 120f, 141.5f)
        for (sRow in summaryRows) {
            currentY = drawTableRow(canvas, margin, currentY, colWidthsSummary, sRow, false, headerBgPaint, borderPaint, cellTextPaint)
        }

        currentY += 10f
        if (projectResult.componentDetails.isNotEmpty()) {
            val colWidths2 = floatArrayOf(123f, 100f, 100f, 100f, 100f)
            val headers2 = listOf("组分名称", "湿重(g/m²)", "单平成本(元/m²)", "整卷用量(kg)", "整卷成本(元)")
            currentY = drawTableRow(canvas, margin, currentY, colWidths2, headers2, true, headerBgPaint, borderPaint, cellBoldPaint)

            for (d in projectResult.componentDetails) {
                val row = listOf(
                    d.name + if (d.isPriceMissing) "(缺价)" else "",
                    DisplayFormatter.formatWeightOrArea(d.wetWeightPerSqm),
                    DisplayFormatter.formatCostInteger(d.costPerSqm),
                    DisplayFormatter.formatWeightOrArea(d.rollConsumptionKg),
                    DisplayFormatter.formatCostInteger(d.rollCost)
                )
                currentY = drawTableRow(canvas, margin, currentY, colWidths2, row, false, headerBgPaint, borderPaint, cellTextPaint)
            }
        }

        val footerY = pageHeight - 28f
        canvas.drawLine(margin, footerY - 12f, pageWidth - margin, footerY - 12f, borderPaint)
        canvas.drawText("最后计算输出时间：${formatTimestamp(exportTime)}", margin, footerY, footerPaint)
        canvas.drawText("薄膜涂布计算器 v1.0", pageWidth - margin - 105f, footerY, footerPaint)

        pdfDocument.finishPage(page)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }

    private fun drawTableRow(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        colWidths: FloatArray,
        cells: List<String>,
        isHeader: Boolean,
        headerBgPaint: Paint,
        borderPaint: Paint,
        textPaint: Paint
    ): Float {
        val rowHeight = 22f
        var x = startX
        for (i in cells.indices) {
            val w = colWidths[i]
            if (isHeader) canvas.drawRect(x, startY, x + w, startY + rowHeight, headerBgPaint)
            canvas.drawRect(x, startY, x + w, startY + rowHeight, borderPaint)
            canvas.drawText(cells[i], x + 5f, startY + 14.5f, textPaint)
            x += w
        }
        return startY + rowHeight
    }

    fun sharePdfViaSystemSheet(
        context: Context,
        sanitizedFileName: String,
        components: List<ComponentUiState>,
        projectResult: ProjectCostResult,
        batchResult: BatchCalculationResult?,
        dryCoatingWeightText: String,
        filmLengthText: String,
        filmWidthText: String,
        layersText: String,
        batchUnit: String
    ) {
        try {
            val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
            val finalName = if (sanitizedFileName.endsWith(".pdf", true)) sanitizedFileName else "$sanitizedFileName.pdf"
            val pdfFile = File(sharedDir, finalName)

            FileOutputStream(pdfFile).use { fos ->
                writeFormulaPdfToStream(fos, components, projectResult, batchResult, dryCoatingWeightText, filmLengthText, filmWidthText, layersText, batchUnit, Date())
            }

            val contentUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, finalName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "分享配方 PDF").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (e: Exception) {
            Toast.makeText(context, "分享失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
