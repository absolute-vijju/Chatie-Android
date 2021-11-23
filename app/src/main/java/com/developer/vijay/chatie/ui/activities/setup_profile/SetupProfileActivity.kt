package com.developer.vijay.chatie.ui.activities.setup_profile

import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import com.developer.vijay.chatie.R
import com.developer.vijay.chatie.databinding.ActivitySetupProfileBinding
import com.developer.vijay.chatie.models.User
import com.developer.vijay.chatie.ui.activities.home.HomeActivity
import com.developer.vijay.chatie.ui.activities.phone_number.PhoneNumberActivity
import com.developer.vijay.chatie.ui.activities.view_image.ViewImageActivity
import com.developer.vijay.chatie.utils.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import timber.log.Timber

class SetupProfileActivity : BaseActivity() {

    private val mBinding by lazy { ActivitySetupProfileBinding.inflate(layoutInflater) }
    private var selectedImageURI: Uri? = null
    private var currentUser: User? = null
    private var isFirstTime = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        isFirstTime = intent.getBooleanExtra(Constants.IS_FIRST_TIME, true)

        if (isFirstTime) {
            supportActionBar?.apply {
                title = "Create Profile"
                setDisplayHomeAsUpEnabled(false)
                setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this@SetupProfileActivity, R.color.green)))
            }
            mBinding.btnSetupProfile.text = getString(R.string.setup_profile)
        } else {
            supportActionBar?.apply {
                title = "Edit Profile"
                setDisplayHomeAsUpEnabled(true)
                setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this@SetupProfileActivity, R.color.green)))
            }

            PrefUtils.getUser()?.apply {

                firebaseDatabase.reference
                    .child(FirebaseUtils.USERS)
                    .child(uid)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            snapshot.getValue(User::class.java)?.let {
                                currentUser = it
                                GeneralFunctions.loadImage(applicationContext, it.profileImage, mBinding.ivUser)
                                mBinding.etUserName.setText(it.name)
                                selectedImageURI = Uri.parse(it.profileImage)
                                PrefUtils.setUser(it)
                            }

                        }

                        override fun onCancelled(error: DatabaseError) {

                        }
                    })
            }
            mBinding.btnSetupProfile.text = getString(R.string.save)
        }

        val pickImageContract =
            registerForActivityResult(ActivityResultContracts.GetContent()) { imageUri ->
                imageUri?.let {
                    mBinding.ivUser.setImageURI(imageUri)
                    selectedImageURI = imageUri
                } ?: showToast("an ERROR occurred.")
            }

        mBinding.ivUser.setOnClickListener {
            currentUser?.apply {
                val intent = Intent(this@SetupProfileActivity, ViewImageActivity::class.java).apply { putExtra(FirebaseUtils.IMAGE, profileImage) }
                val transitionName = getString(R.string.transition_name)
                val activityOption = ActivityOptionsCompat.makeSceneTransitionAnimation(this@SetupProfileActivity, mBinding.ivUser, transitionName)
                ActivityCompat.startActivity(this@SetupProfileActivity, intent, activityOption.toBundle())
            }
        }

        mBinding.ivAddImage.setOnClickListener {
            pickImageContract.launch("image/*")
        }

        mBinding.btnSetupProfile.setOnClickListener {

            val name = mBinding.etUserName.text.toString()

            if (name.isEmpty()) {
                mBinding.etUserName.error = "Please enter name"
                return@setOnClickListener
            }

            if (currentUser == null) {
                if (selectedImageURI == null) {
                    showToast("Please select image")
                    return@setOnClickListener
                }
            }

            if (mBinding.btnSetupProfile.text.toString().equals(getString(R.string.setup_profile), true))
                createProfile(name)
            else {
                currentUser?.let { updateProfile(name) }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (!isFirstTime)
            menuInflater.inflate(R.menu.menu_logout, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mLogout -> {
                AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Yes") { dialogInterface, i ->
                        PrefUtils.clear()
                        firebaseAuth.signOut()
                        startActivity(Intent(this, PhoneNumberActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        })
                        finishAffinity()
                    }
                    .setNegativeButton("No") { dialogInterface, i ->
                        dialogInterface.dismiss()
                    }
                    .show()
            }
            else -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateProfile(name: String) {
        showProgressDialog("Updating your profile.")

        if (selectedImageURI.toString().contains("https")) {

            firebaseMessaging.token.addOnCompleteListener { deviceTokenResponse ->
                if (deviceTokenResponse.isSuccessful)
                    deviceTokenResponse.result?.let { deviceToken ->

                        val user = User(currentUser!!.uid, name, currentUser!!.phoneNumber, currentUser!!.profileImage, deviceToken)

                        firebaseDatabase.reference
                            .child(FirebaseUtils.USERS)
                            .child(currentUser!!.uid)
                            .setValue(user)
                            .addOnCompleteListener { userCreationResponse ->
                                if (userCreationResponse.isSuccessful) {
                                    hideProgressDialog()
                                    PrefUtils.setUser(user)
                                    showToast("Updated.")
                                    startActivity(Intent(this, HomeActivity::class.java))
                                    finish()
                                } else
                                    userCreationResponse.exception?.message?.let { errorMessage -> showToast(errorMessage) }
                            }

                    }
                else
                    deviceTokenResponse.exception?.message?.let { errorMessage -> showToast(errorMessage) }
            }

        } else {
            selectedImageURI?.let { imageUri ->

                val storageReference = firebaseStorage.reference.child(FirebaseUtils.PROFILES).child(currentUser!!.uid)  // Create Folder in Realtime DB and put user image into it.

                storageReference.putFile(imageUri).addOnCompleteListener { imageResponse ->
                    if (imageResponse.isSuccessful) {

                        storageReference.downloadUrl.addOnCompleteListener { uploadedImageResponse ->

                            if (uploadedImageResponse.isSuccessful) {

                                firebaseMessaging.token.addOnCompleteListener { deviceTokenResponse ->
                                    if (deviceTokenResponse.isSuccessful)
                                        deviceTokenResponse.result?.let { deviceToken ->

                                            val user = User(currentUser!!.uid, name, currentUser!!.phoneNumber, uploadedImageResponse.result.toString(), deviceToken)

                                            firebaseDatabase.reference
                                                .child(FirebaseUtils.USERS)
                                                .child(currentUser!!.uid)
                                                .setValue(user)
                                                .addOnCompleteListener { userCreationResponse ->
                                                    if (userCreationResponse.isSuccessful) {
                                                        hideProgressDialog()
                                                        PrefUtils.setUser(user)
                                                        showToast("Updated.")
                                                        startActivity(Intent(this, HomeActivity::class.java))
                                                        finish()
                                                    } else
                                                        userCreationResponse.exception?.message?.let { errorMessage -> showToast(errorMessage) }
                                                }

                                        }
                                    else
                                        deviceTokenResponse.exception?.message?.let { errorMessage -> showToast(errorMessage) }
                                }

                            } else {
                                Timber.e("uploadedImageResponse")
                                hideProgressDialog()
                                uploadedImageResponse.exception?.message?.let { errorMessage -> showToast(errorMessage) }
                            }
                        }
                    } else {
                        Timber.e("imageResponse")
                        hideProgressDialog()
                        imageResponse.exception?.message?.let { errorMessage -> showToast(errorMessage) }
                    }
                }

            } ?: showToast("Image url not found.")
        }
    }

    private fun createProfile(name: String) {
        showProgressDialog("Creating your profile.")

        selectedImageURI?.let { imageUri ->
            val storageReference = firebaseStorage.reference.child(FirebaseUtils.PROFILES).child(firebaseAuth.uid.toString())  // Create Folder in Realtime DB and put user image into it.

            storageReference.putFile(imageUri).addOnCompleteListener { imageResponse ->
                if (imageResponse.isSuccessful) {
                    storageReference.downloadUrl.addOnCompleteListener { uploadedImageResponse ->

                        if (uploadedImageResponse.isSuccessful) {

                            firebaseMessaging.token.addOnCompleteListener { deviceTokenResponse ->
                                if (deviceTokenResponse.isSuccessful)
                                    deviceTokenResponse.result?.let { deviceToken ->

                                        val user = User(firebaseAuth.uid.toString(), name, firebaseAuth.currentUser?.phoneNumber.toString(), uploadedImageResponse.result.toString(), deviceToken)

                                        firebaseDatabase.reference
                                            .child(FirebaseUtils.USERS)
                                            .child(firebaseAuth.uid.toString())
                                            .setValue(user)
                                            .addOnCompleteListener { userCreationResponse ->
                                                if (userCreationResponse.isSuccessful) {
                                                    hideProgressDialog()
                                                    PrefUtils.setUser(user)
                                                    showToast("User Created.")
                                                    startActivity(Intent(this, HomeActivity::class.java))
                                                    finish()
                                                } else
                                                    userCreationResponse.exception?.message?.let { errorMessage -> showToast(errorMessage) }
                                            }

                                    }
                                else
                                    deviceTokenResponse.exception?.message?.let { errorMessage -> showToast(errorMessage) }
                            }

                        } else {
                            hideProgressDialog()
                            uploadedImageResponse.exception?.message?.let { errorMessage -> showToast(errorMessage) }
                        }
                    }
                } else {
                    hideProgressDialog()
                    imageResponse.exception?.message?.let { errorMessage -> showToast(errorMessage) }
                }
            }

        } ?: showToast("Image url not found.")
    }
}