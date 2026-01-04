package com.draintech.draintech.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.draintech.draintech.ui.theme.DraintechTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DraintechTheme {

                MainScreen()
            }
        }
    }
}

