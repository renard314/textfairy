/*
 * Copyright (C) 2012,2013 Renard Wellnitz.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.renard.ocr.documents.creation.visualisation

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.graphics.times
import androidx.core.graphics.toRectF
import com.renard.ocr.R
import com.renard.ocr.documents.creation.crop.ImageViewTouchBase
import java.util.*

/**
 * is used to show preview images and progress during ocr process
 *
 * @author renard
 */
class OCRImageView : ImageViewTouchBase {
    private val mNumberStrokePaint = Paint()
    private val mNumberPaint = Paint()
    private val mWordPaint = Paint()
    private val mBackgroundPaint = Paint()
    private val mScanLinePaint = Paint()
    private val mImageRectPaint = Paint()
    private val mTextRectPaint = Paint()
    private val mTouchedImageRectPaint = Paint()
    private val mTouchedTextRectPaint = Paint()
    private var mImageRects: MutableList<RectF> = mutableListOf()
    private var mTextRects: MutableList<RectF> = mutableListOf()
    private val mTouchedImageRects = ArrayList<RectF>()
    private var mTouchedTextRects = ArrayList<RectF>()
    private var mProgress = 0
    private val mWordBoundingBox = RectF()
    private val mOCRBoundingBox = RectF()
    private val mViewDrawingRect = RectF()

    constructor(context: Context) : super(context) {
        init(context)
    }

    fun clearAllProgressInfo() {
        mTouchedImageRects.clear()
        mTouchedTextRects.clear()
        mImageRects.clear()
        mTextRects!!.clear()
    }

    fun getSelectedImageIndexes(): IntArray {
        val result = IntArray(mTouchedImageRects.size)
        for (i in mTouchedImageRects.indices) {
            val j = mImageRects!!.indexOf(mTouchedImageRects[i])
            result[i] = j
        }
        return result
    }

    fun getSelectedTextIndexes(): IntArray {
        val result = IntArray(mTouchedTextRects.size)
        for (i in mTouchedTextRects.indices) {
            val j = mTextRects!!.indexOf(mTouchedTextRects[i])
            result[i] = j
        }
        return result
    }

    fun setImageRects(boxes: List<Rect>, pageWidth: Int, pageHeight: Int) {
        val xScale = 1.0f * mBitmapDisplayed.width / pageWidth
        mImageRects.apply {
            clear()
            addAll(boxes.map { it.toRectF().times(xScale) })
        }
        this.invalidate()
    }

    fun setTextRects(boxes: List<Rect>, pageWidth: Int, pageHeight: Int) {
        val xScale = 1.0f * mBitmapDisplayed.width / pageWidth
        mTextRects.apply {
            clear()
            addAll(boxes.map { it.toRectF().times(xScale) })
        }
        this.invalidate()
    }

    fun setProgress(newProgress: Int, wordBoundingBox: Rect, pageBoundingBox: Rect, pageWidth: Int, pageHeight: Int) {
        // scale the word bounding rectangle to the preview image space
        val xScale = 1.0f * mBitmapDisplayed.width / pageWidth
        val wordBounds = wordBoundingBox.toRectF().times(xScale)
        val ocrBounds = pageBoundingBox.toRectF().times(xScale)
        mProgress = newProgress
        mWordBoundingBox.set(wordBounds)
        mOCRBoundingBox.set(ocrBounds)
        this.invalidate()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
        mTouchedTextRects = ArrayList()
    }

    override fun onZoomFinished() {}

    private fun init(c: Context) {
        val progressColor = c.resources.getColor(R.color.progress_color)
        mBackgroundPaint.setARGB(125, 50, 50, 50)
        mScanLinePaint.color = progressColor
        mScanLinePaint.strokeWidth = 3f
        mScanLinePaint.isAntiAlias = true
        mScanLinePaint.style = Paint.Style.STROKE
        mWordPaint.setARGB(125, Color.red(progressColor), Color.green(progressColor), Color.blue(progressColor))
        mImageRectPaint.color = progressColor
        mImageRectPaint.strokeWidth = 3f
        mImageRectPaint.isAntiAlias = true
        mImageRectPaint.style = Paint.Style.STROKE
        mTouchedImageRectPaint.setARGB(125, Color.red(progressColor), Color.green(progressColor), Color.blue(progressColor))
        mTouchedImageRectPaint.strokeWidth = 3f
        mTouchedImageRectPaint.isAntiAlias = true
        mTouchedImageRectPaint.style = Paint.Style.FILL
        mTextRectPaint.color = -0xffd501
        mTextRectPaint.strokeWidth = 3f
        mTextRectPaint.isAntiAlias = true
        mTextRectPaint.style = Paint.Style.STROKE
        mTouchedTextRectPaint.setARGB(125, 0x00, 0x2A, 0xFF)
        mTouchedTextRectPaint.strokeWidth = 3f
        mTouchedTextRectPaint.isAntiAlias = true
        mTouchedTextRectPaint.style = Paint.Style.FILL
        mNumberPaint.setARGB(0xff, 0x33, 0xb5, 0xe5)
        mNumberPaint.textAlign = Paint.Align.CENTER
        mNumberPaint.textSize = TEXT_SIZE * resources.displayMetrics.density
        val tf = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        mNumberPaint.typeface = tf
        mNumberPaint.isAntiAlias = true
        mNumberPaint.maskFilter = BlurMaskFilter(3.toFloat(), BlurMaskFilter.Blur.SOLID)
        mNumberStrokePaint.setARGB(255, 0, 0, 0)
        mNumberStrokePaint.textAlign = Paint.Align.CENTER
        mNumberStrokePaint.textSize = TEXT_SIZE * resources.displayMetrics.density
        mNumberStrokePaint.typeface = tf
        mNumberStrokePaint.style = Paint.Style.STROKE
        mNumberStrokePaint.strokeWidth = 2f
        mNumberStrokePaint.isAntiAlias = true

        // mNumberPaint.setMaskFilter(new EmbossMaskFilter(new float[] { 1, 1,
        // 1},0.8f, 10, 4f));
        mProgress = -1
    }

    private fun updateTouchedBoxesByPoint(x: Float, y: Float, boxes: List<RectF>?, touchedBoxes: MutableList<RectF>) {
        if (boxes != null) {
            for (r in boxes) {
                if (r.contains(x, y)) {
                    if (touchedBoxes.contains(r)) {
                        touchedBoxes.remove(r)
                    } else {
                        touchedBoxes.add(r)
                    }
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val pts = floatArrayOf(event.x, event.y)
                val inverse = Matrix()
                imageViewMatrix.invert(inverse)
                inverse.mapPoints(pts)
                updateTouchedBoxesByPoint(pts[0], pts[1], mImageRects, mTouchedImageRects)
                updateTouchedBoxesByPoint(pts[0], pts[1], mTextRects, mTouchedTextRects)
                this.invalidate()
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (drawable == null) {
            return
        }
        if (mProgress >= 0) {
            drawBoxAroundCurrentWord(canvas)
            /* draw progress rectangle */mViewDrawingRect[mOCRBoundingBox.left, mOCRBoundingBox.top, mOCRBoundingBox.right] = mOCRBoundingBox.bottom
            imageViewMatrix.mapRect(mViewDrawingRect)
            canvas.drawRect(mViewDrawingRect, mScanLinePaint)
            val centerx = mViewDrawingRect.centerX()
            val centery = mViewDrawingRect.centerY()
            val pos = (mViewDrawingRect.height() * (mProgress / 100f)).toInt()
            mViewDrawingRect.top += pos.toFloat()
            canvas.drawRect(mViewDrawingRect, mBackgroundPaint)
            canvas.drawLine(mViewDrawingRect.left, mViewDrawingRect.top, mViewDrawingRect.right, mViewDrawingRect.top, mScanLinePaint)
            canvas.drawText("$mProgress%", centerx, centery, mNumberPaint)
            canvas.drawText("$mProgress%", centerx, centery, mNumberStrokePaint)
        }
        /* draw boxes around text/images */drawRects(canvas, mImageRects, mImageRectPaint)
        drawRects(canvas, mTextRects, mTextRectPaint)
        /* draw special boxes around text/images selected by the user */drawRects(canvas, mTouchedImageRects, mTouchedImageRectPaint)
        drawRectsWithIndex(canvas, mTouchedTextRects, mTouchedTextRectPaint)
    }

    private fun drawBoxAroundCurrentWord(canvas: Canvas) {
        if (!mWordBoundingBox.isEmpty) {
            imageViewMatrix.mapRect(mWordBoundingBox)
            canvas.drawRect(mWordBoundingBox, mWordPaint)
        }
    }

    private fun drawRectsWithIndex(canvas: Canvas, rects: ArrayList<RectF>?, paint: Paint) {
        if (rects != null) {
            val mappedRect = RectF()
            for (i in rects.indices) {
                val r = rects[i]
                mappedRect.set(r)
                imageViewMatrix.mapRect(mappedRect)
                canvas.drawRect(mappedRect, paint)
                canvas.drawText((i + 1).toString(), mappedRect.centerX(), mappedRect.centerY(), mNumberPaint)
                canvas.drawText((i + 1).toString(), mappedRect.centerX(), mappedRect.centerY(), mNumberStrokePaint)
            }
        }
    }

    private fun drawRects(canvas: Canvas, rects: List<RectF>?, paint: Paint) {
        if (rects != null) {
            val mappedRect = RectF()
            for (r in rects) {
                mappedRect.set(r)
                imageViewMatrix.mapRect(mappedRect)
                canvas.drawRect(mappedRect, paint)
            }
        }
    }

    companion object {
        private const val TEXT_SIZE = 60f
        private val LOG_TAG = OCRImageView::class.java.simpleName
    }
}