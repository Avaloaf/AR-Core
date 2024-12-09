package com.example.arcore

import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position
import io.github.sceneview.node.VideoNode
import java.io.ByteArrayOutputStream
import java.util.UUID

class ARScreen : AppCompatActivity() {

    private lateinit var sceneView: ArSceneView
    private lateinit var placeButton: Button
    private lateinit var resolveButton: Button
    private lateinit var captureButton: Button // Capture button
    private lateinit var hostAnchorButton: Button // Button to trigger cloud anchor hosting
    private lateinit var modelNode: ArModelNode
    private lateinit var videoNode: VideoNode
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var session: Session
    private var currentAnchor: Anchor? = null

    private val modelList = listOf("chair", "sofa", "dressing_table", "table_football")
    private var selectedModelIndex = 0
    private var receivedAnchorId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get the anchor ID from intent extras (if any)
        receivedAnchorId = intent.getStringExtra("anchorId")

        // Initialize the scene view
        sceneView = findViewById<ArSceneView>(R.id.sceneView).apply {
            this.lightEstimationMode = Config.LightEstimationMode.DISABLED
        }

        // Initialize modelNode early as a placeholder
        modelNode = ArModelNode(sceneView.engine, PlacementMode.INSTANT)
        sceneView.addChild(modelNode)

        // Ensure the AR session is initialized
        sceneView.onArSessionCreated = { arSession ->
            session = arSession
            session.configure(
                Config(session).apply {
                    cloudAnchorMode = Config.CloudAnchorMode.ENABLED
                }
            )

            // If an anchor ID was provided, resolve it
            receivedAnchorId?.let { resolveCloudAnchor(it) }
        }

        // Initialize media player for video node
        mediaPlayer = MediaPlayer.create(this, R.raw.ad)

        // Initialize UI elements
        placeButton = findViewById(R.id.place)
        resolveButton = findViewById(R.id.resolveButton)
        captureButton = findViewById(R.id.captureButton) // Capture Button
        hostAnchorButton = findViewById(R.id.hostAnchorButton) // New button to host cloud anchor

        // Place model without hosting the cloud anchor
        placeButton.setOnClickListener {
            placeModel()
        }

        // Resolve Cloud Anchor when button clicked
        resolveButton.setOnClickListener {
            val anchorId = "EXAMPLE_ANCHOR_ID" // Replace with actual ID
            resolveCloudAnchor(anchorId)
        }

        // Add the new capture button functionality
        captureButton.setOnClickListener {
            captureScreenshotAndUpload() // Capture and upload image
        }

        // Host Cloud Anchor when button clicked
        hostAnchorButton.setOnClickListener {
            currentAnchor?.let {
                hostCloudAnchor(it)
            } ?: Toast.makeText(this, "No model placed to host!", Toast.LENGTH_SHORT).show()
        }

        // Initialize video and model nodes
        videoNode = VideoNode(
            sceneView.engine,
            scaleToUnits = 0.7f,
            centerOrigin = Position(y = -4f),
            glbFileLocation = "models/plane.glb",
            player = mediaPlayer,
            onLoaded = { _, _ -> mediaPlayer.start() }
        )

        loadModel(modelList[selectedModelIndex])

        // Model selection buttons
        findViewById<Button>(R.id.btn_chair).setOnClickListener { selectModel(0) }
        findViewById<Button>(R.id.btn_sofa).setOnClickListener { selectModel(1) }
        findViewById<Button>(R.id.btn_dressing_table).setOnClickListener { selectModel(2) }
        findViewById<Button>(R.id.btn_table_football).setOnClickListener { selectModel(3) }
    }

    private fun placeModel() {
        // Detach the current anchor if it exists
        modelNode.anchor?.detach() // Detach existing anchor
        modelNode.anchor() // Create a new anchor for the current placement
        sceneView.planeRenderer.isVisible = false
    }

    private fun loadModel(modelName: String) {
        sceneView.removeChild(modelNode)
        sceneView.removeChild(videoNode) // Ensure videoNode is detached

        modelNode = ArModelNode(sceneView.engine, PlacementMode.INSTANT).apply {
            loadModelGlbAsync(
                glbFileLocation = "models/$modelName.glb",
                scaleToUnits = 1f,
                centerOrigin = Position(-0.5f)
            ) {
                sceneView.planeRenderer.isVisible = true
            }
            onAnchorChanged = {
                placeButton.isGone = it != null
                currentAnchor = it
            }
        }
        sceneView.addChild(modelNode)

        // Add videoNode only if required
        if (modelName == "sofa") {
            sceneView.addChild(videoNode)
        }
    }

    private fun selectModel(index: Int) {
        if (index in modelList.indices) {
            selectedModelIndex = index
            Toast.makeText(this, "Selected Model: ${modelList[selectedModelIndex]}", Toast.LENGTH_SHORT).show()
            loadModel(modelList[selectedModelIndex])
        }
    }

    private fun hostCloudAnchor(anchor: Anchor) {
        session.hostCloudAnchor(anchor).also { cloudAnchor ->
            monitorCloudAnchorState(cloudAnchor) // Monitor the state as before

            // Save to Firebase when the anchor is successfully hosted
            if (cloudAnchor.cloudAnchorState == Anchor.CloudAnchorState.SUCCESS) {
                val anchorId = cloudAnchor.cloudAnchorId
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                saveAnchorToFirebase(userId, anchorId, "Living room setup")
            }
        }
    }

    private fun resolveCloudAnchor(anchorId: String) {
        session.resolveCloudAnchor(anchorId).also { resolvedAnchor ->
            monitorCloudAnchorState(resolvedAnchor)
        }
    }

    private fun monitorCloudAnchorState(anchor: Anchor) {
        when (anchor.cloudAnchorState) {
            Anchor.CloudAnchorState.SUCCESS -> {
                Toast.makeText(this, "Cloud Anchor hosted/resolved!", Toast.LENGTH_SHORT).show()
                currentAnchor = anchor
            }
            Anchor.CloudAnchorState.ERROR_NOT_AUTHORIZED -> {
                Toast.makeText(this, "Authorization error!", Toast.LENGTH_SHORT).show()
            }
            Anchor.CloudAnchorState.ERROR_SERVICE_UNAVAILABLE -> {
                Toast.makeText(this, "Service unavailable. Try again.", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Log.d("ARScreen", "Cloud Anchor State: ${anchor.cloudAnchorState}")
            }
        }
    }

    private fun saveAnchorToFirebase(userId: String?, anchorId: String?, description: String) {
        if (userId == null || anchorId == null) {
            Toast.makeText(this, "Error: Unable to save anchor!", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val anchorData = mapOf(
            "uid" to userId,
            "anchorId" to anchorId,
            "timestamp" to System.currentTimeMillis(),
            "description" to description
        )

        db.collection("anchors").add(anchorData)
            .addOnSuccessListener {
                Toast.makeText(this, "Anchor saved successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving anchor: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun captureScreenshotAndUpload() {
        try {
            // Capture the bitmap of the AR scene
            val bitmap = Bitmap.createBitmap(sceneView.width, sceneView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            sceneView.draw(canvas)  // Make sure this draws the AR scene to the bitmap

            // Compress the bitmap into a byte array
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val data = baos.toByteArray()

            // Call the function to upload the image data
            uploadToFirebase(data)
        } catch (e: Exception) {
            Log.e("ARScreen", "Error capturing screenshot: ${e.message}")
        }
    }

    private fun uploadToFirebase(imageData: ByteArray) {
        try {
            // Get Firebase Storage reference
            val storageReference: StorageReference = FirebaseStorage.getInstance().reference
            val imageRef = storageReference.child("images/${UUID.randomUUID()}.png")

            // Upload the image data to Firebase Storage
            val uploadTask = imageRef.putBytes(imageData)

            // Success and Failure Listeners
            uploadTask.addOnSuccessListener {
                Log.d("ARScreen", "Image uploaded successfully.")
            }.addOnFailureListener { exception ->
                Log.e("ARScreen", "Image upload failed: ${exception.message}")
            }
        } catch (e: Exception) {
            Log.e("ARScreen", "Error uploading image: ${e.message}")
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
