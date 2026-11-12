package com.coating.calculator.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coating.calculator.export.PdfExportAndShareHelper
import com.coating.calculator.ui.i18n.LocalizationManager
import java.util.Date

@Composable
fun ExportAndSharePdfDialog(
    viewModel: CalculatorViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalizationManager.getStrings(viewModel.currentLanguage)
    var fileNameInput by remember {
        mutableStateOf(PdfExportAndShareHelper.generateDefaultFileName())
    }

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    PdfExportAndShareHelper.writeFormulaPdfToStream(
                        outputStream = out,
                        components = viewModel.components,
                        projectResult = viewModel.getProjectCostResult(),
                        batchResult = viewModel.getBatchResult(),
                        dryCoatingWeightText = viewModel.coatingDryWeightText,
                        filmLengthText = viewModel.filmLengthText,
                        filmWidthText = viewModel.filmWidthText,
                        layersText = viewModel.coatingLayersText,
                        batchUnit = viewModel.batchUnitText,
                        exportTime = Date()
                    )
                }
                Toast.makeText(context, strings.saveLocalBtn, Toast.LENGTH_SHORT).show()
                onDismiss()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.exportDialogTitle, fontSize = 17.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.exportDialogDesc, fontSize = 13.sp)
                OutlinedTextField(
                    value = fileNameInput,
                    onValueChange = { fileNameInput = it },
                    label = { Text(strings.fileNameLabel) },
                    suffix = { Text(".pdf") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val cleanName = fileNameInput.trim().ifBlank {
                            PdfExportAndShareHelper.generateDefaultFileName()
                        }
                        val fullName = if (cleanName.endsWith(".pdf", true)) cleanName else "$cleanName.pdf"
                        saveDocumentLauncher.launch(fullName)
                    }
                ) {
                    Text(strings.saveLocalBtn)
                }
                Button(
                    onClick = {
                        val cleanName = fileNameInput.trim().ifBlank {
                            PdfExportAndShareHelper.generateDefaultFileName()
                        }
                        PdfExportAndShareHelper.sharePdfViaSystemSheet(
                            context = context,
                            sanitizedFileName = cleanName,
                            components = viewModel.components,
                            projectResult = viewModel.getProjectCostResult(),
                            batchResult = viewModel.getBatchResult(),
                            dryCoatingWeightText = viewModel.coatingDryWeightText,
                            filmLengthText = viewModel.filmLengthText,
                            filmWidthText = viewModel.filmWidthText,
                            layersText = viewModel.coatingLayersText,
                            batchUnit = viewModel.batchUnitText
                        )
                        onDismiss()
                    }
                ) {
                    Text(strings.directShareBtn)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
