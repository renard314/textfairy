package com.renard.ocr.util

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.os.Environment.MEDIA_MOUNTED
import android.os.StatFs
import com.renard.ocr.R
import java.io.File

object AppStorage {
    private const val EXTERNAL_APP_DIRECTORY = "textfee"
    private const val IMAGE_DIRECTORY = "pictures"
    private const val CACHE_DIRECTORY = "thumbnails"
    private const val OCR_DATA_DIRECTORY = "tessdata"

    @JvmStatic
    fun getImageDirectory(context: Context) = getAppDirectory(context)?.requireDirectory(IMAGE_DIRECTORY)

    @JvmStatic
    fun getCacheDirectory(context: Context) = getAppDirectory(context)?.requireDirectory(CACHE_DIRECTORY)

    @JvmStatic
    fun getTrainingDataDir(context: Context) = getAppDirectory(context)?.requireDirectory(OCR_DATA_DIRECTORY)

    @JvmStatic
    fun getPDFDir(context: Context) = getAppDirectory(context)?.requireDirectory(context.getString(R.string.config_pdf_file_dir))

    private fun getAppDirectory(context: Context) =
            if(Environment.getExternalStorageState()== MEDIA_MOUNTED){
                context.getExternalFilesDir(EXTERNAL_APP_DIRECTORY)!!.also { it.mkdirs() }
            } else {
                null
            }

    @JvmStatic
    fun setTrainedDataDestinationForDownload(context: Context, request: DownloadManager.Request, trainedDataFileName: String) {
        request.setDestinationInExternalFilesDir(context, EXTERNAL_APP_DIRECTORY, "$OCR_DATA_DIRECTORY/$trainedDataFileName")
    }

    private fun File.requireDirectory(dir: String) =
            File(this, dir).also { it.mkdirs() }

    /**
     * @return the free space on sdcard in bytes
     */
    @JvmStatic
    fun getFreeSpaceInBytes(context: Context): Long {
        return try {
            val stat = StatFs(getAppDirectory(context).toString())
            val availableBlocks = stat.availableBlocks.toLong()
            availableBlocks * stat.blockSize
        } catch (ex: Exception) {
            -1
        }
    }

}