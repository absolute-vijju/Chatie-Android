package com.developer.vijay.chatie.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.TransitionOptions
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.developer.vijay.chatie.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

object GeneralFunctions {
    fun loadImage(context: Context, imageUrl: String, imageView: ImageView, placeHolderResourceId: Int = R.drawable.avatar) {
//        imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        Glide.with(context).load(imageUrl)
            .thumbnail(Glide.with(context).load(R.raw.loader))
            .listener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    isFirstResource: Boolean
                ): Boolean {
                    CoroutineScope(Dispatchers.IO).launch {
                        withContext(Dispatchers.Main) {
                            Glide.with(context).load(imageUrl).placeholder(placeHolderResourceId).into(imageView)
                        }
                    }
//                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    return true
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
//                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    return false
                }
            })
            .into(imageView)
    }

    fun getDay(day: Int): String {
        return when (day) {
            Calendar.SUNDAY -> "sunday"
            Calendar.MONDAY -> "monday"
            Calendar.TUESDAY -> "tuesday"
            Calendar.WEDNESDAY -> "wednesday"
            Calendar.THURSDAY -> "thursday"
            Calendar.FRIDAY -> "friday"
            else -> "saturday"
        }
    }

    fun getHour(hour: Int) = if (hour == 0) 12 else hour

    fun getAmPm(amPm: Int) = if (amPm == Calendar.AM) "am" else "pm"
}