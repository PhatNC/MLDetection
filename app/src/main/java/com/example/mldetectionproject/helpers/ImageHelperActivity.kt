package com.example.mldetectionproject.helpers

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import com.example.mldetectionproject.R
import com.example.mldetectionproject.ml.KangarooDetector
import com.example.mldetectionproject.ml.MobilenetV110224Quant
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.SupportPreconditions
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.io.InputStream

class ImageHelperActivity : AppCompatActivity() {
    private lateinit var resultTextView: TextView
    private lateinit var pickImage: FloatingActionButton
    private lateinit var selectedImage: AppCompatImageView
    private lateinit var imageLabeler: ImageLabeler
    private lateinit var bitmap: Bitmap
    
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
        resultTextView = findViewById(R.id.textView)
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
                            bitmap = BitmapFactory.decodeStream(inputStream)
                            inputStream?.close()

                            if (bitmap != null) {
                                // Use the loaded bitmap
                                runClassification()
                            } else {
                                // Handle the case where the bitmap couldn't be loaded
                            }
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

    private fun runClassification() {
        try {
            val model = MobilenetV110224Quant.newInstance(this@ImageHelperActivity)

            // Load the input image from a file or another source
            val inputBitmap: Bitmap = bitmap // Replace this with your actual loading code

            // Resize the input image to match the model's expected size
            val resizedBitmap = Bitmap.createScaledBitmap(inputBitmap, 224, 224, true)

            // Create inputs for reference.
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.UINT8)
            inputFeature0.loadBuffer(TensorImage.fromBitmap(resizedBitmap).buffer)

            // Runs model inference and gets the result.
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer

            val labelIndex = getMax(outputFeature0.floatArray)

            var labels = arrayOf<String>()

            try {
                val inputStream: InputStream = assets.open("labels.txt")
                val lineList = mutableListOf<String>()

                inputStream.bufferedReader().forEachLine { lineList.add(it) }
                labels = lineList.toTypedArray()

            } catch (e: IOException) {
                e.printStackTrace()
                // Handle the exception
            }

            resultTextView.text = labels[labelIndex]
            // Releases model resources if no longer used.
            model.close()
        } catch (e: IllegalArgumentException) {
            // Handle the specific exception
            e.printStackTrace() // Print the exception for debugging
            // Add your custom error handling logic here
        }
    }

    private fun runClassificationMLKit() {
        // Using imageLabeler - mlkit model
        val inputImage: InputImage = InputImage.fromBitmap(bitmap, 0)
        imageLabeler.process(inputImage)
            .addOnSuccessListener { labels ->
                // Handle the labels here
                print("labels")
                println(labels)
                var tempStrResult: String = ""

                for (label in labels) {
                    val labelText = label.text
                    val confidence = label.confidence
                    // Process label information as needed

                    tempStrResult += "label: $labelText confidence: $confidence \n"
                }

                println("tempStrResult $tempStrResult")

                resultTextView.text = tempStrResult
            }
            .addOnFailureListener { e ->
                // Handle any errors here
                e.printStackTrace()
            }
    }

    private fun computeFlatSize(shape: IntArray): Int {
        SupportPreconditions.checkNotNull(shape, "Shape cannot be null.")
        var prod = 1
        val var3 = shape.size
        for (var4 in 0 until var3) {
            val s = shape[var4]
            prod *= s
        }
        return prod
    }

    private fun runClassificationCustomModel() {
        try {

            val model = KangarooDetector.newInstance(this@ImageHelperActivity)

            // Creates inputs for reference.
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 320, 320, 3), DataType.FLOAT32)

            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 320, 320, true)
            val image = TensorImage(DataType.FLOAT32)
            image.load(resizedBitmap)

//            val shapeString = inputFeature0.shape.joinToString(", ")
//            println("shapeString $shapeString")
//            println("computeFlatSize ${computeFlatSize(inputFeature0.shape)}")
//            println("inputFeature0 typeSize ${inputFeature0.typeSize} flatSize ${inputFeature0.flatSize} buffer.limit ${image.buffer.limit()}")

            inputFeature0.loadBuffer(image.buffer)



            // Runs model inference and gets result.
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer
            val outputFeature1 = outputs.outputFeature1AsTensorBuffer
            val outputFeature2 = outputs.outputFeature2AsTensorBuffer
            val outputFeature3 = outputs.outputFeature3AsTensorBuffer

            val output0 = outputFeature0.floatArray
            val output1 = outputFeature1.floatArray
            val output2 = outputFeature2.floatArray
            val output3 = outputFeature3.floatArray

            println("--OUTPUT 0--")
            logFloatArray(output0)
            println("--OUTPUT 1--")
            logFloatArray(output1)
            println("--OUTPUT 2--")
            logFloatArray(output2)
            println("--OUTPUT 3--")
            logFloatArray(output3)

            try {
                val imW= bitmap.width
                val imH = bitmap.height

                println("imageWidth: $imW imgHeight: $imH")
                // Define the minimum confidence threshold for detections
                val minConf = 0.6

                // Access the output arrays you've obtained
                val boundingBoxes = output0 // Adjust as needed
                val classIndices = output1 // Adjust as needed
                val scores = output2 // Adjust as needed

                val detections = mutableListOf<Rect>()

                for (i in boundingBoxes.indices step 4) {
                    println("i: $i")
                    val score = scores[i / 4]
                    println("score: $score")
                    if (score > minConf) {
                        val left = (boundingBoxes[i] * imW).toInt()
                        val top = (boundingBoxes[i + 1] * imH).toInt()
                        val right = (boundingBoxes[i + 2] * imW).toInt()
                        val bottom = (boundingBoxes[i + 3] * imH).toInt()

                        // Draw bounding box and label
//                        Imgproc.rectangle(image, Point(left.toDouble(), top.toDouble()), Point(right.toDouble(), bottom.toDouble()), Scalar(10.0, 255.0, 0.0), 2)
                        val objectIndex = classIndices[i / 4].toInt() // Replace with your labels array
                        val label = "$objectIndex: ${(score * 100).toInt()}%"
//                        Imgproc.putText(image, label, Point(left.toDouble(), (top - 7).toDouble()), Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, Scalar(0.0, 0.0, 0.0), 2)
                        detections.add(Rect(left, top, right - left, bottom - top))

                        val textStr = "left $left, top $top, right $right, bottom $bottom"
                        resultTextView.text = textStr
                    }
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
                e.printStackTrace()
            }


            // Releases model resources if no longer used.
            model.close()

        } catch (e: IllegalArgumentException) {
            // Handle the specific exception
            e.printStackTrace() // Print the exception for debugging
            // Add your custom error handling logic here
        }
    }
    private fun getMax(arr: FloatArray): Int {
        var max = 0
        for (i in arr.indices) {
            // Your code here
            if (arr[i] > arr[max]) {
                max = i
            }
        }
        return max
    }

    private fun logFloatArray(floatArray: FloatArray) {
        println("Logging FloatArray:")
        for (value in floatArray) {
            println(value)
        }
    }

}
