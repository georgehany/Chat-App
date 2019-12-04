package com.example.android.kotlinchatapp.profile_photo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.example.android.kotlinchatapp.R
import kotlinx.android.synthetic.main.activity_profile_photo.*

class ProfilePhotoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_photo)
        title="Profile Image"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        intent.getStringExtra(profilePhoto)?.let { Glide.with(this).load(it).into(imageView) }
        if (!intent.getBooleanExtra(isProfileImage,false))
            title=""
    }
    companion object{
        val profilePhoto="profilePhoto"
        val isProfileImage="isProfileImage"
        val transitionName="transitionName"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}
