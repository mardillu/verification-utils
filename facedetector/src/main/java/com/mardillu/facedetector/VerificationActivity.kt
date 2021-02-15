package com.mardillu.facedetector

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import com.otaliastudios.cameraview.CameraException
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraOptions
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.controls.Mode
import com.mardillu.facedetector.FaceDetector.OnFaceDetectionResultListener
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import kotlin.properties.Delegates

class VerificationActivity : AppCompatActivity() {

    public var imageName: String? = ""
    public var farmerName: String? = ""
    public var phoneNumber: String? = ""
    public var farmerIdString: String? = ""
    public var modelPath: String? = ""
    public var threshHold by Delegates.notNull<Double>()
    public var farmerContext by Delegates.notNull<Int>()
    var frameCount = 0;

    lateinit var dialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        imageName = this.intent.getStringExtra("image_name")
        farmerContext = intent.getIntExtra("image_context", 1)
        farmerName = this.intent.getStringExtra("farmer_name")
        phoneNumber = this.intent.getStringExtra("farmer_phone_number")
        modelPath = this.intent.getStringExtra("model_name")
        threshHold = intent.getDoubleExtra("threshold", 0.70)
        farmerIdString = this.intent.getStringExtra("farmer_id_string")

        setupCamera()
    }

    override fun onResume() {
        super.onResume()
        //updateCountDownTimer()
        viewfinder.mode = Mode.PICTURE
        viewfinder.open()
    }

    override fun onPause() {
        super.onPause()
        //countDownTimer?.cancel()
        viewfinder.close()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        hideDialog()
        viewfinder.destroy()
    }

    private fun setupCamera() {
        val faceDetector = FaceDetector(faceBoundsOverlay)
        faceDetector.setonFaceDetectionFailureListener(resultListener)
        viewfinder.addCameraListener(cameraListener)
        viewfinder.addFrameProcessor {
            faceDetector.process(
                    Frame(
                            data = it.getData(),
                            rotation = it.rotationToUser,
                            size = Size(it.size.width, it.size.height),
                            format = it.format,
                            lensFacing = LensFacing.BACK
                    )
            )
        }
    }

    private fun showDialog(){
        dialog = ProgressDialog(this)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setMessage("Verifying farmer face, please wait...")

        dialog.show()
    }

    private fun hideDialog(){
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    fun verifyFace(cameraFaceBitmap: Bitmap?, options: BitmapFactory.Options){
        showDialog()

        var subLoc = if (farmerContext==1)
            "Images"
        else
            "reference_Images"

        val farmerImage = Environment.getExternalStorageDirectory()
                .toString() + "/MERGDATA/" + subLoc +"/" + imageName

        val farmerFaceBitmap: Bitmap = BitmapFactory.decodeFile(farmerImage, options)
        if (cameraFaceBitmap != null){
            verifyFace(cameraFaceBitmap, farmerFaceBitmap)
        }else{
            frameCount = 0
        }
    }

    private fun verifyFace(face1Bitmap: Bitmap?, face2Bitmap: Bitmap?) {
        Thread {
            try {
                saveImageToDevice(face1Bitmap)
                val facenet: FaceNet = if (modelPath == null){
                    FaceNet(assets)
                }else{
                    FaceNet(modelPath)
                }
                //val mtcnn = MTCNN(activity?.assets)
                //val face1: Bitmap = facenet.cropFace(face1Bitmap, mtcnn)
                //val face2: Bitmap = facenet.cropFace(face2Bitmap, mtcnn)
                // To make sure both faces were detected successfully
                val score: Double = facenet.computeCosineSimilarity(
                        face1Bitmap,
                        face2Bitmap
                ) // cosine similarity between the face descriptor vectors
                Log.d("TAG", "verifyFace: SCORE $score")
                //mtcnn.close()
                facenet.close()
                sendResultSuccess(score)
            } catch (e: Exception) {
                e.printStackTrace()
                sendResultError(e.message)
            }
        }.start()
    }

    public fun sendResultSuccess(score: Double){
        var it = Intent(this, VerificationActivity::class.java)
        it.putExtra("score", score)
        it.putExtra("farmer_id_string", farmerIdString)
        if (score >= threshHold){
            it.putExtra("status", "success")
        }else{
            it.putExtra("status", "failed")
        }
        setResult(RESULT_OK, it)
        finish()
    }

    public fun sendResultError(message: String?){
        var it = Intent(this, VerificationActivity::class.java)
        it.putExtra("score", 0.0)
        it.putExtra("status", "failed")
        it.putExtra("message", message)
        it.putExtra("farmer_id_string", farmerIdString)
        setResult(RESULT_OK, it)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val it = Intent(this, VerificationActivity::class.java)
        it.putExtra("score", -1.0)
        it.putExtra("status", "failed")
        it.putExtra("message", "Canceled")
        it.putExtra("farmer_id_string", farmerIdString)
        setResult(RESULT_CANCELED, it)
        finish()
    }

    fun saveImageToDevice(bitmap: Bitmap?){
        val root = Environment.getExternalStorageDirectory().toString()
        val myDir = File("$root/MERGDATA/Images")
        myDir.mkdirs()
        val file = File(myDir, "temp_face_image.jpg")

        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    var resultListener: OnFaceDetectionResultListener = object : OnFaceDetectionResultListener {
        override fun onSuccess(faceBounds: List<FaceBounds>) {
            viewfinder.takePicture()
        }
        override fun onFailure(exception: Exception) {
            //send error
        }
    }

    var cameraListener: CameraListener = object : CameraListener() {
        override fun onCameraOpened(options: CameraOptions) {
            super.onCameraOpened(options)
        }

        override fun onCameraClosed() {
            super.onCameraClosed()
        }

        override fun onCameraError(exception: CameraException) {
            super.onCameraError(exception)
        }

        override fun onPictureTaken(result: PictureResult) {
            super.onPictureTaken(result)
            if (frameCount++ != 5){
                return
            }
            result.toBitmap {
                verifyFace(it, BitmapFactory.Options())
            }
        }
    }
}
