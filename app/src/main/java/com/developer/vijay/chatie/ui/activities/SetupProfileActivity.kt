package com.developer.vijay.chatie.ui.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.developer.vijay.chatie.R
import com.developer.vijay.chatie.databinding.ActivitySetupProfileBinding
import com.developer.vijay.chatie.models.User
import com.developer.vijay.chatie.ui.activities.home.HomeActivity
import com.developer.vijay.chatie.utils.*
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import timber.log.Timber
import java.io.File

class SetupProfileActivity : BaseActivity() {

    private val mBinding by lazy { ActivitySetupProfileBinding.inflate(layoutInflater) }
    private var selectedImageURI: Uri? = null
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        if (intent.getBooleanExtra(Constants.IS_FIRST_TIME, true))
            mBinding.btnSetupProfile.text = getString(R.string.setup_profile)
        else {

            PrefUtils.getUser()?.apply {
                currentUser = this
                GeneralFunctions.loadImage(applicationContext, profileImage, mBinding.ivUser)
                mBinding.etUserName.setText(name)
                selectedImageURI = Uri.parse(this.profileImage)
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