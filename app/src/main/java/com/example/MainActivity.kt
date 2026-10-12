package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.MainAppContainer
import com.example.ui.theme.PuzzlePopTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dbService = com.example.data.DatabaseService(this)
        com.example.ads.MobileAdsService.getInstance().initialize(this, dbService)
        com.example.ads.AdMobManager.initialize(this)
        enableEdgeToEdge()
        setContent {
            PuzzlePopTheme {
                MainAppContainer()
            }
        }
    }
}
