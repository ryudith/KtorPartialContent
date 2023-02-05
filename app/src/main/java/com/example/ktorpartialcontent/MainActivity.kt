package com.example.ktorpartialcontent

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ktorpartialcontent.service.ClientHelper
import com.example.ktorpartialcontent.service.DownloadMetadata
import com.example.ktorpartialcontent.ui.theme.KtorPartialContentTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KtorPartialContentTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val client = ClientHelper.createInstance()
                    val coroutineScope = rememberCoroutineScope()
                    val downloadMetadata = DownloadMetadata(
                        applicationContext,
                        "http://10.0.2.2:8000/download.png",
                        "sample_file.png"
                    )

                    val allowWrite = ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
                    val writePermissionGranted = remember { mutableStateOf(false) }
                    val requestPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) {
                        writePermissionGranted.value = it
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(onClick = {
                            coroutineScope.launch {
                                if (allowWrite == PackageManager.PERMISSION_GRANTED)
                                {
                                    writePermissionGranted.value = true
                                }
                                else
                                {
                                    requestPermissionLauncher.launch(WRITE_EXTERNAL_STORAGE)
                                }
                            }
                        }) {
                            Text("Request Permission")
                        }

                        Button(onClick = {
                            coroutineScope.launch {
                                val response = client.download(downloadMetadata)
                                Log.d("DEBUG_DATA", "No partial content response: ${response}")
                            }
                        }) {
                            Text("No Partial Content")
                        }

                        Button(onClick = {
                            coroutineScope.launch {
                                downloadMetadata.customFileName = true

                                val response = client.download(downloadMetadata)
                                Log.d("DEBUG_DATA", "No partial content and Content-Disposition response: ${response}")
                            }
                        }) {
                            Text("No Partial Content and Content-Disposition")
                        }

                        Button(onClick = {
                            coroutineScope.launch {
                                downloadMetadata.chunkSize = 1024
                                downloadMetadata.fileName = "partial_content.png"
                                downloadMetadata.customFileName = true

                                val response = client.download(downloadMetadata)
                                Log.d("DEBUG_DATA", "Partial content response: ${response}")
                            }
                        }) {
                            Text("Partial Content")
                        }
                    }
                }
            }
        }
    }
}
