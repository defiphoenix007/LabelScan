package com.elytelabs.labelscan

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min

class CornerBoxOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    // Dim background
    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#99000000") // semi-transparent black
        style = Paint.Style.FILL
    }

    // Erase scan area
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    // Corner “L” guides
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676") // a bright green
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }

    // Optional laser line
    private val laserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeWidth = 5f
    }

    private var scanRect = RectF()
    private var laserY = 0f
    private var animator: ValueAnimator? = null

    // Customizable sizes
    private val scanWidthRatio = 0.75f     // 75% of view width
    private val scanHeightRatio = 0.28f    // 28% of view height
    private val cornerLenRatio = 0.08f     // 8% of min(viewWidth, viewHeight)

    // Controls
    var showLaser: Boolean = true
    var dimOutside: Boolean = true

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null) // needed for CLEAR xfermode on some devices
        startLaser()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        // Compute scan rect centered
        val rectW = w * scanWidthRatio
        val rectH = h * scanHeightRatio
        val left = (w - rectW) / 2f
        val top = (h - rectH) / 2f
        scanRect.set(left, top, left + rectW, top + rectH)

        // Dim everything, then clear scan area
        if (dimOutside) {
            canvas.drawRect(0f, 0f, w, h, dimPaint)
            canvas.drawRoundRect(scanRect, 24f, 24f, clearPaint)
        }

        // Draw corner guides (“L” shapes)
        drawCornerGuides(canvas, scanRect)

        // Draw optional laser
        if (showLaser) {
            val y = scanRect.top + laserY
            canvas.drawLine(scanRect.left + 20f, y, scanRect.right - 20f, y, laserPaint)
        }
    }

    private fun drawCornerGuides(canvas: Canvas, r: RectF) {
        val cornerLen = min(width, height) * cornerLenRatio
        val radius = 24f

        // Top-left
        canvas.drawLine(r.left, r.top + radius, r.left, r.top + cornerLen, cornerPaint)
        canvas.drawLine(r.left + radius, r.top, r.left + cornerLen, r.top, cornerPaint)

        // Top-right
        canvas.drawLine(r.right, r.top + radius, r.right, r.top + cornerLen, cornerPaint)
        canvas.drawLine(r.right - radius, r.top, r.right - cornerLen, r.top, cornerPaint)

        // Bottom-left
        canvas.drawLine(r.left, r.bottom - radius, r.left, r.bottom - cornerLen, cornerPaint)
        canvas.drawLine(r.left + radius, r.bottom, r.left + cornerLen, r.bottom, cornerPaint)

        // Bottom-right
        canvas.drawLine(r.right, r.bottom - radius, r.right, r.bottom - cornerLen, cornerPaint)
        canvas.drawLine(r.right - radius, r.bottom, r.right - cornerLen, r.bottom, cornerPaint)
    }

    private fun startLaser() {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1800
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { va ->
                val frac = va.animatedFraction
                val height = scanRect.height().takeIf { it > 0 } ?: (this@CornerBoxOverlay.height * scanHeightRatio)
                laserY = height * frac
                invalidate()
            }
            start()
        }
    }
}