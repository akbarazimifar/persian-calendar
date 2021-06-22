package net.androgames.level

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.byagowi.persiancalendar.R
import com.byagowi.persiancalendar.utils.a11yAnnounceAndClick
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

/*
*  This file is part of Level (an Android Bubble Level).
*  <https://github.com/avianey/Level>
*
*  Copyright (C) 2014 Antoine Vianey
*
*  Level is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, either version 3 of the License, or
*  (at your option) any later version.
*
*  Level is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*
*  You should have received a copy of the GNU General Public License
*  along with Level. If not, see <http://www.gnu.org/licenses/>
*/
class LevelView : View {
    /**
     * Rect
     */
    private val displayRect = Rect()

    /**
     * Format des angles
     */
    private val displayFormat = DecimalFormat("00.0")
    private val lcdForegroundPaint = Paint()
    private val lcdBackgroundPaint = Paint()
    private val infoPaint = Paint()
    private var isAlreadyLeveled = true // deliberately

    /**
     * Dimensions
     */
    private var canvasWidth = 0
    private var canvasHeight = 0
    private var minLevelX = 0
    private var maxLevelX = 0
    private var levelWidth = 0
    private var levelHeight = 0
    private var levelMinusBubbleWidth = 0
    private var levelMinusBubbleHeight = 0
    private var middleX = 0
    private var middleY = 0
    private var halfBubbleWidth = 0
    private var halfBubbleHeight = 0
    private var halfMarkerGap = 0
    private var minLevelY = 0
    private var maxLevelY = 0
    private var minBubble = 0
    private var maxBubble = 0
    private var markerThickness = 0
    private var levelBorderWidth = 0
    private var levelBorderHeight = 0
    private var lcdWidth = 0
    private var lcdHeight = 0
    private var displayPadding = 0
    private var displayGap = 0
    private var sensorGap = 0
    private var levelMaxDimension = 0

    /**
     * Angles
     */
    private var angle1 = 0f
    private var angle2 = 0f

    /**
     * Orientation
     */
    private var orientation: Orientation? = null
    private var lastTime: Long = 0
    private var lastTimeShowAngle: Long = 0
    private var angleToShow1 = 0.0
    private var angleToShow2 = 0.0
    private var angleX = 0.0
    private var angleY = 0.0
    private var speedX = 0.0
    private var speedY = 0.0
    private var x = 0.0
    private var y = 0.0

    /**
     * Drawables
     */
    private var level1D: Drawable? = null
    private var bubble1D: Drawable? = null
    private var marker1D: Drawable? = null
    private var level2D: Drawable? = null
    private var bubble2D: Drawable? = null
    private var marker2D: Drawable? = null
    private var display: Drawable? = null

    /**
     * Ajustement de la vitesse
     */
    private var viscosityValue = 1.0
    private var firstTime = true

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    private fun init(context: Context) {
        val resources = context.resources
        // drawable
        level1D = ResourcesCompat.getDrawable(resources, R.drawable.level_1d, null)
        level2D = ResourcesCompat.getDrawable(resources, R.drawable.level_2d, null)
        bubble1D = ResourcesCompat.getDrawable(resources, R.drawable.bubble_1d, null)
        bubble2D = ResourcesCompat.getDrawable(resources, R.drawable.bubble_2d, null)
        marker1D = ResourcesCompat.getDrawable(resources, R.drawable.marker_1d, null)
        marker2D = ResourcesCompat.getDrawable(resources, R.drawable.marker_2d, null)
        display = ResourcesCompat.getDrawable(resources, R.drawable.display, null)

        // config
        displayFormat.decimalFormatSymbols = DecimalFormatSymbols(Locale.ENGLISH)

        // typeface
        val lcd = Typeface.createFromAsset(context.assets, FONT_LCD)

        // paint
        infoPaint.color = resources.getColor(R.color.black)
        infoPaint.isAntiAlias = true
        infoPaint.textSize = resources.getDimensionPixelSize(R.dimen.info_text).toFloat()
        infoPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        infoPaint.textAlign = Paint.Align.CENTER
        lcdForegroundPaint.color = resources.getColor(R.color.lcd_front)
        lcdForegroundPaint.isAntiAlias = true
        lcdForegroundPaint.textSize = resources.getDimensionPixelSize(R.dimen.lcd_text).toFloat()
        lcdForegroundPaint.typeface = lcd
        lcdForegroundPaint.textAlign = Paint.Align.CENTER
        lcdBackgroundPaint.color = resources.getColor(R.color.lcd_back)
        lcdBackgroundPaint.isAntiAlias = true
        lcdBackgroundPaint.textSize = resources.getDimensionPixelSize(R.dimen.lcd_text).toFloat()
        lcdBackgroundPaint.typeface = lcd
        lcdBackgroundPaint.textAlign = Paint.Align.CENTER

        // dimens
        lcdBackgroundPaint.getTextBounds(
            displayBackgroundText,
            0,
            displayBackgroundText.length,
            displayRect
        )
        lcdHeight = displayRect.height()
        lcdWidth = displayRect.width()
        levelBorderWidth = resources.getDimensionPixelSize(R.dimen.level_border_width)
        levelBorderHeight = resources.getDimensionPixelSize(R.dimen.level_border_height)
        markerThickness = resources.getDimensionPixelSize(R.dimen.marker_thickness)
        displayGap = resources.getDimensionPixelSize(R.dimen.display_gap)
        sensorGap = resources.getDimensionPixelSize(R.dimen.sensor_gap)
        displayPadding = resources.getDimensionPixelSize(R.dimen.display_padding)
        invalidate()
    }

    fun setOrientation(
        newOrientation: Orientation,
        newPitch: Float,
        newRoll: Float,
        newBalance: Float
    ) {
        if (orientation == null || orientation != newOrientation) {
            orientation = newOrientation
            val infoY: Int
            infoY = when (newOrientation) {
                Orientation.LEFT, Orientation.RIGHT -> (canvasHeight - canvasWidth) / 2 + canvasWidth
                Orientation.TOP, Orientation.BOTTOM -> canvasHeight
                else -> canvasHeight
            }
            val sensorY = infoY - sensorGap
            middleX = canvasWidth / 2
            middleY = canvasHeight / 2
            when (newOrientation) {
                Orientation.LANDING -> {
                    levelWidth = levelMaxDimension
                    levelHeight = levelMaxDimension
                }
                Orientation.TOP, Orientation.BOTTOM, Orientation.LEFT, Orientation.RIGHT -> {
                    levelWidth = canvasWidth - 2 * displayGap
                    levelHeight = (levelWidth * LEVEL_ASPECT_RATIO).toInt()
                }
            }
            viscosityValue = levelWidth.toDouble()
            minLevelX = middleX - levelWidth / 2
            maxLevelX = middleX + levelWidth / 2
            minLevelY = middleY - levelHeight / 2
            maxLevelY = middleY + levelHeight / 2

            // bubble
            halfBubbleWidth = (levelWidth * BUBBLE_WIDTH / 2).toInt()
            halfBubbleHeight = (halfBubbleWidth * BUBBLE_ASPECT_RATIO).toInt()
            val bubbleWidth = 2 * halfBubbleWidth
            val bubbleHeight = 2 * halfBubbleHeight
            maxBubble = (maxLevelY - bubbleHeight * BUBBLE_CROPPING).toInt()
            minBubble = maxBubble - bubbleHeight

            // display
            displayRect[middleX - lcdWidth / 2 - displayPadding, sensorY - displayGap - 2 * displayPadding - lcdHeight, middleX + lcdWidth / 2 + displayPadding] =
                sensorY - displayGap

            // marker
            halfMarkerGap = (levelWidth * MARKER_GAP / 2).toInt()

            // autres
            levelMinusBubbleWidth = levelWidth - bubbleWidth - 2 * levelBorderWidth
            levelMinusBubbleHeight = levelHeight - bubbleHeight - 2 * levelBorderWidth

            // positionnement
            level1D!!.setBounds(minLevelX, minLevelY, maxLevelX, maxLevelY)
            level2D!!.setBounds(minLevelX, minLevelY, maxLevelX, maxLevelY)
            marker2D!!.setBounds(
                middleX - halfMarkerGap - markerThickness,
                middleY - halfMarkerGap - markerThickness,
                middleX + halfMarkerGap + markerThickness,
                middleY + halfMarkerGap + markerThickness
            )
            x = (maxLevelX + minLevelX).toDouble() / 2
            y = (maxLevelY + minLevelY).toDouble() / 2
        }
        when (orientation) {
            Orientation.TOP, Orientation.BOTTOM -> {
                angle1 = Math.abs(newBalance)
                angleX = Math.sin(Math.toRadians(newBalance.toDouble())) / MAX_SINUS
            }
            Orientation.LANDING -> {
                angle2 = Math.abs(newRoll)
                angleX = Math.sin(Math.toRadians(newRoll.toDouble())) / MAX_SINUS
                angle1 = Math.abs(newPitch)
                angleY = Math.sin(Math.toRadians(newPitch.toDouble())) / MAX_SINUS
                if (angle1 > 90) {
                    angle1 = 180 - angle1
                }
            }
            Orientation.RIGHT, Orientation.LEFT -> {
                angle1 = Math.abs(newPitch)
                angleY = Math.sin(Math.toRadians(newPitch.toDouble())) / MAX_SINUS
                if (angle1 > 90) {
                    angle1 = 180 - angle1
                }
            }
        }
        // correction des angles affiches
        if (angle1 > 99.9f) {
            angle1 = 99.9f
        }
        if (angle2 > 99.9f) {
            angle2 = 99.9f
        }
        // correction des angles aberrants
        // pour ne pas que la bulle sorte de l'ecran
        if (angleX > 1) {
            angleX = 1.0
        } else if (angleX < -1) {
            angleX = -1.0
        }
        if (angleY > 1) {
            angleY = 1.0
        } else if (angleY < -1) {
            angleY = -1.0
        }
        // correction des angles a plat
        // la bulle ne doit pas sortir du niveau
        if (orientation == Orientation.LANDING && angleX != 0.0 && angleY != 0.0) {
            val n = Math.sqrt(angleX * angleX + angleY * angleY)
            val teta = Math.acos(Math.abs(angleX) / n)
            val l = 1 / Math.max(Math.abs(Math.cos(teta)), Math.abs(Math.sin(teta)))
            angleX = angleX / l
            angleY = angleY / l
        }
        if (orientation!!.isLevel(newPitch, newRoll, newBalance, .8f)) {
            if (!isAlreadyLeveled) {
                a11yAnnounceAndClick(this, R.string.level)
                isAlreadyLeveled = true
            }
        } else {
            isAlreadyLeveled = false
        }
        postInvalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasWidth = w
        canvasHeight = h
        levelMaxDimension = Math.min(
            Math.min(h, w) - 2 * displayGap,
            Math.max(h, w) - 2 * (sensorGap + 3 * displayGap + lcdHeight)
        )
    }

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        if (firstTime) {
            setOrientation(Orientation.LANDING, 0f, 0f, 0f)
            firstTime = false
        }

        // update physics
        val currentTime = System.currentTimeMillis()
        val timeDiff = (currentTime - lastTime) / 1000.0
        lastTime = currentTime
        if (currentTime - lastTimeShowAngle > 500) {
            angleToShow1 = angle1.toDouble()
            angleToShow2 = angle2.toDouble()
            lastTimeShowAngle = currentTime
        }
        val posX =
            orientation.getReverse() * (2 * x - minLevelX - maxLevelX) / levelMinusBubbleWidth
        when (orientation) {
            Orientation.TOP, Orientation.BOTTOM -> speedX =
                orientation.getReverse() * (angleX - posX) * viscosityValue
            Orientation.LEFT, Orientation.RIGHT -> speedX =
                orientation.getReverse() * (angleY - posX) * viscosityValue
            Orientation.LANDING -> {
                val posY = (2 * y - minLevelY - maxLevelY) / levelMinusBubbleHeight
                speedX = (angleX - posX) * viscosityValue
                speedY = (angleY - posY) * viscosityValue
                y += speedY * timeDiff
            }
        }
        x += speedX * timeDiff
        // en cas de latence elevee
        // si la bubble a trop deviee
        // elle est replacee correctement
        if (orientation == Orientation.LANDING) {
            if (Math.sqrt(
                    (middleX - x) * (middleX - x)
                            + (middleY - y) * (middleY - y)
                ) > levelMaxDimension / 2 - halfBubbleWidth
            ) {
                x = (angleX * levelMinusBubbleWidth + minLevelX + maxLevelX) / 2
                y = (angleY * levelMinusBubbleHeight + minLevelY + maxLevelY) / 2
            }
        } else {
            if (x < minLevelX + halfBubbleWidth || x > maxLevelX - halfBubbleWidth) {
                x = (angleX * levelMinusBubbleWidth + minLevelX + maxLevelX) / 2
            }
        }
        if (orientation == Orientation.LANDING) {
            // Angle
            display!!.setBounds(
                displayRect.left - (displayRect.width() + displayGap) / 2,
                displayRect.top,
                displayRect.right - (displayRect.width() + displayGap) / 2,
                displayRect.bottom
            )
            display!!.draw(canvas)
            display!!.setBounds(
                displayRect.left + (displayRect.width() + displayGap) / 2,
                displayRect.top,
                displayRect.right + (displayRect.width() + displayGap) / 2,
                displayRect.bottom
            )
            display!!.draw(canvas)
            canvas.drawText(
                displayBackgroundText, (
                        middleX - (displayRect.width() + displayGap) / 2).toFloat(), (
                        displayRect.centerY() + lcdHeight / 2).toFloat(),
                lcdBackgroundPaint
            )
            canvas.drawText(
                displayFormat.format(angleToShow2), (
                        middleX - (displayRect.width() + displayGap) / 2).toFloat(), (
                        displayRect.centerY() + lcdHeight / 2).toFloat(),
                lcdForegroundPaint
            )
            canvas.drawText(
                displayBackgroundText, (
                        middleX + (displayRect.width() + displayGap) / 2).toFloat(), (
                        displayRect.centerY() + lcdHeight / 2).toFloat(),
                lcdBackgroundPaint
            )
            canvas.drawText(
                displayFormat.format(angleToShow1), (
                        middleX + (displayRect.width() + displayGap) / 2).toFloat(), (
                        displayRect.centerY() + lcdHeight / 2).toFloat(),
                lcdForegroundPaint
            )
            //
            bubble2D!!.setBounds(
                (x - halfBubbleWidth).toInt(),
                (y - halfBubbleHeight).toInt(),
                (x + halfBubbleWidth).toInt(),
                (y + halfBubbleHeight).toInt()
            )
            level2D!!.draw(canvas)
            bubble2D!!.draw(canvas)
            marker2D!!.draw(canvas)
            canvas.drawLine(
                minLevelX.toFloat(), middleY.toFloat(), (
                        middleX - halfMarkerGap).toFloat(), middleY.toFloat(), infoPaint
            )
            canvas.drawLine(
                (middleX + halfMarkerGap).toFloat(), middleY.toFloat(),
                maxLevelX.toFloat(), middleY.toFloat(), infoPaint
            )
            canvas.drawLine(
                middleX.toFloat(), minLevelY.toFloat(),
                middleX.toFloat(), (middleY - halfMarkerGap).toFloat(), infoPaint
            )
            canvas.drawLine(
                middleX.toFloat(), (middleY + halfMarkerGap).toFloat(),
                middleX.toFloat(), maxLevelY.toFloat(), infoPaint
            )
        } else {
            canvas.rotate(orientation.getRotation().toFloat(), middleX.toFloat(), middleY.toFloat())
            // Angle
            display!!.bounds = displayRect
            display!!.draw(canvas)
            canvas.drawText(
                displayBackgroundText, middleX.toFloat(), (
                        displayRect.centerY() + lcdHeight / 2).toFloat(), lcdBackgroundPaint
            )
            canvas.drawText(
                displayFormat.format(angleToShow1), middleX.toFloat(), (
                        displayRect.centerY() + lcdHeight / 2).toFloat(), lcdForegroundPaint
            )
            // level
            level1D!!.draw(canvas)
            // bubble
            canvas.clipRect(
                minLevelX + levelBorderWidth, minLevelY + levelBorderHeight,
                maxLevelX - levelBorderWidth, maxLevelY - levelBorderHeight
            )
            bubble1D!!.setBounds(
                (x - halfBubbleWidth).toInt(), minBubble,
                (x + halfBubbleWidth).toInt(), maxBubble
            )
            bubble1D!!.draw(canvas)
            // marker
            marker1D!!.setBounds(
                middleX - halfMarkerGap - markerThickness, minLevelY,
                middleX - halfMarkerGap, maxLevelY
            )
            marker1D!!.draw(canvas)
            marker1D!!.setBounds(
                middleX + halfMarkerGap, minLevelY,
                middleX + halfMarkerGap + markerThickness, maxLevelY
            )
            marker1D!!.draw(canvas)
        }
        canvas.restore()
    }

    companion object {
        private const val LEVEL_ASPECT_RATIO = 0.150
        private const val BUBBLE_WIDTH = 0.150
        private const val BUBBLE_ASPECT_RATIO = 1.000
        private const val BUBBLE_CROPPING = 0.500
        private const val MARKER_GAP = BUBBLE_WIDTH + 0.020

        /**
         * Angle max
         */
        private val MAX_SINUS = Math.sin(Math.PI / 4)

        /**
         * Fonts and colors
         */
        private const val FONT_LCD = "fonts/lcd.ttf"
        private const val displayBackgroundText = "88.8"
    }
}
