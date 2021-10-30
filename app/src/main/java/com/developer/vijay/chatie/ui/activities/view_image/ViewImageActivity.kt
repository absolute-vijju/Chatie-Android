package com.developer.vijay.chatie.ui.activities.view_image

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import com.developer.vijay.chatie.databinding.ActivityViewImageBinding
import com.developer.vijay.chatie.utils.FirebaseUtils
import com.developer.vijay.chatie.utils.GeneralFunctions

class ViewImageActivity : AppCompatActivity() {

    private val mBinding by lazy { ActivityViewImageBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        supportActionBar?.hide()

        val imageUrl = intent.getStringExtra(FirebaseUtils.IMAGE)

        imageUrl?.let {
            GeneralFunctions.loadImage(this, imageUrl, mBinding.iv)
        }

        mBinding.iv.setOnClickListener { ActivityCompat.finishAfterTransition(this) }

    }

    override fun onBackPressed() {
        super.onBackPressed()
        ActivityCompat.finishAfterTransition(this)
    }

}