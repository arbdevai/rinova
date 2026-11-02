package com.rinova.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.rinova.app.core.di.RinovaContainer
import com.rinova.app.ui.navigation.RinovaMainScreen
import com.rinova.app.ui.theme.RinovaTheme

class MainActivity : ComponentActivity() {

    private lateinit var container: RinovaContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        container = RinovaContainer(applicationContext)

        setContent {
            RinovaTheme {
                RinovaMainScreen(
                    container = container,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
