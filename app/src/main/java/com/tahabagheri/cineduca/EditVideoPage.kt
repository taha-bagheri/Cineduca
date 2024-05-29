package com.tahabagheri.cineduca

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.ExecuteCallback
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.LogCallback
import com.arthenica.mobileffmpeg.LogMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

class EditVideoPage : AppCompatActivity() {
    private val SELECT_VIDEO_REQUEST_CODE = 1
    private val PERMISSION_REQUEST_CODE = 1001
    private lateinit var selectedVideoUri: Uri
    private lateinit var videoView: VideoView
    private lateinit var seekBar: SeekBar
    private lateinit var btnSelectVideo: Button
    private lateinit var btnSaveVideo: Button
    private lateinit var btnReverseVideo: Button
    private lateinit var progressDialog: ProgressDialog
    private var isSeekBarThreadRunning = false
    private var ffmpegOutput = StringBuilder()
    private var originalBitrate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_video_view)

        // Initialize UI components
        videoView = findViewById(R.id.videoView)
        seekBar = findViewById(R.id.seekBar)
        btnSelectVideo = findViewById(R.id.btn_selectVideo_editVideo)
        btnSaveVideo = findViewById(R.id.btn_saveVideo_editVideo)
        btnReverseVideo = findViewById(R.id.button4)

        // Check and request permissions if necessary
        if (!hasPermissions()) {
            requestPermissions()
        }

        // Set click listeners for buttons
        btnSelectVideo.setOnClickListener {
            selectVideo()
        }

        btnReverseVideo.setOnClickListener {
            reverseVideo()
        }

        btnSaveVideo.setOnClickListener {
            saveReversedVideo()
        }

        // Set up videoView and seekBar
        videoView.setOnPreparedListener { mediaPlayer ->
            seekBar.max = mediaPlayer.duration

            val updateSeekBarThread = object : Thread() {
                override fun run() {
                    isSeekBarThreadRunning = true
                    while (isSeekBarThreadRunning) {
                        if (videoView.isPlaying) {
                            runOnUiThread {
                                seekBar.progress = videoView.currentPosition
                            }
                            sleep(100)
                        }
                    }
                }
            }
            updateSeekBarThread.start()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    videoView.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Initialize progress dialog
        progressDialog = ProgressDialog(this).apply {
            setMessage("Processing video...")
            setCancelable(false)
        }

        // Set up custom log callback for FFmpeg
        Config.enableLogCallback(LogCallback { logMessage: LogMessage ->
            ffmpegOutput.append(logMessage.text).append("\n")
            Log.d("FFmpeg", logMessage.text)
        })
    }

    // Check if the necessary permissions are granted
    private fun hasPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Request the necessary permissions
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
    }

    // Handle the result of permission requests
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permissions granted
            } else {
                // Permissions denied
            }
        }
    }

    // Open a file picker to select a video
    private fun selectVideo() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }
        startActivityForResult(intent, SELECT_VIDEO_REQUEST_CODE)
    }

    // Handle the result of the video selection
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_VIDEO_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            data.data?.let { uri ->
                selectedVideoUri = uri
                videoView.setVideoURI(uri)
                videoView.start()
                extractVideoBitrate(uri)
            }
        }
    }

    // Extract the bitrate of the selected video
    private fun extractVideoBitrate(uri: Uri) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, uri)
        originalBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
        retriever.release()
    }

    // Reverse the selected video
    private fun reverseVideo() {
        stopSeekBarThread()
        videoView.stopPlayback() // Stop the video playback before processing
        progressDialog.show() // Show the loading indicator

        val inputFile = copyUriToFile(selectedVideoUri)
        if (inputFile == null) {
            runOnUiThread {
                progressDialog.dismiss() // Dismiss the loading indicator
                Toast.makeText(this, "Failed to get file path from URI", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val needsConversion = needsConversionToMpeg4(inputFile)
        if (needsConversion) {
            convertToMpeg4(inputFile) { convertedFile ->
                splitAndReverseChunks(convertedFile)
            }
        } else {
            splitAndReverseChunks(inputFile)
        }
    }

    // Check if the video needs conversion to MPEG-4 format
    private fun needsConversionToMpeg4(file: File): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            mimeType != "video/mp4"
        } catch (e: Exception) {
            e.printStackTrace()
            true
        } finally {
            retriever.release()
        }
    }

    // Convert the video to MPEG-4 format
    private fun convertToMpeg4(inputFile: File, callback: (File) -> Unit) {
        val tempFile = File.createTempFile("temp_video_mpeg4", ".mp4", cacheDir)
        val cmd = arrayOf(
            "-i", inputFile.absolutePath,
            "-c:v", "mpeg4",
            "-q:v", "1", // If number increase, quality will be reduce
            "-c:a", "aac",
            "-b:a", originalBitrate ?: "192k",
            "-y",
            tempFile.absolutePath
        )

        FFmpeg.executeAsync(cmd, ExecuteCallback { executionId, returnCode ->
            runOnUiThread {
                if (returnCode == 0) {
                    callback(tempFile)
                } else {
                    progressDialog.dismiss() // Dismiss the loading indicator
                    val errorOutput = ffmpegOutput.toString()
                    Toast.makeText(this, "Failed to convert video to MPEG-4. Error: $errorOutput", Toast.LENGTH_SHORT).show()
                    Log.d("FFmpeg Error", errorOutput)
                }
                ffmpegOutput.setLength(0) // Clear the StringBuilder after use
            }
        })
    }

    // Split the video into chunks and reverse each chunk
    private fun splitAndReverseChunks(inputFile: File) {
        val chunkDir = File(cacheDir, "video_chunks")
        if (!chunkDir.exists()) {
            chunkDir.mkdirs()
        }
        chunkDir.listFiles()?.forEach { it.delete() } // Clean up old chunks

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(inputFile.absolutePath)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
        retriever.release()

        val chunkDuration = 10_000 // 10 seconds per chunk
        val numChunks = (duration / chunkDuration).toInt() + 1

        val reversedChunkFiles = mutableListOf<File>()

        // Process each chunk sequentially
        fun processChunk(index: Int) {
            if (index >= numChunks) {
                concatenateChunks(reversedChunkFiles)
                return
            }

            val chunkFile = File(chunkDir, "chunk_$index.mp4")

            val startTime = index * chunkDuration
            val cmd = arrayOf(
                "-i", inputFile.absolutePath,
                "-ss", (startTime / 1000).toString(),
                "-t", (chunkDuration / 1000).toString(),
                "-c:v", "mpeg4",
                "-q:v", "1", // If number increase, quality will be reduce
                "-c:a", "aac",
                "-b:a", originalBitrate ?: "192k",
                "-y",
                chunkFile.absolutePath
            )

            FFmpeg.executeAsync(cmd, ExecuteCallback { executionId, returnCode ->
                if (returnCode == 0) {
                    reverseChunk(chunkFile) { reversedChunkFile ->
                        reversedChunkFiles.add(reversedChunkFile)
                        processChunk(index + 1)
                    }
                } else {
                    runOnUiThread {
                        progressDialog.dismiss() // Dismiss the loading indicator
                        val errorOutput = ffmpegOutput.toString()
                        Toast.makeText(this, "Failed to split video. Error: $errorOutput", Toast.LENGTH_SHORT).show()
                        Log.d("FFmpeg Error", errorOutput)
                    }
                }
            })
        }

        processChunk(0)
    }

    // Reverse a single chunk of video
    private fun reverseChunk(chunkFile: File, callback: (File) -> Unit) {
        val reversedFile = File(chunkFile.parent, "reversed_${chunkFile.name}")
        val cmd = arrayOf(
            "-i", chunkFile.absolutePath,
            "-vf", "reverse",
            "-af", "areverse",
            "-c:v", "mpeg4",
            "-q:v", "1", // If number increase, quality will be reduce
            "-c:a", "aac",
            "-b:a", originalBitrate ?: "192k",
            "-y",
            reversedFile.absolutePath
        )

        FFmpeg.executeAsync(cmd, ExecuteCallback { executionId, returnCode ->
            runOnUiThread {
                if (returnCode == 0) {
                    chunkFile.delete()
                    callback(reversedFile)
                } else {
                    progressDialog.dismiss() // Dismiss the loading indicator
                    val errorOutput = ffmpegOutput.toString()
                    Toast.makeText(this, "Failed to reverse chunk. Error: $errorOutput", Toast.LENGTH_SHORT).show()
                    Log.d("FFmpeg Error", errorOutput)
                }
            }
        })
    }

    // Concatenate reversed chunks into a single video file
    private fun concatenateChunks(chunkFiles: List<File>) {
        val outputPath = getExternalFilesDir(null)?.absolutePath + "/reversed_video.mp4"

        val inputs = chunkFiles.reversed().flatMap { file ->
            listOf("-i", file.absolutePath)
        }.toTypedArray()

        val filterComplex = (0 until chunkFiles.size).joinToString("") { "[$it:v:0][$it:a:0]" } + "concat=n=${chunkFiles.size}:v=1:a=1[outv][outa]"

        val cmd = arrayOf(
            *inputs,
            "-filter_complex", filterComplex,
            "-map", "[outv]",
            "-map", "[outa]",
            "-c:v", "mpeg4",
            "-q:v", "1", // If number increase, quality will be reduce
            "-c:a", "aac",
            "-b:a", originalBitrate ?: "192k",
            "-y",
            outputPath
        )

        FFmpeg.executeAsync(cmd, ExecuteCallback { executionId, returnCode ->
            runOnUiThread {
                progressDialog.dismiss() // Dismiss the loading indicator
                if (returnCode == 0) {
                    Toast.makeText(this, "Video reversed successfully", Toast.LENGTH_SHORT).show()
                    videoView.setVideoURI(Uri.fromFile(File(outputPath)))
                    videoView.start()
                } else {
                    val errorOutput = ffmpegOutput.toString()
                    Toast.makeText(this, "Failed to concatenate chunks. Error: $errorOutput", Toast.LENGTH_SHORT).show()
                    Log.d("FFmpeg Error", errorOutput)
                }
                ffmpegOutput.setLength(0) // Clear the StringBuilder after use
            }
        })
    }

    // Save the reversed video to external storage
    private fun saveReversedVideo() {
        val inputPath = getExternalFilesDir(null)?.absolutePath + "/reversed_video.mp4"
        val file = File(inputPath)
        if (!file.exists()) {
            Toast.makeText(this, "Reversed video file not found", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = ProgressDialog(this).apply {
            setMessage("Saving reversed video...")
            setCancelable(false)
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
            show()
        }

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val resolver = contentResolver
                    val videoCollection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                    val newVideoDetails = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, "reversed_video.mp4")
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/")
                        put(MediaStore.Video.Media.IS_PENDING, 1) // Set the video as pending while writing
                    }

                    val videoUri = resolver.insert(videoCollection, newVideoDetails)
                    videoUri?.let { uri ->
                        resolver.openOutputStream(uri).use { outputStream ->
                            FileInputStream(file).use { inputStream ->
                                val buffer = ByteArray(4096)
                                var length: Int
                                while (inputStream.read(buffer).also { length = it } > 0) {
                                    outputStream?.write(buffer, 0, length)
                                }
                            }
                        }

                        // After writing the file, mark it as not pending
                        newVideoDetails.clear()
                        newVideoDetails.put(MediaStore.Video.Media.IS_PENDING, 0)
                        resolver.update(uri, newVideoDetails, null, null)
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditVideoPage, "Reversed video file saved!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditVideoPage, "Failed to save reversed video", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                }
            }
        }
    }

    // Copy the selected video URI to a temporary file
    private fun copyUriToFile(uri: Uri): File? {
        return try {
            val inputStream: InputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("temp_video", ".mp4", cacheDir)
            FileOutputStream(tempFile).use { outputStream ->
                val buffer = ByteArray(4096)
                var length: Int
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }
            }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Stop the seek bar update thread
    private fun stopSeekBarThread() {
        isSeekBarThreadRunning = false
    }
}
