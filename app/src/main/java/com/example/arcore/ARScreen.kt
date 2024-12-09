package com.example.arcore

import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
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

    private var isModelPlaced = false

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
            val anchorId = receivedAnchorId // Use receivedAnchorId if it's available
            anchorId?.let { resolveCloudAnchor(it) }
        }

        // Add the new capture button functionality
        captureButton.setOnClickListener {
            captureScreenshotAndUpload() // Capture and upload image
        }

        // Host Cloud Anchor when button clicked
        hostAnchorButton.setOnClickListener {
            if (isModelPlaced) {
                currentAnchor?.let {
                    // If the model is placed and there's a valid anchor, host it
                    hostCloudAnchor(it)
                    Toast.makeText(this, "Model is placed!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Model is not placed yet!", Toast.LENGTH_SHORT).show()
            }
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

        // Touch listener for moving the model
        sceneView.setOnTouchListener { _, event ->
            if (!isModelPlaced) {
                handleTouch(event)
            }
            true
        }
    }

    private fun placeModel() {
        // Display a Toast message for indicating the process has started
        Toast.makeText(this, "Placing model...", Toast.LENGTH_SHORT).show()

        // Detach the current anchor if it exists
        modelNode.anchor?.detach()

        // Check if the AR session has a valid plane to place the anchor
        val frame = session.update()
        val hitTestResults = frame.hitTest(sceneView.width / 2f, sceneView.height / 2f)

        // If hit test returns a valid result, create an anchor
        if (hitTestResults.isNotEmpty()) {
            val hitResult = hitTestResults[0]  // Pick the first valid hit result
            val pose = hitResult.hitPose
            currentAnchor = session.createAnchor(pose)  // Create the anchor and assign it to currentAnchor

            // Create a new ArModelNode and associate it with the newly created anchor
            modelNode.anchor = currentAnchor
            sceneView.addChild(modelNode)

            // Show a success message using a Toast
            Toast.makeText(this, "Anchor placed successfully!", Toast.LENGTH_SHORT).show()

            // Mark model as placed
            isModelPlaced = true
        } else {
            // Show a failure message using a Toast
            Toast.makeText(this, "Failed to place anchor. Try again.", Toast.LENGTH_SHORT).show()
        }
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

    private fun handleTouch(event: MotionEvent) {
        // Perform hit testing to get a new pose based on touch event location
        val frame = session.update()
        val hitTestResults = frame.hitTest(event.x, event.y)

        if (hitTestResults.isNotEmpty()) {
            val hitResult = hitTestResults[0]  // Pick the first valid hit result
            val pose = hitResult.hitPose

            // Update model position to follow the touch
            modelNode.position = Position(pose.tx(), pose.ty(), pose.tz())
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
        // Host the cloud anchor
        val cloudAnchor = session.hostCloudAnchor(anchor)
        val handler = Handler() // Used to manage timeout and retries
        val startTime = System.currentTimeMillis() // Record the start time
        val timeout = 10000L // Timeout in milliseconds (10 seconds)

        // Runnable to check the cloud anchor state periodically
        val checkStateRunnable = object : Runnable {
            override fun run() {
                when (cloudAnchor.cloudAnchorState) {
                    Anchor.CloudAnchorState.SUCCESS -> {
                        // Cloud Anchor hosted successfully
                        val cloudAnchorId = cloudAnchor.cloudAnchorId
                        val userId = FirebaseAuth.getInstance().currentUser?.uid
                        saveAnchorToFirebase(userId, cloudAnchorId, "Anchor hosted successfully")
                        Toast.makeText(this@ARScreen, "Cloud Anchor hosted successfully!", Toast.LENGTH_SHORT).show()
                        handler.removeCallbacks(this) // Stop the timeout checking
                    }
                    Anchor.CloudAnchorState.ERROR_NOT_AUTHORIZED -> {
                        Toast.makeText(this@ARScreen, "Authorization error!", Toast.LENGTH_SHORT).show()
                        handler.removeCallbacks(this) // Stop the timeout checking
                    }
                    Anchor.CloudAnchorState.ERROR_SERVICE_UNAVAILABLE -> {
                        Toast.makeText(this@ARScreen, "Service unavailable. Try again.", Toast.LENGTH_SHORT).show()
                        handler.removeCallbacks(this) // Stop the timeout checking
                    }
                    else -> {
                        // Check timeout
                        if (System.currentTimeMillis() - startTime > timeout) {
                            Toast.makeText(this@ARScreen, "Hosting timed out. Please try again.", Toast.LENGTH_SHORT).show()
                            handler.removeCallbacks(this) // Stop further retries
                        } else {
                            // Retry checking the anchor state
                            handler.postDelayed(this, 1000) // Check every second
                        }
                    }
                }
            }
        }

        // Start the periodic state check
        handler.post(checkStateRunnable)
    }

    private fun resolveCloudAnchor(anchorId: String) {
        session.resolveCloudAnchor(anchorId).also { resolvedAnchor ->
            monitorCloudAnchorState(resolvedAnchor)
        }
    }

    private fun monitorCloudAnchorState(anchor: Anchor) {
        // Monitor the state of the anchor and display status messages accordingly
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

        Log.d("ARScreen", "Saving anchor: UserId=$userId, AnchorId=$anchorId, Description=$description")

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
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save anchor.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun captureScreenshotAndUpload() {
        val bitmap = captureScreenshot()
        uploadImage(bitmap)
    }

    private fun captureScreenshot(): Bitmap {
        val bitmap = Bitmap.createBitmap(sceneView.width, sceneView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        sceneView.draw(canvas)
        return bitmap
    }

    private fun uploadImage(bitmap: Bitmap) {
        val storageReference: StorageReference = FirebaseStorage.getInstance().reference.child("images/${UUID.randomUUID()}.png")
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val data = byteArrayOutputStream.toByteArray()

        storageReference.putBytes(data).addOnSuccessListener {
            Toast.makeText(this, "Screenshot uploaded!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to upload image.", Toast.LENGTH_SHORT).show()
        }
    }
}
