package com.developer.vijay.chatie.utils

import android.app.Dialog
import android.app.ProgressDialog
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.developer.vijay.chatie.R
import com.developer.vijay.chatie.databinding.DialogProgressBinding
import com.developer.vijay.chatie.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import java.util.*

open class BaseActivity : AppCompatActivity() {

    private var dialog: Dialog? = null
    val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firebaseStorage by lazy { FirebaseStorage.getInstance() }
    val firebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    val firebaseMessaging by lazy { FirebaseMessaging.getInstance() }
    val firebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val configSettings = FirebaseRemoteConfigSettings.Builder().apply {
            minimumFetchIntervalInSeconds = 0
        }
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings.build())

    }

    fun getBackgroundFromFirebase(view: View) {
        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener {
            if (it.isSuccessful) {
                val backgroundImageUrl = firebaseRemoteConfig.getString(FirebaseUtils.BACKGROUND_IMAGE_URL)
                val backgroundColor = firebaseRemoteConfig.getString(FirebaseUtils.BACKGROUND_COLOR)
                val showBackgroundImage = firebaseRemoteConfig.getBoolean(FirebaseUtils.SHOW_BACKGROUND_IMAGE)

                if (showBackgroundImage) {
                    Glide.with(this).load(backgroundImageUrl).into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            view.background = resource
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {

                        }

                    })
                } else {
                    view.setBackgroundColor(Color.parseColor(backgroundColor))
                }
            } else
                it.exception?.message?.let { it1 -> showToast(it1) }
        }
    }

    fun showProgressDialog(message: String) {
        val view = DialogProgressBinding.inflate(layoutInflater)
        dialog = Dialog(this).apply {
            setContentView(view.root)
            setCancelable(false)
            view.tvMessage.text = message
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            show()
        }
    }

    fun hideProgressDialog() {
        dialog?.dismiss()
    }

    fun getChildValues(
        childPath1: String,
        onDataChange: (snapshot: DataSnapshot) -> Unit
    ) {
        firebaseDatabase.reference.child(childPath1)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    hideProgressDialog()
                    onDataChange(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    hideProgressDialog()
                    showToast(error.message)
                }
            })
    }

    fun getChildValues(
        childPath1: String,
        childPath2: String,
        onDataChange: (snapshot: DataSnapshot) -> Unit
    ) {
        firebaseDatabase.reference.child(childPath1).child(childPath2)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    hideProgressDialog()
                    onDataChange(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    hideProgressDialog()
                    showToast(error.message)
                }
            })
    }

    fun getChildValues(
        childPath1: String,
        childPath2: String,
        childPath3: String,
        onDataChange: (snapshot: DataSnapshot) -> Unit
    ) {
        firebaseDatabase.reference.child(childPath1).child(childPath2).child(childPath3)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    hideProgressDialog()
                    onDataChange(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    hideProgressDialog()
                    showToast(error.message)
                }
            })
    }

    override fun onResume() {
        super.onResume()
        PrefUtils.getUser()?.let { firebaseDatabase.reference.child(FirebaseUtils.PRESENCE).child(it.uid).setValue(0) }
    }

    override fun onPause() {
        super.onPause()
        PrefUtils.getUser()?.let { firebaseDatabase.reference.child(FirebaseUtils.PRESENCE).child(it.uid).setValue(Calendar.getInstance().timeInMillis) }
    }
}