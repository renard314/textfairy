package com.renard.ocr.util

import android.app.DownloadManager
import android.content.Context
import android.os.Environment.getExternalStorageDirectory
import android.os.StatFs
import com.renard.ocr.R
import java.io.File

object AppStorage {
    private const val EXTERNAL_APP_DIRECTORY = "textfee"
    private const val IMAGE_DIRECTORY = "pictures"
    private const val CACHE_DIRECTORY = "thumbnails"
    private const val OCR_DATA_DIRECTORY = "tessdata"


    private fun getOldTextFairyAppDirectory() = getExternalStorageDirectory().requireDirectory(EXTERNAL_APP_DIRECTORY)
    fun getOldTextFairyCacheDirectory() = getOldTextFairyAppDirectory().requireDirectory(CACHE_DIRECTORY)
    fun getOldTextFairyImageDirectory() = getOldTextFairyAppDirectory().requireDirectory(IMAGE_DIRECTORY)
    @JvmStatic
    fun getOldTrainingDataDir() = getOldTextFairyAppDirectory().requireDirectory(OCR_DATA_DIRECTORY)


    @JvmStatic
    fun getImageDirectory(context: Context) = getAppDirectory(context).requireDirectory(IMAGE_DIRECTORY)

    @JvmStatic
    fun getCacheDirectory(context: Context) = getAppDirectory(context).requireDirectory(CACHE_DIRECTORY)

    @JvmStatic
    fun getTrainingDataDir(context: Context) = getAppDirectory(context).requireDirectory(OCR_DATA_DIRECTORY)

    @JvmStatic
    fun getPDFDir(context: Context) = getAppDirectory(context).requireDirectory(context.getString(R.string.config_pdf_file_dir))

    private fun getAppDirectory(context: Context) = context.getExternalFilesDir(EXTERNAL_APP_DIRECTORY)!!.also { it.mkdirs() }

    @JvmStatic
    fun setTrainedDataDestinationForDownload(context: Context, request: DownloadManager.Request, trainedDataFileName: String) {
        request.setDestinationInExternalFilesDir(context,EXTERNAL_APP_DIRECTORY, "$OCR_DATA_DIRECTORY/$trainedDataFileName")
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