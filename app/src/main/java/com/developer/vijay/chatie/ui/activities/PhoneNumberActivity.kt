package com.developer.vijay.chatie.ui.activities

import android.content.Intent
import android.os.Bundle
import com.developer.vijay.chatie.databinding.ActivityPhoneNumberBinding
import com.developer.vijay.chatie.ui.activities.home.HomeActivity
import com.developer.vijay.chatie.ui.activities.otp.OtpActivity
import com.developer.vijay.chatie.utils.BaseActivity
import com.developer.vijay.chatie.utils.Constants
import com.developer.vijay.chatie.utils.PrefUtils

class PhoneNumberActivity : BaseActivity() {

    private val mBinding by lazy { ActivityPhoneNumberBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        if (firebaseAuth.uid != null) {
            startActivity(Intent(this, HomeActivity::class.java)).apply {
                finish()
            }
        }

        supportActionBar?.hide()
        mBinding.etPhoneNumber.requestFocus()

        mBinding.btnContinue.setOnClickListener {
            Intent(this, OtpActivity::class.java).apply {
                putExtra(Constants.KEY_PHONE_NUMBER, mBinding.etPhoneNumber.text.trim().toString())
                startActivity(this)
            }
        }

    }

}