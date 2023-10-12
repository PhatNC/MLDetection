package com.example.mldetectionproject.helpers

import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageView
import com.example.mldetectionproject.R
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.IOException

class ImageHelperActivity : AppCompatActivity() {
    private lateinit var pickImage: FloatingActionButton
    private lateinit var selectedImage: AppCompatImageView
    private lateinit var imageLabeler: ImageLabeler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_helper)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_IMAGES), 0)
            }

            if (checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_VIDEO), 0)
            }
        } else {
            // Android 7.0+ to before 13
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
                }
            }
        }

        pickImage = findViewById(R.id.pick_image)
        selectedImage = findViewById(R.id.selected_image)

        pickImage.setOnClickListener {
            val pickImg = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            changeImage.launch(pickImg)
        }

        imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.7f)
            .build())
    }

    private val changeImage =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            println("HEHEHE")
            println(it)
            if (it.resultCode == Activity.RESULT_OK) {
                val data = it.data
                val imgUri = data?.data
                selectedImage.setImageURI(imgUri)

                // Ensure that the imgUri is not null before proceeding
                if (imgUri != null) {
                    try {
                        val contentResolver = applicationContext.contentResolver
                        val inputStream = contentResolver.openInputStream(imgUri)

                        if (inputStream != null) {
                            println("HEHEHEH")
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            // Now, 'bitmap' contains the decoded image from 'imgUri'
                            // You can use 'bitmap' as needed
                            runClassification(bitmap)

                        } else {
                            // Handle the case where 'inputStream' is null
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        // Handle any exceptions that may occur during image decoding
                    }
                }
            }
        }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(ImageHelperActivity::class.java.simpleName, "Grant result for ${permissions[0]} is ${grantResults[0]}")
    }

    private fun runClassification(bitmap: Bitmap) {
        println("runClassification")
        println(bitmap)
        val inputImage: InputImage = InputImage.fromBitmap(bitmap, 0)
        imageLabeler.process(inputImage)
            .addOnSuccessListener { labels ->
                // Handle the labels here
                print("labels")
                println(labels)
                for (label in labels) {
                    val labelText = label.text
                    val confidence = label.confidence
                    // Process label information as needed

                    println("labelText $labelText")
                    println("labelText $confidence")
                }
            }
            .addOnFailureListener { e ->
                // Handle any errors here
                e.printStackTrace()
            }
    }

}