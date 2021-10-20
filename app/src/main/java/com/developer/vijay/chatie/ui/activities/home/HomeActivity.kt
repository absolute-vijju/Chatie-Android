package com.developer.vijay.chatie.ui.activities.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.developer.vijay.chatie.R
import com.developer.vijay.chatie.databinding.ActivityHomeBinding
import com.developer.vijay.chatie.models.User
import com.developer.vijay.chatie.ui.activities.chat.ChatActivity
import com.developer.vijay.chatie.utils.*
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import java.util.*

class HomeActivity : BaseActivity() {

    private val mBinding by lazy { ActivityHomeBinding.inflate(layoutInflater) }
    private val userAdapter by lazy {
        UserAdapter { position ->
            startActivity(Intent(this, ChatActivity::class.java).apply {
                putExtra(Constants.USER, Gson().toJson(userList[position]))
                putExtra(Constants.UID, userList[position].uid)
            })
        }
    }
    private val statusAdapter by lazy {
        StatusAdapter { position ->

        }
    }
    private val userList = arrayListOf<User>()
    private val statusList = arrayListOf<UserStatus>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        init()

        val pickImageContract =
            registerForActivityResult(ActivityResultContracts.GetContent()) { imageUri ->

                showProgressDialog("Uploading Status")

                val currentDate = Date()

                val reference =
                    firebaseStorage.reference.child(FirebaseUtils.STATUS).child(currentDate.time.toString())

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
                                        mapObj[FirebaseUtils.PROFILEIMAGE] = userStatus.profileImage
                                        mapObj[FirebaseUtils.LASTUPDATED] = userStatus.lastUpdated

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
                        stories.child(FirebaseUtils.PROFILEIMAGE).getValue(String::class.java)!!
                    lastUpdated =
                        stories.child(FirebaseUtils.LASTUPDATED).getValue(Long::class.java)!!


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

    private fun init() {
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
            }
            R.id.mGroup -> {
            }
            R.id.mInvite -> {
            }
            R.id.mSetting -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.topmenu, menu)
        return super.onCreateOptionsMenu(menu)
    }
}