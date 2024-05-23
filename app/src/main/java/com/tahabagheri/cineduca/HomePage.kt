package com.tahabagheri.cineduca

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView

class HomePage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_view)

        val imgBtnAboutHome = findViewById<ImageView>(R.id.img_btn_about_home)


        imgBtnAboutHome.setOnClickListener {
            // Aquí abrimos la actividad AboutPage.kt
            val intent = Intent(this, AboutPage::class.java)
            startActivity(intent)
        }

        // TODO: yadam bashe background aksaro pak konam


    }
}