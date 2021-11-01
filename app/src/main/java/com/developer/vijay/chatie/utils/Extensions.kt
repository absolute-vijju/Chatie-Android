package com.developer.vijay.chatie.utils

import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun AppCompatActivity.hideKeyboard() {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    var view = currentFocus
    if (view == null)
        view = View(this)
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}