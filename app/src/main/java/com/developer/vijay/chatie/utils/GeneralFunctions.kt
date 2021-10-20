package com.developer.vijay.chatie.utils

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.developer.vijay.chatie.R

object GeneralFunctions {

    fun loadImage(context: Context, imageUrl: String, imageView: ImageView) {
        Glide.with(context)
            .load(imageUrl)
            .placeholder(R.drawable.loader)
            .error(R.drawable.avatar)
            .into(imageView)
    }

}