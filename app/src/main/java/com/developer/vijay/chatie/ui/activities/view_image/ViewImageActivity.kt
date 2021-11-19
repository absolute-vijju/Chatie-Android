package com.developer.vijay.chatie.ui.activities.view_image

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.URLUtil
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.developer.vijay.chatie.R
import com.developer.vijay.chatie.databinding.ActivityViewImageBinding
import com.developer.vijay.chatie.utils.BaseActivity
import com.developer.vijay.chatie.utils.FirebaseUtils
import com.developer.vijay.chatie.utils.GeneralFunctions
import com.developer.vijay.chatie.utils.showToast

class ViewImageActivity : BaseActivity() {

    private val mBinding by lazy { ActivityViewImageBinding.inflate(layoutInflater) }
    private var imageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        supportActionBar?.hide()

        imageUrl = intent.getStringExtra(FirebaseUtils.IMAGE)

        if (imageUrl == null)
            return

        GeneralFunctions.loadImage(this, imageUrl!!, mBinding.iv)

        val permissionContract = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultMap ->
            resultMap.entries.forEach { entry ->
                if (entry.value) {
                    GeneralFunctions.downloadImage(this, imageUrl!!)

                }
            }
        }

        mBinding.iv.setOnClickListener { ActivityCompat.finishAfterTransition(this) }

        mBinding.ivDownload.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 29)
                GeneralFunctions.downloadImage(this, imageUrl!!)
            else
                permissionContract.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        ActivityCompat.finishAfterTransition(this)
    }

}