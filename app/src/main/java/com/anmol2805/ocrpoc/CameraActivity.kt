package com.anmol2805.ocrpoc

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.SparseArray
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.lang.StringBuilder
import kotlin.properties.Delegates


class CameraActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CAMERA = 100
    private val REQUEST_TAKE_PHOTO = 1
    private var photoFile: File? = null
    private var textRecognizer by Delegates.notNull<TextRecognizer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        textRecognizer = TextRecognizer.Builder(this).build()
        clickImage.setOnClickListener {
            openCamera()
        }
    }

    private fun openCamera() {
        if (isCameraPermissionGranted()) {
            clickImageFromCamera()
        } else {
            requestForPermission()
        }
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestForPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CAMERA
        )
    }

    private fun clickImageFromCamera() {
        val takePicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePicture.resolveActivity(packageManager) != null) {
            // Create the File where the photo should go
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                ex.printStackTrace()
                // Error occurred while creating the File
            }
            if (photoFile != null) {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this, fileProvider(packageName),
                    photoFile!!
                )
                takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePicture, REQUEST_TAKE_PHOTO)
            }
        }
    }


    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode != PERMISSION_REQUEST_CAMERA) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (isCameraPermissionGranted()) {
                clickImageFromCamera()
            } else {
                Toast.makeText(this, "Permissions needed", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    @Throws(IOException::class)
    fun createImageFile(): File? {
        // Create an image file name
        val timeStamp: String = System.currentTimeMillis().toString()

        val mFileName = "Test_${timeStamp}_"
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(mFileName, ".jpg", storageDir)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_TAKE_PHOTO -> {
                    val options = BitmapFactory.Options()
                    var bitmap =
                        BitmapFactory.decodeFile(photoFile?.absolutePath, options)

                    val exif = ExifInterface(photoFile?.absolutePath)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
                    val matrix = Matrix()
                    when (orientation) {
                        6 -> {
                            matrix.postRotate(90F)
                        }
                        3 -> {
                            matrix.postRotate(180F)
                        }
                        8 -> {
                            matrix.postRotate(270F)
                        }
                    }
                    bitmap = bitmap?.let { res ->
                        Bitmap.createBitmap(
                            res, 0, 0, res.width, res.height,
                            matrix, true
                        )
                    }

                    imageView.setImageBitmap(bitmap)

                    if (!textRecognizer.isOperational) {
                        Toast.makeText(
                            this,
                            "Dependencies are not loaded yet...please try after few moment!!",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d("TAG", "Dependencies are downloading....try after few moment")
                    } else
                        bitmap?.let {
                            val frame = Frame.Builder().setBitmap(it).build()
                            val items = textRecognizer.detect(frame)
                            if (items.size() == 0) {
                                return
                            }
                            val sb = StringBuilder()
                            for (i in 0 until items.size()) {
                                val item = items.valueAt(i)
                                sb.append(item.value)
                                sb.append("\n")
                            }
                            val intent = Intent(this, MainActivity::class.java)
                            intent.putExtra("image", sb.toString())
                            startActivity(intent)
                        }
                }

            }
        }
    }

}