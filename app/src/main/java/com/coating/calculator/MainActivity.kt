package com.coating.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.coating.calculator.ui.CalculatorViewModel
import com.coating.calculator.ui.CoatingCalculatorScreen

class MainActivity : ComponentActivity() {
    private val viewModel = CalculatorViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                CoatingCalculatorScreen(viewModel = viewModel)
            }
        }
    }
}
