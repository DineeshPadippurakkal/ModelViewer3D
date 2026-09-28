package com.modelviewer3d

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.modelviewer3d.ui.ModelViewerScreen
import com.modelviewer3d.ui.theme.ModelViewer3DTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            ModelViewer3DTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ModelViewerScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
