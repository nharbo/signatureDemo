package dk.nodes.signaturedemo

import android.content.Context
import android.content.pm.ActivityInfo
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.view.MotionEvent
import android.widget.Toast
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    var mClear: Button? = null
    var mGetSign: Button? = null

    var finalSignatureIV: ImageView? = null

    var file: File? = null
    var mContent: LinearLayout? = null
    var view: View? = null
    var mSignature: signature? = null
    var bitmap: Bitmap? = null

    private val STROKE_WIDTH = 5f
    private val HALF_STROKE_WIDTH = STROKE_WIDTH / 2

    var pic_name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

        finalSignatureIV = findViewById(R.id.signatureIV) as ImageView

        mContent = findViewById<View>(R.id.canvasLayout) as LinearLayout
        mSignature = signature(applicationContext, null)
        mSignature?.setBackgroundColor(Color.WHITE)
        // Dynamically generating Layout through java code
        mContent?.addView(mSignature, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        mClear = findViewById<View>(R.id.clear) as Button
        mGetSign = findViewById<View>(R.id.getsign) as Button
        mGetSign?.setEnabled(false)

        view = mContent

        file = File(filesDir.path, pic_name + ".png")

        mClear?.setOnClickListener {
            Log.v("log_tag", "Panel Cleared")
            mSignature?.clear()
            mGetSign?.setEnabled(false)
        }

        mGetSign?.setOnClickListener {
            getImage()
        }

    }

    fun getImage() {
        view?.let {
            it.isDrawingCacheEnabled = true
//                    mSignature?.save(it, storedPath)
            mSignature?.getImageAsPng(it)
        }

        Toast.makeText(applicationContext, "Successfully Saved", Toast.LENGTH_SHORT).show()
        // Calling the same class
//        recreate()
    }


    inner class signature(context: Context, attrs: AttributeSet?) : View(context, attrs) {
        private val paint = Paint()
        private val path = Path()

        private var lastTouchX: Float = 0.toFloat()
        private var lastTouchY: Float = 0.toFloat()
        private val dirtyRect = RectF()

        init {
            paint.setAntiAlias(true)
            paint.setColor(Color.BLACK)
            paint.setStyle(Paint.Style.STROKE)
            paint.setStrokeJoin(Paint.Join.ROUND)
            paint.setStrokeWidth(STROKE_WIDTH)
        }

        fun getImageAsPng(v: View) {
            Log.v("log_tag", "Width: " + v.width)
            Log.v("log_tag", "Height: " + v.height)

            if (bitmap == null) {
                mContent?.let {
                    bitmap = Bitmap.createBitmap(it.width, it.height, Bitmap.Config.RGB_565)
                }
            }

            val canvas = Canvas(bitmap)

            try {
                // Output the file
                val mFileOutStream = FileOutputStream(file)
                v.draw(canvas)

                finalSignatureIV?.setImageBitmap(bitmap)

                mFileOutStream.flush()
                mFileOutStream.close()

//                // Convert the output file to .png
//                bitmap!!.compress(Bitmap.CompressFormat.PNG, 90, mFileOutStream)

            } catch (e: Exception) {
                Log.v("log_tag", e.toString())
            }

        }

        fun clear() {
            path.reset()
            invalidate()
            mGetSign?.setEnabled(false)
        }

        override fun onDraw(canvas: Canvas) {
            canvas.drawPath(path, paint)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val eventX = event.x
            val eventY = event.y
            mGetSign?.setEnabled(true)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    path.moveTo(eventX, eventY)
                    lastTouchX = eventX
                    lastTouchY = eventY
                    return true
                }

                MotionEvent.ACTION_MOVE,

                MotionEvent.ACTION_UP -> {

                    resetDirtyRect(eventX, eventY)
                    val historySize = event.historySize
                    for (i in 0 until historySize) {
                        val historicalX = event.getHistoricalX(i)
                        val historicalY = event.getHistoricalY(i)
                        expandDirtyRect(historicalX, historicalY)
                        path.lineTo(historicalX, historicalY)
                    }
                    path.lineTo(eventX, eventY)
                }

                else -> {
                    debug("Ignored touch event: " + event.toString())
                    return false
                }
            }

            invalidate(
                (dirtyRect.left - HALF_STROKE_WIDTH).toInt(),
                (dirtyRect.top - HALF_STROKE_WIDTH).toInt(),
                (dirtyRect.right + HALF_STROKE_WIDTH).toInt(),
                (dirtyRect.bottom + HALF_STROKE_WIDTH).toInt()
            )

            lastTouchX = eventX
            lastTouchY = eventY

            return true
        }

        private fun debug(string: String) {

            Log.v("log_tag", string)

        }

        private fun expandDirtyRect(historicalX: Float, historicalY: Float) {
            if (historicalX < dirtyRect.left) {
                dirtyRect.left = historicalX
            } else if (historicalX > dirtyRect.right) {
                dirtyRect.right = historicalX
            }

            if (historicalY < dirtyRect.top) {
                dirtyRect.top = historicalY
            } else if (historicalY > dirtyRect.bottom) {
                dirtyRect.bottom = historicalY
            }
        }

        private fun resetDirtyRect(eventX: Float, eventY: Float) {
            dirtyRect.left = Math.min(lastTouchX, eventX)
            dirtyRect.right = Math.max(lastTouchX, eventX)
            dirtyRect.top = Math.min(lastTouchY, eventY)
            dirtyRect.bottom = Math.max(lastTouchY, eventY)
        }

    }

}
