package a48626.sumolmbao

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView
    private lateinit var closeButton: ImageView
    private var hasAttemptedAlternativeUrl = false

    companion object {
        private const val TAG = "VideoPlayerDebug"
        const val EXTRA_BASHO_ID = "EXTRA_BASHO_ID"
        const val EXTRA_DAY = "EXTRA_DAY"
        const val EXTRA_RIKISHI1_ID = "EXTRA_RIKISHI1_ID"
        const val EXTRA_RIKISHI2_ID = "EXTRA_RIKISHI2_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        videoView = findViewById(R.id.videoView)
        progressBar = findViewById(R.id.progressBar)
        errorTextView = findViewById(R.id.errorTextView)
        closeButton = findViewById(R.id.closeButton)

        closeButton.setOnClickListener {
            finish()
        }

        val bashoId = intent.getStringExtra(EXTRA_BASHO_ID)
        val day = intent.getIntExtra(EXTRA_DAY, -1)
        val rikishi1Id = intent.getIntExtra(EXTRA_RIKISHI1_ID, -1)
        val rikishi2Id = intent.getIntExtra(EXTRA_RIKISHI2_ID, -1)

        if (bashoId == null || day == -1 || rikishi1Id == -1 || rikishi2Id == -1) {
            Log.e(TAG, "FATAL: One or more required parameters are missing.")
            showError("Could not load video: Missing information.")
            return
        }

        val formattedDay = String.format("%02d", day)
        val videoUrl = "https://pub-50908de714314521b9e4252f39cb0424.r2.dev/${bashoId}-${formattedDay}-${rikishi1Id}-${rikishi2Id}.mp4"

        playVideo(videoUrl)
    }

    private fun playVideo(videoUrl: String) {
        Log.i(TAG, "Attempting to play video from URL: $videoUrl")

        progressBar.visibility = View.VISIBLE
        errorTextView.visibility = View.GONE

        // Use our custom MediaController
        val mediaController = object : MediaController(this) {
            override fun show() {
                super.show()
                closeButton.visibility = View.VISIBLE
            }

            override fun hide() {
                super.hide()
                closeButton.visibility = View.GONE
            }

            // *** NEW: Override setAnchorView to find and hide the buttons ***
            override fun setAnchorView(view: View) {
                super.setAnchorView(view)

                // The MediaController layout has default IDs for its buttons.
                // We find them by ID and hide them.
                // The 'ffwd' (fast-forward) and 'rew' (rewind) are the standard IDs.
                val fastForwardButton = this.findViewById<ImageButton>(resources.getIdentifier("ffwd", "id", "android"))
                val rewindButton = this.findViewById<ImageButton>(resources.getIdentifier("rew", "id", "android"))

                fastForwardButton?.visibility = View.GONE
                rewindButton?.visibility = View.GONE
            }
        }

        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        val videoUri = Uri.parse(videoUrl)
        videoView.setVideoURI(videoUri)

        videoView.setOnPreparedListener { mp ->
            Log.i(TAG, "SUCCESS: Video prepared. Starting playback for: $videoUrl")
            progressBar.visibility = View.GONE
            mp.start()
        }

        videoView.setOnErrorListener { _, what, extra ->
            Log.e(TAG, "ERROR: Video playback failed for URL: $videoUrl (Code: $what, Extra: $extra)")

            if (!hasAttemptedAlternativeUrl) {
                hasAttemptedAlternativeUrl = true
                val alternativeUrl = getAlternativeUrl()
                Log.w(TAG, "Original URL failed. Trying alternative: $alternativeUrl")
                playVideo(alternativeUrl)
            } else {
                Log.e(TAG, "FATAL: Both primary and alternative URLs failed. Displaying error.")
                showError("Video not found.")
            }
            true
        }
    }

    private fun getAlternativeUrl(): String {
        val bashoId = intent.getStringExtra(EXTRA_BASHO_ID)!!
        val day = intent.getIntExtra(EXTRA_DAY, -1)
        val rikishi1Id = intent.getIntExtra(EXTRA_RIKISHI2_ID, -1)
        val rikishi2Id = intent.getIntExtra(EXTRA_RIKISHI1_ID, -1)
        val formattedDay = String.format("%02d", day)
        return "https://pub-50908de714314521b9e4252f39cb0424.r2.dev/${bashoId}-${formattedDay}-${rikishi1Id}-${rikishi2Id}.mp4"
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        videoView.visibility = View.GONE
        errorTextView.visibility = View.VISIBLE
        errorTextView.text = message
        closeButton.visibility = View.VISIBLE
    }
}