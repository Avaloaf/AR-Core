package com.example.arcore

import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
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
import java.util.function.BiConsumer


class ARScreen : AppCompatActivity() {

    private lateinit var sceneView: ArSceneView
    private lateinit var placeButton: Button
    private lateinit var resolveButton: Button
    private lateinit var captureButton: Button
    private lateinit var hostAnchorButton: Button
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
            receivedAnchorId?.let { resolveCloudAnchorAsync(it) }
        }

        // Initialize media player for video node
        mediaPlayer = MediaPlayer.create(this, R.raw.ad)

        // Initialize UI elements
        placeButton = findViewById(R.id.place)
        resolveButton = findViewById(R.id.resolveButton)
        captureButton = findViewById(R.id.captureButton)
        hostAnchorButton = findViewById(R.id.hostAnchorButton)

        // Place model without hosting the cloud anchor
        placeButton.setOnClickListener {
            placeModel()
        }

        // Resolve Cloud Anchor when button clicked
        resolveButton.setOnClickListener {
            val anchorId = receivedAnchorId
            anchorId?.let { resolveCloudAnchorAsync(it) }
        }

        // Add the new capture button functionality
        captureButton.setOnClickListener {
            captureScreenshotAndUpload()
        }

        // Host Cloud Anchor when button clicked
        hostAnchorButton.setOnClickListener {
            if (isModelPlaced && currentAnchor != null) {
                hostCloudAnchorAsync(currentAnchor!!)
                Toast.makeText(this, "Model is placed!", Toast.LENGTH_SHORT).show()
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
        if (::session.isInitialized) {
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
        } else {
            Log.e("ARScreen", "AR session is not initialized yet!")
        }
    }

    private fun loadModel(modelName: String) {
        sceneView.removeChild(modelNode)
        sceneView.removeChild(videoNode)

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
        if (::session.isInitialized) {
            val frame = session.update()
            val hitTestResults = frame.hitTest(event.x, event.y)

            if (hitTestResults.isNotEmpty()) {
                val hitResult = hitTestResults[0]
                val pose = hitResult.hitPose

                modelNode.position = Position(pose.tx(), pose.ty(), pose.tz())
            }
        } else {
            Log.e("ARScreen", "AR session is not initialized yet!")
        }
    }

    private fun selectModel(index: Int) {
        if (index in modelList.indices) {
            selectedModelIndex = index
            Toast.makeText(this, "Selected Model: ${modelList[selectedModelIndex]}", Toast.LENGTH_SHORT).show()
            loadModel(modelList[selectedModelIndex])
        }
    }

    // Host Cloud Anchor asynchronously with ttlDays and BiConsumer callback
    private fun hostCloudAnchorAsync(anchor: Anchor) {
        // Set ttlDays (time to live in days, e.g., 365 days)
        val ttlDays = 365

        // Host the cloud anchor asynchronously with ttlDays and a BiConsumer callback
        session.hostCloudAnchorAsync(anchor, ttlDays, BiConsumer { cloudAnchorId, cloudAnchorState ->
            when (cloudAnchorState) {
                Anchor.CloudAnchorState.SUCCESS -> {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    saveAnchorId(userId, cloudAnchorId, "Anchor hosted successfully")
                    Toast.makeText(this@ARScreen, "Cloud Anchor hosted successfully!", Toast.LENGTH_SHORT).show()
                }
                Anchor.CloudAnchorState.ERROR_NOT_AUTHORIZED -> {
                    Toast.makeText(this@ARScreen, "Authorization error hosting anchor!", Toast.LENGTH_SHORT).show()
                }
                Anchor.CloudAnchorState.ERROR_SERVICE_UNAVAILABLE -> {
                    Toast.makeText(this@ARScreen, "Cloud Anchor service unavailable!", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this@ARScreen, "Cloud Anchor failed to host.", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    // Resolve Cloud Anchor asynchronously with a BiConsumer callback
    private fun resolveCloudAnchorAsync(anchorId: String) {
        // Resolve the cloud anchor asynchronously with a BiConsumer callback
        session.resolveCloudAnchorAsync(anchorId, BiConsumer { resolvedAnchorId, cloudAnchorState ->
            when (cloudAnchorState) {
                Anchor.CloudAnchorState.SUCCESS -> {
                    Toast.makeText(this@ARScreen, "Cloud Anchor resolved successfully!", Toast.LENGTH_SHORT).show()
                    // Optionally, set resolvedAnchorId or further processing
                }
                Anchor.CloudAnchorState.ERROR_NOT_AUTHORIZED -> {
                    Toast.makeText(this@ARScreen, "Authorization error resolving anchor!", Toast.LENGTH_SHORT).show()
                }
                Anchor.CloudAnchorState.ERROR_SERVICE_UNAVAILABLE -> {
                    Toast.makeText(this@ARScreen, "Cloud Anchor service unavailable during resolution!", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this@ARScreen, "Failed to resolve Cloud Anchor.", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    // Save the anchor ID to Firebase
    private fun saveAnchorId(userId: String?, anchorId: String?, Status: String) {
        if (userId == null || anchorId == null) {
            Toast.makeText(this, "Error: Unable to save anchor!", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("ARScreen", "Saving anchor: UserId=$userId, AnchorId=$anchorId")

        val db = FirebaseFirestore.getInstance()
        val anchorData = mapOf(
            "uid" to userId,
            "anchorId" to anchorId
        )

        // Use the userId as the document ID to overwrite any existing anchor data for the user
        db.collection("anchors").document(userId)
            .set(anchorData) // Use set to overwrite the document if it already exists
            .addOnSuccessListener {
                Toast.makeText(this, "Anchor saved successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving anchor: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    // Capture a screenshot of the AR view
    private fun captureScreenshotAndUpload() {
        val screenshot = captureScreenshot(sceneView)
        if (screenshot != null) {
            // Proceed to upload the screenshot to Firebase Storage
            val byteArrayOutputStream = ByteArrayOutputStream()
            screenshot.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            val storageReference: StorageReference =
                FirebaseStorage.getInstance().getReference("screenshots/${UUID.randomUUID()}.jpg")

            storageReference.putBytes(byteArray).addOnSuccessListener {
                Toast.makeText(this, "Screenshot uploaded successfully!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to upload screenshot!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Failed to capture screenshot!", Toast.LENGTH_SHORT).show()
        }
    }

    // Capture screenshot from a View (in this case, the AR scene view)
    private fun captureScreenshot(view: View): Bitmap? {
        try {
            // Create a Bitmap with the same size as the view
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Draw the view's content onto the canvas
            view.draw(canvas)
            return bitmap
        } catch (e: Exception) {
            Log.e("ARScreen", "Error capturing screenshot: ${e.localizedMessage}")
            return null
        }
    }
}