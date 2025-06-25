package a48626.sumolmbao

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView

    companion object {
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

        val bashoId = intent.getStringExtra(EXTRA_BASHO_ID)
        val day = intent.getIntExtra(EXTRA_DAY, -1)
        val rikishi1Id = intent.getIntExtra(EXTRA_RIKISHI1_ID, -1)
        val rikishi2Id = intent.getIntExtra(EXTRA_RIKISHI2_ID, -1)

        // Validate the received data
        if (bashoId == null || day == -1 || rikishi1Id == -1 || rikishi2Id == -1) {
            Log.e("VideoPlayerActivity", "Missing required video parameters.")
            showError("Could not load video: Missing information.")
            return
        }

        // Construct the video URL
        val videoUrl = "https://pub-50908de714314521b9e4252f39cb0424.r2.dev/${bashoId}-${day}-${rikishi1Id}-${rikishi2Id}.mp4"
        Log.i("VideoPlayerActivity", "Attempting to play video from URL: $videoUrl")

        playVideo(videoUrl)
    }

    private fun playVideo(videoUrl: String) {
        progressBar.visibility = View.VISIBLE
        errorTextView.visibility = View.GONE

        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        val videoUri = Uri.parse(videoUrl)
        videoView.setVideoURI(videoUri)

        videoView.setOnPreparedListener { mp ->
            Log.d("VideoPlayerActivity", "Video is prepared. Starting playback.")
            progressBar.visibility = View.GONE
            mp.start()
        }

        videoView.setOnErrorListener { _, what, extra ->
            Log.e("VideoPlayerActivity", "Video playback error. Code: $what, Extra: $extra")
            // Try the alternative URL by swapping rikishi IDs
            val alternativeUrl = getAlternativeUrl(videoUrl)
            if (alternativeUrl != null) {
                Log.w("VideoPlayerActivity", "Original URL failed. Trying alternative: $alternativeUrl")
                playVideo(alternativeUrl) // Recursive call with the new URL
            } else {
                showError("Video not found.")
            }
            true // Indicates we have handled the error
        }
    }

    // The order of rikishi IDs might be swapped in the filename. This handles that case.
    private fun getAlternativeUrl(originalUrl: String): String? {
        val bashoId = intent.getStringExtra(EXTRA_BASHO_ID)!!
        val day = intent.getIntExtra(EXTRA_DAY, -1)
        val rikishi1Id = intent.getIntExtra(EXTRA_RIKISHI1_ID, -1)
        val rikishi2Id = intent.getIntExtra(EXTRA_RIKISHI2_ID, -1)

        val alternativeUrl = "https://pub-50908de714314521b9e4252f39cb0424.r2.dev/${bashoId}-${day}-${rikishi2Id}-${rikishi1Id}.mp4"

        // Ensure we don't try the same URL again in an endless loop
        return if (originalUrl != alternativeUrl) alternativeUrl else null
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        videoView.visibility = View.GONE
        errorTextView.visibility = View.VISIBLE
        errorTextView.text = message
    }
}