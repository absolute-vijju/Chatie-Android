package com.developer.vijay.chatie.utils

import android.app.Dialog
import android.app.ProgressDialog
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}