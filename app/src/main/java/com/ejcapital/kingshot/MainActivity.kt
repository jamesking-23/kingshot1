package com.ejcapital.kingshot

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.*
import android.widget.*
import android.graphics.ImageFormat
import android.util.Size
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var preview: TextureView
    private lateinit var status: TextView
    private lateinit var minimal: CheckBox
    private lateinit var manager: CameraManager
    private val executor = Executors.newSingleThreadExecutor()
    private var camera: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var reader: ImageReader? = null
    private var cameraId: String? = null
    private var characteristics: CameraCharacteristics? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        manager = getSystemService(CAMERA_SERVICE) as CameraManager
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            setBackgroundColor(android.graphics.Color.rgb(13, 16, 20))
        }
        val title = TextView(this).apply {
            text = "KINGSHOT  /  CAMERA LAB"
            textSize = 20f
            setTextColor(android.graphics.Color.WHITE)
        }
        status = TextView(this).apply {
            text = "Choose Inspect to check camera capabilities."
            textSize = 13f
            setTextColor(android.graphics.Color.LTGRAY)
            setPadding(0, 8, 0, 8)
        }
        preview = TextureView(this)
        minimal = CheckBox(this).apply {
            text = "Request minimal processing where supported"
            isChecked = true
            setTextColor(android.graphics.Color.WHITE)
        }
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val inspect = Button(this).apply { text = "Inspect"; setOnClickListener { inspectCamera() } }
        val capture = Button(this).apply { text = "CAPTURE"; setOnClickListener { capturePhoto() } }
        controls.addView(inspect, LinearLayout.LayoutParams(0, -2, 1f))
        controls.addView(capture, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(title)
        root.addView(status)
        root.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(minimal)
        root.addView(controls)
        setContentView(root)
        preview.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) { openCamera() }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    private fun inspectCamera() {
        try {
            val id = manager.cameraIdList.firstOrNull { manager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK }
                ?: manager.cameraIdList.first()
            cameraId = id
            characteristics = manager.getCameraCharacteristics(id)
            val c = characteristics!!
            val caps = c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.joinToString() ?: "not reported"
            val raw = c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) == true
            val nr = c.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES)?.joinToString() ?: "not reported"
            val edge = c.get(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES)?.joinToString() ?: "not reported"
            status.text = "Camera $id · RAW=$raw\nCapabilities: $caps\nNoise reduction modes: $nr\nEdge modes: $edge"
        } catch (e: Exception) { status.text = "Inspect failed: ${e.message}" }
    }

    private fun openCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 20); return
        }
        try {
            if (cameraId == null) inspectCamera()
            val id = cameraId ?: return
            manager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) { camera = device; startPreview() }
                override fun onDisconnected(device: CameraDevice) { device.close(); camera = null }
                override fun onError(device: CameraDevice, error: Int) { device.close(); camera = null; runOnUiThread { status.text = "Camera error: $error" } }
            }, executor)
        } catch (e: Exception) { status.text = "Open camera failed: ${e.message}" }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 20 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) openCamera()
        else status.text = "Camera permission is required."
    }

    private fun startPreview() {
        val device = camera ?: return
        val texture = preview.surfaceTexture ?: return
        val size = Size(1280, 720)
        texture.setDefaultBufferSize(size.width, size.height)
        val previewSurface = Surface(texture)
        val map = characteristics?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val jpegSize = map?.getOutputSizes(ImageFormat.JPEG)?.maxByOrNull { it.width.toLong() * it.height } ?: Size(1920, 1080)
        reader?.close()
        reader = ImageReader.newInstance(jpegSize.width, jpegSize.height, ImageFormat.JPEG, 2)
        reader?.setOnImageAvailableListener({ r -> r.acquireNextImage()?.use { image ->
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining()); buffer.get(bytes)
            saveJpeg(bytes)
        } }, executor)
        try {
            device.createCaptureSession(listOf(previewSurface, reader!!.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(s: CameraCaptureSession) {
                    session = s
                    try {
                        val request = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        request.addTarget(previewSurface)
                        applyMinimalControls(request)
                        s.setRepeatingRequest(request.build(), null, executor)
                        runOnUiThread { status.text = "Camera ready · Natural / minimal-control request" }
                    } catch (e: Exception) { runOnUiThread { status.text = "Preview failed: ${e.message}" } }
                }
                override fun onConfigureFailed(s: CameraCaptureSession) { runOnUiThread { status.text = "Camera session configuration failed." } }
            }, executor)
        } catch (e: Exception) { status.text = "Session failed: ${e.message}" }
    }

    private fun applyMinimalControls(request: CaptureRequest.Builder) {
        val c = characteristics ?: return
        if (!minimal.isChecked) return
        c.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES)?.let {
            if (it.contains(CaptureRequest.NOISE_REDUCTION_MODE_OFF)) request.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF)
        }
        c.get(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES)?.let {
            if (it.contains(CaptureRequest.EDGE_MODE_OFF)) request.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF)
        }
        c.get(CameraCharacteristics.SHADING_AVAILABLE_MODES)?.let {
            if (it.contains(CaptureRequest.SHADING_MODE_OFF)) request.set(CaptureRequest.SHADING_MODE, CaptureRequest.SHADING_MODE_OFF)
        }
    }

    private fun capturePhoto() {
        val device = camera ?: run { status.text = "Camera is not ready."; return }
        val active = session ?: run { status.text = "Preview session is not ready."; return }
        try {
            val request = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            request.addTarget(reader!!.surface)
            applyMinimalControls(request)
            active.capture(request.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(s: CameraCaptureSession, r: CaptureRequest, result: TotalCaptureResult) {
                    runOnUiThread { status.text = "Capture complete · ISO ${result.get(CaptureResult.SENSOR_SENSITIVITY) ?: "auto"} · saved to Pictures/KingShot" }
                }
                override fun onCaptureFailed(s: CameraCaptureSession, r: CaptureRequest, failure: CaptureFailure) {
                    runOnUiThread { status.text = "Capture failed: ${failure.reason}" }
                }
            }, executor)
        } catch (e: Exception) { status.text = "Capture error: ${e.message}" }
    }

    private fun saveJpeg(bytes: ByteArray) {
        try {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "KingShot_$stamp.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KingShot")
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        } catch (e: Exception) { runOnUiThread { status.text = "Saving failed: ${e.message}" } }
    }

    override fun onResume() { super.onResume(); if (::preview.isInitialized && preview.isAvailable && camera == null) openCamera() }
    override fun onPause() { closeCamera(); super.onPause() }
    private fun closeCamera() {
        try { session?.close(); session = null; camera?.close(); camera = null; reader?.close(); reader = null } catch (_: Exception) {}
    }
    override fun onDestroy() { closeCamera(); executor.shutdown(); super.onDestroy() }
}
