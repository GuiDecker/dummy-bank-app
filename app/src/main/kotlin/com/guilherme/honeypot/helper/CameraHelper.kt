package com.guilherme.honeypot.helper

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume

class CameraHelper(private val context: Context) {

    suspend fun captureSilent(lifecycleOwner: LifecycleOwner): File? {
        val outputFile = File(context.cacheDir, "evidence_${System.currentTimeMillis()}.jpg")

        return withTimeoutOrNull(5000L) {
            suspendCancellableCoroutine { cont ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                            .build()

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            imageCapture
                        )

                        val outputOptions = ImageCapture.OutputFileOptions
                            .Builder(outputFile)
                            .build()

                        imageCapture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(
                                    output: ImageCapture.OutputFileResults
                                ) {
                                    cameraProvider.unbindAll()
                                    Log.d(
                                        "Honeypot",
                                        "Photo saved: ${outputFile.absolutePath} (${outputFile.length()} bytes)"
                                    )
                                    cont.resume(outputFile)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e("Honeypot", "Capture failed: ${exception.message}")
                                    cameraProvider.unbindAll()
                                    cont.resume(null)
                                }
                            }
                        )
                    } catch (e: Exception) {
                        Log.e("Honeypot", "Camera setup failed: ${e.message}")
                        cont.resume(null)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        }
    }
}
