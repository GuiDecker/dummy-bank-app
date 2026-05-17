package com.guilherme.honeypot.helper

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.ByteBuffer
import kotlin.coroutines.resume

class CameraHelper(private val context: Context) {

    suspend fun captureSilent(lifecycleOwner: LifecycleOwner): ByteArray? {
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

                        // Small delay to let camera initialize
                        imageCapture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val buffer: ByteBuffer = image.planes[0].buffer
                                    val bytes = ByteArray(buffer.remaining())
                                    buffer.get(bytes)
                                    image.close()
                                    cameraProvider.unbindAll()
                                    Log.d("Honeypot", "Photo captured: ${bytes.size} bytes")
                                    cont.resume(bytes)
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
