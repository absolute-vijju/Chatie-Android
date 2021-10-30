package com.developer.vijay.chatie.ui.activities.chat

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.*
import com.developer.vijay.chatie.databinding.ActivityChatBinding
import com.developer.vijay.chatie.models.User
import com.developer.vijay.chatie.utils.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.*
import com.developer.vijay.chatie.R
import com.developer.vijay.chatie.ui.activities.view_image.ViewImageActivity

class ChatActivity : BaseActivity() {

    private val mBinding by lazy { ActivityChatBinding.inflate(layoutInflater) }
    private val messageAdapter by lazy {
        MessageAdapter { view, imageUrl ->
            val intent = Intent(this, ViewImageActivity::class.java).apply { putExtra(FirebaseUtils.IMAGE, imageUrl) }
            val transitionName = getString(R.string.transition_name)
            val activityOption = ActivityOptionsCompat.makeSceneTransitionAnimation(this, view, transitionName)
            ActivityCompat.startActivity(this, intent, activityOption.toBundle())
        }
    }
    private val messageList = arrayListOf<Message>()

    private var senderRoom = ""
    private var receiverRoom = ""

    private var currentUser: User? = null
    private var receiverUser: User? = null

    private var resultUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        supportActionBar?.hide()

        receiverUser = Gson().fromJson(intent.getStringExtra(Constants.USER), User::class.java)
        currentUser = PrefUtils.getUser()

        if (currentUser == null) {
            showToast("Can't find SENDER.")
            return
        }

        if (receiverUser == null) {
            showToast("Can't find RECEIVER.")
            return
        }

        firebaseDatabase.reference.child(FirebaseUtils.PRESENCE).child(receiverUser!!.uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(Long::class.java)?.let {
                    when (it) {
                        0L -> mBinding.tvStatus.text = "online"
                        1L -> mBinding.tvStatus.text = "typing..."
                        else -> {
                            val calendar = Calendar.getInstance()
                            val numberFormat = DecimalFormat("#00")
                            calendar.timeInMillis = it
                            val day = calendar.get(Calendar.DAY_OF_WEEK)
                            val hour = calendar.get(Calendar.HOUR)
                            val minute = calendar.get(Calendar.MINUTE)
                            val amPM = calendar.get(Calendar.AM_PM)
                            mBinding.tvStatus.text = "last seen ${GeneralFunctions.getDay(day)} at ${GeneralFunctions.getHour(hour)}:${numberFormat.format(minute)} ${GeneralFunctions.getAmPm(amPM)}"
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

        senderRoom = currentUser!!.uid + receiverUser!!.uid
        receiverRoom = receiverUser!!.uid + currentUser!!.uid

        mBinding.tvName.text = receiverUser!!.name

        GeneralFunctions.loadImage(this, receiverUser!!.profileImage, mBinding.ivProfilePic)

        mBinding.rvChats.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = messageAdapter
        }
        messageAdapter.setData(messageList, senderRoom, receiverRoom)

        getChildValues(FirebaseUtils.CHATS, senderRoom, FirebaseUtils.MESSAGES) { snapshot ->

            messageList.clear()
            for (children in snapshot.children) {
                val message = children.getValue(Message::class.java)
                message?.messageId = children.key!!
                message?.let { messageList.add(it) }
            }

            messageAdapter.setData(messageList, senderRoom, receiverRoom)

            if (messageList.isNotEmpty())
                mBinding.rvChats.scrollToPosition(messageList.size - 1)
        }

        mBinding.btnSend.setOnClickListener {
            val message = mBinding.etMessageBox.text.toString()

            if (message.isEmpty()) {
                mBinding.etMessageBox.error = "Please type message."
                return@setOnClickListener
            }

            val messageObj = Message(message = message, senderId = currentUser!!.uid, timeStamp = Date().time)

            sendTextMessage(messageObj)

        }

        mBinding.ivBack.setOnClickListener { finish() }

        val pickImageContract =
            registerForActivityResult(ActivityResultContracts.GetContent()) { imageUri ->
                imageUri?.let {
                    showProgressDialog("Sending image...")
                    val messageObj = Message(message = FirebaseUtils.IMAGE, senderId = currentUser!!.uid, timeStamp = Date().time)
                    sendImageMessage(messageObj, imageUri)
                } ?: showToast("an ERROR occurred.")
            }

        mBinding.ivAttachment.setOnClickListener {
            pickImageContract.launch("image/*")
        }

        mBinding.etMessageBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                firebaseDatabase.reference.child(FirebaseUtils.PRESENCE).child(currentUser!!.uid).setValue(1)
                lifecycleScope.launch {
                    delay(1000)
                    firebaseDatabase.reference.child(FirebaseUtils.PRESENCE).child(currentUser!!.uid).setValue(0)
                }
            }
        })

        val takePhotoContract = registerForActivityResult(ActivityResultContracts.TakePicture()) { status ->
            if (status) {
                resultUri?.let {
                    showProgressDialog("Sending image...")
                    val messageObj = Message(message = FirebaseUtils.IMAGE, senderId = currentUser!!.uid, timeStamp = Date().time)
                    sendImageMessage(messageObj, it)
                }
            } else
                showToast("an ERROR occurred.")
        }

        val permissionContract = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultMap ->
            resultMap.entries.forEach { entry ->
                if (entry.value) {
                    createImageURI()?.let { takePhotoContract.launch(it) }
                }
            }
        }

        mBinding.ivCamera.setOnClickListener {
            permissionContract.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    private fun createImageURI(): Uri? {

        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val imageName = System.currentTimeMillis()

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$imageName")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        val finalURI = contentResolver.insert(imageCollection, contentValues)
        resultUri = finalURI
        return finalURI
    }

    private fun sendImageMessage(message: Message, imageUri: Uri) {

        val reference = firebaseStorage.reference.child("Chats").child(Calendar.getInstance().timeInMillis.toString())
        reference.putFile(imageUri)
            .addOnCompleteListener { uploadedImageResponse ->
                if (uploadedImageResponse.isSuccessful) {
                    reference.downloadUrl.addOnCompleteListener { uploadedImageDownloadUrlResponse ->
                        if (uploadedImageDownloadUrlResponse.isSuccessful) {
                            message.imageUrl = uploadedImageDownloadUrlResponse.result.toString()
                            sendTextMessage(message)
                        } else {
                            hideProgressDialog()
                            uploadedImageDownloadUrlResponse.exception?.message?.let { showToast(it) }
                        }
                    }
                } else
                    uploadedImageResponse.exception?.message?.let { showToast(it) }
            }

    }

    private fun sendTextMessage(message: Message) {
        val randomKey = firebaseDatabase.reference.push().key.toString()

        mBinding.etMessageBox.text.clear()

        firebaseDatabase.reference.child(FirebaseUtils.CHATS)
            .child(senderRoom)
            .child(FirebaseUtils.MESSAGES)
            .child(randomKey)
            .setValue(message)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    firebaseDatabase.reference.child(FirebaseUtils.CHATS)
                        .child(receiverRoom)
                        .child(FirebaseUtils.MESSAGES)
                        .child(randomKey)
                        .setValue(message)
                        .addOnCompleteListener {
                            hideProgressDialog()
                            if (it.isSuccessful) {
                                currentUser?.let { FirebaseUtils.sendNotification(this, it.name, message.message, receiverUser!!.deviceToken) }
                            } else
                                it.exception?.message?.let { it1 -> showToast(it1) }
                        }


                } else
                    it.exception?.message?.let { it1 -> showToast(it1) }
            }

        val lastMsgMap = hashMapOf<String, Any>()
        lastMsgMap[FirebaseUtils.LAST_MSG] = message.message
        lastMsgMap[FirebaseUtils.LAST_MSG_TIME] = message.timeStamp

        firebaseDatabase.reference.child(FirebaseUtils.CHATS).child(senderRoom).updateChildren(lastMsgMap)
        firebaseDatabase.reference.child(FirebaseUtils.CHATS).child(receiverRoom).updateChildren(lastMsgMap)
    }

}