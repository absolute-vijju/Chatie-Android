package com.developer.vijay.chatie.ui.activities.home

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.developer.vijay.chatie.R
import com.developer.vijay.chatie.databinding.ActivityHomeBinding
import com.developer.vijay.chatie.models.User
import com.developer.vijay.chatie.ui.activities.chat.ChatActivity
import com.developer.vijay.chatie.ui.activities.group_chat.GroupChatActivity
import com.developer.vijay.chatie.utils.*
import com.google.gson.Gson
import java.util.*

class HomeActivity : BaseActivity() {

    private val mBinding by lazy { ActivityHomeBinding.inflate(layoutInflater) }
    private val userAdapter by lazy {
        UserAdapter { position ->
            startActivity(Intent(this, ChatActivity::class.java).apply {
                putExtra(Constants.USER, Gson().toJson(userList[position]))
            })
        }
    }
    private val statusAdapter by lazy { StatusAdapter() }
    private val userList = arrayListOf<User>()
    private val statusList = arrayListOf<UserStatus>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        init()
        setDeviceToken()

        val pickImageContract =
            registerForActivityResult(ActivityResultContracts.GetContent()) { imageUri ->

                imageUri?.let {
                    showProgressDialog("Uploading Status")

                    val currentDate = Date()

                    val reference =
                        firebaseStorage.reference.child(FirebaseUtils.STATUS)
                            .child(currentDate.time.toString())

                    reference.putFile(imageUri)
                        .addOnCompleteListener { imageUploadResponse ->
                            if (imageUploadResponse.isSuccessful) {

                                reference.downloadUrl.addOnCompleteListener { uploadedImageUrlResponse ->
                                    if (uploadedImageUrlResponse.isSuccessful) {

                                        val currentUser = PrefUtils.getUser()

                                        currentUser?.let {
                                            val userStatus = UserStatus()
                                            userStatus.name = currentUser.name
                                            userStatus.profileImage = currentUser.profileImage
                                            userStatus.lastUpdated = currentDate.time

                                            val mapObj = hashMapOf<String, Any>()
                                            mapObj[FirebaseUtils.NAME] = userStatus.name
                                            mapObj[FirebaseUtils.PROFILE_IMAGE] = userStatus.profileImage
                                            mapObj[FirebaseUtils.LAST_UPDATED] = userStatus.lastUpdated

                                            val status = Status(
                                                uploadedImageUrlResponse.result.toString(),
                                                userStatus.lastUpdated
                                            )

                                            firebaseDatabase.reference
                                                .child(FirebaseUtils.STORIES)
                                                .child(currentUser.uid)
                                                .updateChildren(mapObj)

                                            firebaseDatabase.reference
                                                .child(FirebaseUtils.STORIES)
                                                .child(currentUser.uid)
                                                .child(FirebaseUtils.STATUSES)
                                                .push()
                                                .setValue(status)

                                            hideProgressDialog()

                                        }

                                    } else
                                        uploadedImageUrlResponse.exception?.message?.let { it1 ->
                                            showToast(
                                                it1
                                            )
                                        }
                                }

                            } else
                                imageUploadResponse.exception?.message?.let { it1 -> showToast(it1) }
                        }
                }
            }

        getChildValues(FirebaseUtils.USERS) { snapshot ->
            userList.clear()

            for (children in snapshot.children) {
                val user = children.getValue(User::class.java)
                if (!user?.uid.equals(PrefUtils.getUser()?.uid))
                    user?.let { userList.add(it) }
            }

            mBinding.rvUsers.hideShimmerAdapter()
            userAdapter.setData(userList)
        }

        getChildValues(FirebaseUtils.STORIES) { snapshot ->
            statusList.clear()

            for (stories in snapshot.children) {

                UserStatus().apply {
                    name = stories.child(FirebaseUtils.NAME).getValue(String::class.java)!!
                    profileImage =
                        stories.child(FirebaseUtils.PROFILE_IMAGE).getValue(String::class.java)!!
                    lastUpdated =
                        stories.child(FirebaseUtils.LAST_UPDATED).getValue(Long::class.java)!!


                    val statusesList = arrayListOf<Status>()
                    for (statusesSnapshot in stories.child(FirebaseUtils.STATUSES).children) {
                        val status = statusesSnapshot.getValue(Status::class.java)
                        status?.let { statusesList.add(it) }
                    }

                    statuses = statusesList

                    statusList.add(this)
                }
            }

            mBinding.rvStatus.hideShimmerAdapter()
            statusAdapter.setData(statusList)
        }

        mBinding.bottomNavigation.setOnItemSelectedListener {

            when (it.itemId) {
                R.id.mStatus -> {
                    pickImageContract.launch("image/*")
                }
            }

            false
        }

    }

    private fun setDeviceToken() {
        firebaseMessaging.token.addOnCompleteListener { response ->
            if (response.isSuccessful)
                response.result?.let { it ->
                    PrefUtils.set(FirebaseUtils.DEVICE_TOKEN, it)
                    val map = hashMapOf<String, Any>()
                    map[FirebaseUtils.DEVICE_TOKEN] = it

                    PrefUtils.getUser()?.uid?.let { it1 ->
                        firebaseDatabase.reference.child(FirebaseUtils.USERS)
                            .child(it1)
                            .updateChildren(map)
                    }
                }
            else
                PrefUtils.set(FirebaseUtils.DEVICE_TOKEN, "")
        }
    }

    private fun init() {
        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener {
            if (it.isSuccessful) {
                val backgroundImageUrl = firebaseRemoteConfig.getString(FirebaseUtils.BACKGROUND_IMAGE_URL)
                val backgroundColor = firebaseRemoteConfig.getString(FirebaseUtils.BACKGROUND_COLOR)
                val showBackgroundImage = firebaseRemoteConfig.getBoolean(FirebaseUtils.SHOW_BACKGROUND_IMAGE)

                if (showBackgroundImage) {
                    Glide.with(this).load(backgroundImageUrl).into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            mBinding.ivBackground.background = resource
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {

                        }

                    })
                } else {
                    mBinding.ivBackground.setBackgroundColor(Color.parseColor(backgroundColor))
                }
            } else
                it.exception?.message?.let { it1 -> showToast(it1) }
        }


        mBinding.rvStatus.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = statusAdapter
        }

        mBinding.rvUsers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = userAdapter
        }

        mBinding.rvStatus.showShimmerAdapter()
        mBinding.rvUsers.showShimmerAdapter()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mSearch -> {
                showToast("Coming soon...")
            }
            R.id.mGroup -> {
                startActivity(Intent(this, GroupChatActivity::class.java))
            }
            R.id.mInvite -> {
                showToast("Coming soon...")
            }
            R.id.mSetting -> {
                showToast("Coming soon...")
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.topmenu, menu)
        return super.onCreateOptionsMenu(menu)
    }
}