package com.renard.ocr

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import com.googlecode.leptonica.android.ReadFile
import com.renard.ocr.documents.creation.NewDocumentActivity.EXTRA_NATIVE_PIX
import com.renard.ocr.documents.creation.visualisation.OCRActivity
import com.renard.ocr.util.Screen
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.log.logcat
import io.fotoapparat.log.loggers
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.selector.*
import kotlinx.android.synthetic.main.activity_camera_preview.*

class CameraPreviewActivity : MonitoredActivity() {

    override fun getScreenName() = "CameraPreview"

    override fun getHintDialogId() = -1

    private lateinit var fotoapparat: Fotoapparat

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_take_picture -> {
                fotoapparat.takePicture().toBitmap().whenAvailable {
                    if (it != null) {
                        val pix = ReadFile.readBitmap(it.bitmap)
                        val intent = Intent(this, OCRActivity::class.java)
                        intent.putExtra(EXTRA_NATIVE_PIX, pix.nativePix)
                        intent.putExtra(OCRActivity.EXTRA_USE_ACCESSIBILITY_MODE, false)
                        intent.putExtra(OCRActivity.EXTRA_PARENT_DOCUMENT_ID, -1)
                    }
                }
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_home -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_preview)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        val cameraConfiguration = CameraConfiguration(
                pictureResolution = highestResolution(),
                previewResolution = highestResolution(),
                previewFpsRange = highestFps(),
                focusMode = firstAvailable(
                        continuousFocusPicture(),
                        autoFocus(),
                        fixed()
                ),
                flashMode = firstAvailable(
                        torch(),
                        off()
                ),
                antiBandingMode = firstAvailable(
                        auto(),
                        hz50(),
                        hz60(),
                        none()
                ),
                jpegQuality = highestQuality()
        )

        fotoapparat = Fotoapparat(
                context = this,
                view = camera_view,
                scaleType = ScaleType.CenterCrop,
                lensPosition = back(),
                cameraConfiguration = cameraConfiguration,
                logger = loggers(logcat()),
                cameraErrorCallback = { error -> }   // (optional) log fatal errors
        )
    }

    @Suppress("unused")
    fun onEventMainThread(event: PermissionGrantedEvent) {
        fotoapparat.start()
    }

    override fun onStart() {
        super.onStart()
        Screen.lockOrientation(this)
        ensurePermission(Manifest.permission.CAMERA, R.string.permission_explanation)
    }

    override fun onStop() {
        super.onStop()
        Screen.unlockOrientation(this)
        fotoapparat.stop()
    }
}
