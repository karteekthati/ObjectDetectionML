package kk.objectdetection.ml

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import kk.objectdetection.ml.databinding.ActivityMainBinding
import kk.objectdetection.ml.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    val paint = Paint()
    lateinit var bitmap: Bitmap
    lateinit var cameraDevice: CameraDevice
    lateinit var cameraManager: CameraManager
    lateinit var handler: Handler
    lateinit var model: SsdMobilenetV11Metadata1
    lateinit var imageProcessor: ImageProcessor
    lateinit var labels: List<String>

    var colors = listOf<Int>(
        Color.BLACK, Color.BLUE, Color.BLACK, Color.RED, Color.GRAY, Color.GREEN,
        Color.YELLOW, Color.DKGRAY, Color.MAGENTA, Color.CYAN
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
    }

    private fun initViews() {

        labels = FileUtil.loadLabels(this, "labels.txt")
        model = SsdMobilenetV11Metadata1.newInstance(this@MainActivity)

        imageProcessor =
            ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()

        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        surfaceListener()

    }

    private fun surfaceListener() {
        binding.tvHello.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {

            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                bitmap = binding.tvHello.bitmap!!

                processAndShowData(bitmap)
            }

        }
    }

    private fun processAndShowData(bitmap: Bitmap) {

// Creates inputs for reference.
        var image = TensorImage.fromBitmap(bitmap)
        image = imageProcessor.process(image)

// Runs model inference and gets result.
        val outputs = model.process(image)
        val locations = outputs.locationsAsTensorBuffer.floatArray
        val classes = outputs.classesAsTensorBuffer.floatArray
        val scores = outputs.scoresAsTensorBuffer.floatArray
        val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer

        var mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)

        val h = mutable.height
        val w = mutable.width

        paint.textSize = h / 15f
        paint.strokeWidth = h / 85f

        var x = 0
        scores.forEachIndexed { index, fl ->
            x = index
            x *= 4

            if (fl > 0.5) {
                paint.color = colors[index]
                paint.style = Paint.Style.STROKE
                canvas.drawRect(
                    RectF(
                        locations[x + 1] * w, locations[x] * h, locations[x + 3] * w,
                        locations[x + 2] * h
                    ), paint
                )
                paint.style = Paint.Style.FILL
                canvas.drawText(
                    labels.get(classes[index].toInt()) + " " +
                            fl.toString(), locations[x + 1] * w, locations[x] * h, paint
                )
            }

        }

        binding.imgView.setImageBitmap(mutable)
    }


    @SuppressLint("MissingPermission")
    private fun openCamera() {
        cameraManager.openCamera(
            cameraManager.cameraIdList[0],
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera

                    var surfaceTexture = binding.tvHello.surfaceTexture
                    var surface = Surface(surfaceTexture)
                    var captureRequest =
                        cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    captureRequest.addTarget(surface)

                    cameraDevice.createCaptureSession(
                        listOf(surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                session.setRepeatingRequest(captureRequest.build(), null, null)
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                TODO("Not yet implemented")
                            }

                        },
                        handler
                    )
                }

                override fun onDisconnected(camera: CameraDevice) {
                    // Handle the case where the camera is disconnected
                    // You might want to close the session and release resources here
                    cameraDevice?.close()
                    model.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    // Handle camera errors
                    // You can log the error or take appropriate action based on the error code
                    when (error) {
                        CameraDevice.StateCallback.ERROR_CAMERA_DEVICE -> {
                            // Handle device-level errors
                        }
                        CameraDevice.StateCallback.ERROR_CAMERA_DISABLED -> {
                            // Handle cases where the camera is disabled
                        }
                        // Add more error handling cases as needed
                        else -> {
                            // Handle other errors
                        }
                    }
                    // Close the camera device and release resources
                    cameraDevice?.close()
                    model.close()
                }

            },
            handler
        )

    }


    override fun onDestroy() {
        super.onDestroy()
// Releases model resources if no longer used.
        model.close()
    }

}