package com.tahabagheri.cineduca

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class AboutPage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about_view)

        val imgBtnLogoAbout = findViewById<ImageView>(R.id.img_btn_logo_about)
        val imgBtnFacebook = findViewById<ImageView>(R.id.img_btn_facebook_about)
        val imgBtnTwitter = findViewById<ImageView>(R.id.img_btn_twitter_about)
        val imgBtnInstagram = findViewById<ImageView>(R.id.img_btn_instagram_about)
        val imgBtnYoutube = findViewById<ImageView>(R.id.img_btn_youtube_about)
        val imgBtnLinkedin = findViewById<ImageView>(R.id.img_btn_linkedin_about)
        val imgBtnWhatsapp = findViewById<ImageView>(R.id.img_btn_whatsapp_about)
        val imgBtnEmail = findViewById<ImageView>(R.id.img_btn_email_about)
        val imgBtnPhone = findViewById<ImageView>(R.id.img_btn_phone_about)


        imgBtnLogoAbout.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }

        imgBtnFacebook.setOnClickListener {
            val intent1 =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/asociacioncineduca"))
            startActivity(intent1)
        }

        imgBtnTwitter.setOnClickListener {
            val intent2 = Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/AC_Cineduca"))
            startActivity(intent2)
        }

        imgBtnInstagram.setOnClickListener {
            val intent3 =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/accineduca"))
            startActivity(intent3)
        }

        imgBtnYoutube . setOnClickListener {
            val intent4 =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@asociacioncineduca"))
            startActivity(intent4)
        }

        imgBtnLinkedin . setOnClickListener {
            val intent5 =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/company/asociaci-n-cineduca/"))
            startActivity(intent5)
        }

        imgBtnWhatsapp . setOnClickListener {
            val intent6 =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://cineduca.org/+34%20652%2033%2078%2007"))
            startActivity(intent6)
        }

        imgBtnEmail . setOnClickListener {
            val intent7 =
                Intent(Intent.ACTION_VIEW, Uri.parse("mailto:hector.garcia@cineduca.org"))
            startActivity(intent7)
        }

        imgBtnPhone . setOnClickListener {
            val intent8 =
                Intent(Intent.ACTION_VIEW, Uri.parse("tel:+34 652 33 78 07"))
            startActivity(intent8)
        }


    }
}