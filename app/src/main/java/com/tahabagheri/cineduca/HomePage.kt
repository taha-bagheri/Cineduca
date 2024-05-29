package com.tahabagheri.cineduca

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.mobileffmpeg.FFmpeg


private val PERMISSION_REQUEST_CODE = 1001


class HomePage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_view)


        val imgBtnAboutHome = findViewById<ImageView>(R.id.img_btn_about_home)
        val imgBtnReverse = findViewById<ImageView>(R.id.img_btn_reverse_home)

        imgBtnAboutHome.setOnClickListener {
            // Aquí abrimos la actividad AboutPage.kt
            val intent = Intent(this, AboutPage::class.java)
            startActivity(intent)
        }

        imgBtnReverse.setOnClickListener {
            val intent = Intent(this, EditVideoPage::class.java)
            startActivity(intent)
        }



        // TODO: yadam bashe background aksaro pak konam


    }

    /*
    fun slowMotion(inputPath: String, factor: Double, outputPath: String) {
        val cmd = "-i $inputPath -vf setpts=$factor*PTS $outputPath"
        FFmpeg.execute(cmd)
    }


    fun cutVideo(inputPath: String, startTime: String, duration: String, outputPath: String) {
        val cmd = "-i $inputPath -ss $startTime -t $duration -c copy $outputPath"
        FFmpeg.execute(cmd)
    }

    fun flipVideo(inputPath: String, outputPath: String) {
        val cmd = "-i $inputPath -vf hflip $outputPath"
        FFmpeg.execute(cmd)
    }

     */
}