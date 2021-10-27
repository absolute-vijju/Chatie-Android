package com.developer.vijay.chatie.ui.activities.status

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.developer.vijay.chatie.R
import com.developer.vijay.chatie.databinding.FragmentStatusBinding
import com.developer.vijay.chatie.ui.activities.home.Status
import com.developer.vijay.chatie.ui.activities.home.StatusAdapter
import com.developer.vijay.chatie.ui.activities.home.UserStatus
import com.developer.vijay.chatie.utils.*
import java.util.*

class StatusFragment : Fragment(R.layout.fragment_status) {

    private val mBinding: FragmentStatusBinding by lazy { FragmentStatusBinding.bind(requireView()) }
    private val statusList = arrayListOf<UserStatus>()
    private val statusAdapter by lazy { StatusAdapter() }
    private val activity: BaseActivity by lazy { (requireActivity() as BaseActivity) }
    private val oneDayMillis = 60 * 60 * 24 * 1000

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.rvStatus.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = statusAdapter
        }
        mBinding.rvStatus.showShimmerAdapter()

        activity.getChildValues(FirebaseUtils.STORIES) { snapshot ->
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
                        status?.let {
                            if (status.timeStamp + oneDayMillis > Date().time)
                                statusesList.add(it)
                        }
                    }

                    statuses = statusesList

                    if (statuses.isNotEmpty())
                        statusList.add(this)
                }
            }

            mBinding.rvStatus.hideShimmerAdapter()
            statusAdapter.setData(statusList)
        }

        val pickImageContract =
            registerForActivityResult(ActivityResultContracts.GetContent()) { imageUri ->

                imageUri?.let {
                    activity.showProgressDialog("Uploading Status")

                    val currentDate = Date()

                    val reference =
                        activity.firebaseStorage.reference.child(FirebaseUtils.STATUS).child(currentDate.time.toString())

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

                                            val status = Status(uploadedImageUrlResponse.result.toString(), userStatus.lastUpdated)

                                            activity.firebaseDatabase.reference
                                                .child(FirebaseUtils.STORIES)
                                                .child(currentUser.uid)
                                                .updateChildren(mapObj)

                                            activity.firebaseDatabase.reference
                                                .child(FirebaseUtils.STORIES)
                                                .child(currentUser.uid)
                                                .child(FirebaseUtils.STATUSES)
                                                .push()
                                                .setValue(status)

                                            activity.hideProgressDialog()

                                        }

                                    } else
                                        uploadedImageUrlResponse.exception?.message?.let { it1 -> activity.showToast(it1) }
                                }

                            } else
                                imageUploadResponse.exception?.message?.let { it1 -> activity.showToast(it1) }
                        }
                }
            }

        mBinding.fabAddStatus.setOnClickListener { pickImageContract.launch("image/*") }
    }
}