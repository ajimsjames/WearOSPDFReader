package com.example.wearpdfreader.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

class PdfCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bitmap: Bitmap? = null
    private val drawMatrix = Matrix()
    private val baseMatrix = Matrix()
    
    private val paint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = true
        isDither = true
    }

    private var scaleFactor = 1.0f
    private var posX = 0f
    private var posY = 0f
    private var isNightMode = false

    // Color Inversion Matrix for Night Mode reading
    private val nightModeFilter = ColorMatrixColorFilter(
        ColorMatrix(
            floatArrayOf(
                -1f,  0f,  0f, 0f, 255f,
                 0f, -1f,  0f, 0f, 255f,
                 0f,  0f, -1f, 0f, 255f,
                 0f,  0f,  0f, 1f,   0f
            )
        )
    )

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
    }

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val prevScale = scaleFactor
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(0.9f, 5.0f)
            
            if (prevScale != scaleFactor) {
                updateDrawMatrix()
                postInvalidateOnAnimation()
            }
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            posX -= distanceX
            posY -= distanceY
            updateDrawMatrix()
            postInvalidateOnAnimation()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (scaleFactor > 1.2f) {
                scaleFactor = 1.0f
                posX = 0f
                posY = 0f
            } else {
                scaleFactor = 2.2f
                posX = 0f
                posY = 0f
            }
            updateDrawMatrix()
            postInvalidateOnAnimation()
            return true
        }
    })

    fun setPageBitmap(newBitmap: Bitmap?) {
        bitmap = newBitmap
        scaleFactor = 1.0f
        posX = 0f
        posY = 0f
        recomputeBaseMatrix()
        updateDrawMatrix()
        postInvalidateOnAnimation()
    }

    fun setNightMode(nightMode: Boolean) {
        if (isNightMode != nightMode) {
            isNightMode = nightMode
            paint.colorFilter = if (isNightMode) nightModeFilter else null
            invalidate()
        }
    }

    private fun recomputeBaseMatrix() {
        val bmp = bitmap ?: return
        val vWidth = width.toFloat()
        val vHeight = height.toFloat()
        if (vWidth <= 0 || vHeight <= 0 || bmp.width <= 0 || bmp.height <= 0) return

        baseMatrix.reset()
        val scale = (vWidth / bmp.width.toFloat()).coerceAtMost(vHeight / bmp.height.toFloat())
        val dx = (vWidth - bmp.width * scale) / 2f
        val dy = (vHeight - bmp.height * scale) / 2f

        baseMatrix.postScale(scale, scale)
        baseMatrix.postTranslate(dx, dy)
    }

    private fun updateDrawMatrix() {
        drawMatrix.set(baseMatrix)
        if (scaleFactor != 1.0f || posX != 0f || posY != 0f) {
            val pivotX = width / 2f
            val pivotY = height / 2f
            drawMatrix.postScale(scaleFactor, scaleFactor, pivotX, pivotY)
            drawMatrix.postTranslate(posX, posY)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recomputeBaseMatrix()
        updateDrawMatrix()
    }

    // Intercept Galaxy Watch 6 Rotary Crown / Physical & Touch Bezel Scroll Events
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL) {
            val scrollDelta = event.getAxisValue(MotionEvent.AXIS_SCROLL)
                .takeIf { it != 0f } ?: event.getAxisValue(MotionEvent.AXIS_VSCROLL)

            if (scrollDelta != 0f) {
                // Scroll page content vertically via bezel rotation
                posY += scrollDelta * 80f
                updateDrawMatrix()
                postInvalidateOnAnimation()
                return true
            }
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val sHandled = scaleDetector.onTouchEvent(event)
        val gHandled = gestureDetector.onTouchEvent(event)
        return sHandled || gHandled || super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = bitmap ?: return
        canvas.drawBitmap(bmp, drawMatrix, paint)
    }
}
