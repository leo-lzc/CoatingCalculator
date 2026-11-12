package com.coating.calculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coating.calculator.export.PdfExportAndShareHelper
import com.coating.calculator.ui.i18n.LocalizationManager
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExportAndSharePdfDialog(
    viewModel: CalculatorViewModel,
    onDismiss: () -> Unit
) {
    val strings = LocalizationManager.getStrings(viewModel.currentLanguage)
    val timeFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    val defaultFileName = "CoatingReport_${timeFormat.format(Date())}"
    
    var fileName by remember { mutableStateOf(defaultFileName) }
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.exportDialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.exportDialogDesc, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text(strings.fileNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    PdfExportAndShareHelper.generateAndSharePdf(context, viewModel)
                    onDismiss()
                }
            ) {
                Text(strings.directShareBtn)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
