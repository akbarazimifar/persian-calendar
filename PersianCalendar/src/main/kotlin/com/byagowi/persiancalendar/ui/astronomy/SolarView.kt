package com.byagowi.persiancalendar.ui.astronomy

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.graphics.withRotation
import androidx.core.graphics.withTranslation
import com.byagowi.persiancalendar.ui.common.SolarDraw
import com.byagowi.persiancalendar.ui.utils.dp
import com.byagowi.persiancalendar.ui.utils.resolveColor
import com.byagowi.persiancalendar.utils.DAY_IN_MILLIS
import com.google.android.material.math.MathUtils
import io.github.cosinekitty.astronomy.Ecliptic
import io.github.cosinekitty.astronomy.Spherical
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.eclipticGeoMoon
import io.github.cosinekitty.astronomy.sunPosition
import java.util.*
import kotlin.math.min

class SolarView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var currentTime = System.currentTimeMillis() - DAY_IN_MILLIS // Initial animation
    private var sun: Ecliptic? = null
    private var moon: Spherical? = null
    private var animator: ValueAnimator? = null

    fun setTime(
        time: GregorianCalendar,
        immediate: Boolean,
        update: (Ecliptic, Spherical) -> Unit
    ) {
        animator?.removeAllUpdateListeners()
        if (immediate) {
            currentTime = time.time.time
            val date = Time.fromMillisecondsSince1970(currentTime)
            val sun = sunPosition(date).also { sun = it }
            val moon = eclipticGeoMoon(date).also { moon = it }
            update(sun, moon)
            invalidate()
            return
        }
        ValueAnimator.ofFloat(currentTime.toFloat(), time.timeInMillis.toFloat()).also {
            animator = it
            it.duration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
            it.interpolator = AccelerateDecelerateInterpolator()
            it.addUpdateListener { _ ->
                currentTime = ((it.animatedValue as? Float) ?: 0f).toLong()
                val date = Time.fromMillisecondsSince1970(currentTime)
                sun = sunPosition(date)
                moon = eclipticGeoMoon(date)
                invalidate()
            }
        }.start()
    }

    var isTropicalDegree = false
        set(value) {
            if (value == field) return
            ValueAnimator.ofFloat(if (value) 0f else 1f, if (value) 1f else 0f).also { animator ->
                animator.duration =
                    resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
                animator.interpolator = AccelerateDecelerateInterpolator()
                animator.addUpdateListener { _ ->
                    val fraction = ((animator.animatedValue as? Float) ?: 0f)
                    ranges.indices.forEach {
                        ranges[it][0] = MathUtils.lerp(
                            iauRanges[it][0], tropicalRanges[it][0], fraction
                        )
                        ranges[it][1] = MathUtils.lerp(
                            iauRanges[it][1], tropicalRanges[it][1], fraction
                        )
                    }
                    invalidate()
                }
            }.start()
            field = value
        }
    private val tropicalRanges = Zodiac.values().map { it.tropicalRange.map(Double::toFloat) }
    private val iauRanges = Zodiac.values().map { it.iauRange.map(Double::toFloat) }
    private val ranges = iauRanges.map { it.toFloatArray() }

    private val labels = Zodiac.values().map { it.format(context, false, short = true) }

    override fun onDraw(canvas: Canvas) {
        val sun = sun ?: return
        val moon = moon ?: return
        val radius = min(width, height) / 2f
        arcRect.set(0f, 0f, 2 * radius, 2 * radius)
        val circleInset = radius * .05f
        arcRect.inset(circleInset, circleInset)
        canvas.drawArc(arcRect, 0f, 360f, true, zodiacBackgroundPaint)
        ranges.forEachIndexed { index, (start, end) ->
            canvas.withRotation(-end + 90f, radius, radius) {
                if (index % 2 == 0) canvas.drawArc(
                    arcRect, -90f, end - start, true, zodiacForegroundPaint
                )
                drawLine(radius, circleInset, radius, radius, zodiacSeparatorPaint)
            }
            canvas.withRotation(-(start + end) / 2 + 90f, radius, radius) {
                drawText(labels[index], radius, radius * .12f, zodiacPaint)
            }
        }
        val cr = radius / 8f
        solarDraw.earth(canvas, radius, radius, cr / 1.5f, sun)
        val sunDegree = sun.elon.toFloat()
        canvas.withRotation(-sunDegree + 90f, radius, radius) {
            solarDraw.sun(this, radius, radius / 3.5f, cr)
            canvas.withTranslation(x = radius, y = 0f) {
                canvas.drawPath(trianglePath, sunIndicatorPaint)
            }
        }
        val moonDegree = moon.lon.toFloat()
        canvas.drawCircle(radius, radius, radius * .3f, moonOrbitPaint)
        canvas.withRotation(-moonDegree + 90f, radius, radius) {
            val moonDistance = moon.dist / 0.002569 // Lunar distance in AU
            solarDraw.moon(
                this, sun, moon, radius,
                radius * moonDistance.toFloat() * .7f, cr / 1.9f
            )
            canvas.withTranslation(x = radius, y = 0f) {
                canvas.drawPath(trianglePath, moonIndicatorPaint)
            }
        }
    }

    private val trianglePath = Path().also {
        it.moveTo(0f, 6.dp)
        it.lineTo((-5).dp, .5.dp)
        it.lineTo(5.dp, .5.dp)
        it.close()
    }
    private val arcRect = RectF()

    private val moonIndicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).also {
        it.color = 0x78808080
        it.style = Paint.Style.FILL
    }
    private val sunIndicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).also {
        it.color = 0xFFEEBB22.toInt()
        it.style = Paint.Style.FILL
    }
    private val zodiacBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).also {
        it.color = 0x08808080
        it.style = Paint.Style.FILL
    }
    private val zodiacForegroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).also {
        it.color = 0x18808080
        it.style = Paint.Style.FILL
    }
    private val zodiacSeparatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).also {
        it.color = context.resolveColor(com.google.android.material.R.attr.colorSurface)
        it.strokeWidth = .5.dp
        it.style = Paint.Style.STROKE
    }

    private val zodiacPaint = Paint(Paint.ANTI_ALIAS_FLAG).also {
        it.color = 0xFF808080.toInt()
        it.strokeWidth = 1.dp
        it.textSize = 10.dp
        it.textAlign = Paint.Align.CENTER
    }
    private val moonOrbitPaint = Paint(Paint.ANTI_ALIAS_FLAG).also {
        it.style = Paint.Style.STROKE
        it.strokeWidth = 1.dp
        it.color = 0x40808080
    }

    private val solarDraw = SolarDraw(context)
}
