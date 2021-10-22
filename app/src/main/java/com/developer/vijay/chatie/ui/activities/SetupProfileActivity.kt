package com.developer.vijay.chatie.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.developer.vijay.chatie.databinding.ActivitySetupProfileBinding
import com.developer.vijay.chatie.models.User
import com.developer.vijay.chatie.ui.activities.home.HomeActivity
import com.developer.vijay.chatie.utils.BaseActivity
import com.developer.vijay.chatie.utils.FirebaseUtils
import com.developer.vijay.chatie.utils.PrefUtils
import com.developer.vijay.chatie.utils.showToast

class SetupProfileActivity : BaseActivity() {

    private val mBinding by lazy { ActivitySetupProfileBinding.inflate(layoutInflater) }
    private var selectedImageURI: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

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

            if (selectedImageURI == null) {
                showToast("Please select image")
                return@setOnClickListener
            }

            showProgressDialog("Creating your account.")

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
                                                    } else {
                                                        userCreationResponse.exception?.message?.let { errorMessage -> showToast(errorMessage) }
                                                    }
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
}