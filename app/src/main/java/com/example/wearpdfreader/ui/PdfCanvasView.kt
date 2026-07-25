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

    var onSingleTapListener: (() -> Unit)? = null
    var onSwipeNextPageListener: (() -> Unit)? = null
    var onSwipePrevPageListener: (() -> Unit)? = null

    // Night Mode Color Inversion Filter
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
    }

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val prevScale = scaleFactor
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(1.0f, 4.5f)
            
            if (prevScale != scaleFactor) {
                clampPosition()
                updateDrawMatrix()
                postInvalidateOnAnimation()
            }
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            onSingleTapListener?.invoke()
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (scaleFactor > 1.05f) {
                posX -= distanceX
                posY -= distanceY
                clampPosition()
                updateDrawMatrix()
                postInvalidateOnAnimation()
            }
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            // Horizontal swipe for page change when at 1.0x zoom
            if (scaleFactor <= 1.05f && e1 != null) {
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > 100 && Math.abs(velocityX) > 200) {
                    if (diffX < 0) {
                        // Swipe Left -> Next Page
                        onSwipeNextPageListener?.invoke()
                    } else {
                        // Swipe Right -> Previous Page
                        onSwipePrevPageListener?.invoke()
                    }
                    return true
                }
            }
            return false
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
            clampPosition()
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

    private fun clampPosition() {
        val bmp = bitmap ?: return
        val vWidth = width.toFloat()
        val vHeight = height.toFloat()
        if (vWidth <= 0 || vHeight <= 0 || bmp.width <= 0 || bmp.height <= 0) return

        val scale = (vWidth / bmp.width.toFloat()).coerceAtMost(vHeight / bmp.height.toFloat())
        val scaledWidth = bmp.width * scale * scaleFactor
        val scaledHeight = bmp.height * scale * scaleFactor

        // Clamp Horizontal
        if (scaledWidth <= vWidth) {
            posX = 0f
        } else {
            val maxDx = (scaledWidth - vWidth) / 2f
            posX = posX.coerceIn(-maxDx, maxDx)
        }

        // Clamp Vertical
        if (scaledHeight <= vHeight) {
            posY = 0f
        } else {
            val maxDy = (scaledHeight - vHeight) / 2f
            posY = posY.coerceIn(-maxDy, maxDy)
        }
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
