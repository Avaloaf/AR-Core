package com.example.arcore

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import com.google.ar.core.Config
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.math.Position
import io.github.sceneview.ar.node.PlacementMode
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import io.github.sceneview.node.VideoNode
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import android.util.Log
import java.io.ByteArrayOutputStream
import java.util.UUID
import android.widget.Toast
import android.widget.Button

class ARScreen : AppCompatActivity() {

    private lateinit var sceneView: ArSceneView
    private lateinit var placeButton: ExtendedFloatingActionButton
    private lateinit var modelNode: ArModelNode
    private lateinit var videoNode: VideoNode
    private lateinit var mediaPlayer: MediaPlayer

    // Define a list of available models
    private val modelList = listOf("chair", "sofa", "dressing_table", "table_football")
    private var selectedModelIndex = 0  // Default to the first model

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sceneView = findViewById<ArSceneView>(R.id.sceneView).apply {
            this.lightEstimationMode = Config.LightEstimationMode.DISABLED
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.ad)

        placeButton = findViewById(R.id.place)

        placeButton.setOnClickListener {
            placeModel()
            captureScreenshotAndUpload()  // Capture screenshot after placing model
        }

        videoNode = VideoNode(
            sceneView.engine,
            scaleToUnits = 0.7f,
            centerOrigin = Position(y = -4f),
            glbFileLocation = "models/plane.glb",
            player = mediaPlayer,
            onLoaded = { _, _ -> mediaPlayer.start() }
        )

        // Initially load the first model (chair)
        loadModel(modelList[selectedModelIndex])

        modelNode = ArModelNode(sceneView.engine, PlacementMode.INSTANT).apply {
            loadModelGlbAsync(
                glbFileLocation = "models/chair.glb", // Default model (chair)
                scaleToUnits = 1f,
                centerOrigin = Position(-0.5f)
            ) {
                sceneView.planeRenderer.isVisible = true
            }
            onAnchorChanged = {
                placeButton.isGone = it != null
            }
        }

        sceneView.addChild(modelNode)
        modelNode.addChild(videoNode)

        // Add model selection buttons
        findViewById<Button>(R.id.btn_chair).setOnClickListener {
            selectModel(0)  // Chair model selected
        }
        findViewById<Button>(R.id.btn_sofa).setOnClickListener {
            selectModel(1)  // Sofa model selected
        }
        findViewById<Button>(R.id.btn_dressing_table).setOnClickListener {
            selectModel(2)  // Dressing table model selected
        }
        findViewById<Button>(R.id.btn_table_football).setOnClickListener {
            selectModel(3)  // Table football model selected
        }
    }

    private fun placeModel() {
        modelNode.anchor()
        sceneView.planeRenderer.isVisible = false
    }

    // Function to dynamically load the selected model
    private fun loadModel(modelName: String) {
        modelNode = ArModelNode(sceneView.engine, PlacementMode.INSTANT).apply {
            loadModelGlbAsync(
                glbFileLocation = "models/$modelName.glb",  // Load model based on selected name
                scaleToUnits = 1f,
                centerOrigin = Position(-0.5f)
            ) {
                sceneView.planeRenderer.isVisible = true
            }
            onAnchorChanged = {
                placeButton.isGone = it != null
            }
        }
        sceneView.addChild(modelNode)
        modelNode.addChild(videoNode)
    }

    // Function to change the selected model (called by buttons)
    private fun selectModel(index: Int) {
        if (index in modelList.indices) {
            selectedModelIndex = index
            Toast.makeText(this, "Selected Model: ${modelList[selectedModelIndex]}", Toast.LENGTH_SHORT).show()
            loadModel(modelList[selectedModelIndex])  // Load the new model
        }
    }

    // Function to capture screenshot and upload to Firebase
    private fun captureScreenshotAndUpload() {
        // Capture screenshot from the AR scene
        val bitmap = Bitmap.createBitmap(sceneView.width, sceneView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        sceneView.draw(canvas)

        // Convert the Bitmap to a byte array
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val data = baos.toByteArray()

        // Upload the image to Firebase Storage
        uploadToFirebase(data)
    }

    private fun uploadToFirebase(imageData: ByteArray) {
        val storageReference: StorageReference = FirebaseStorage.getInstance().reference
        val imageRef = storageReference.child("images/${UUID.randomUUID()}.png")

        // Upload the image
        val uploadTask = imageRef.putBytes(imageData)
        uploadTask.addOnSuccessListener {
            Log.d("ARScreen", "Image uploaded successfully.")
        }.addOnFailureListener { exception ->
            Log.e("ARScreen", "Image upload failed: ${exception.message}")
        }
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}
