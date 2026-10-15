package com.rainweaver.cogscalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rainweaver.cogscalc.ui.AppNav
import com.rainweaver.cogscalc.ui.CogsViewModel
import com.rainweaver.cogscalc.ui.theme.CogsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CogsTheme {
                val vm: CogsViewModel = viewModel()
                AppNav(vm)
            }
        }
    }
}
