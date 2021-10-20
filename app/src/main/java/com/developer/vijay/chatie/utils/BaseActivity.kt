package com.developer.vijay.chatie.utils

import android.app.Dialog
import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.developer.vijay.chatie.R
import com.developer.vijay.chatie.databinding.DialogProgressBinding
import com.developer.vijay.chatie.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

open class BaseActivity : AppCompatActivity() {

    private var dialog: Dialog? = null
    val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firebaseStorage by lazy { FirebaseStorage.getInstance() }
    val firebaseDatabase by lazy { FirebaseDatabase.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun showProgressDialog(message: String) {
        dialog = ProgressDialog(this).apply {
            setMessage(message)
            setCancelable(false)
            show()
        }
        /*val params = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        val view = DialogProgressBinding.inflate(layoutInflater)
        dialog = Dialog(this).apply {
            setContentView(R.layout.dialog_progress)
            findViewById<TextView>(R.id.tvMessage).text = message
            show()
        }*/
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
                    onDataChange(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
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
                    onDataChange(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
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
                    onDataChange(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast(error.message)
                }
            })
    }
}