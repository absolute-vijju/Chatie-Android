package com.developer.vijay.chatie.ui.activities.group_chat

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.*
import com.developer.vijay.chatie.databinding.ActivityGroupChatBinding
import com.developer.vijay.chatie.models.User
import com.developer.vijay.chatie.ui.activities.chat.Message
import com.developer.vijay.chatie.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class GroupChatActivity : BaseActivity() {

    private val mBinding by lazy { ActivityGroupChatBinding.inflate(layoutInflater) }
    private val groupMessageAdapter by lazy { GroupMessageAdapter() }
    private val messageList = arrayListOf<Message>()

    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        supportActionBar?.hide()

        currentUser = PrefUtils.getUser()

        if (currentUser == null) {
            showToast("Can't find SENDER.")
            return
        }

        GeneralFunctions.loadImage(this, "https://www.vippng.com/png/detail/12-126390_png-file-svg-group-chat-transparent.png", mBinding.ivProfilePic)
        mBinding.tvName.text = "Public Group"

        mBinding.rvChats.apply {
            layoutManager = LinearLayoutManager(this@GroupChatActivity)
            adapter = groupMessageAdapter
        }

        groupMessageAdapter.setData(messageList)

        getChildValues(FirebaseUtils.PUBLIC) { snapshot ->

            messageList.clear()
            for (children in snapshot.children) {
                val message = children.getValue(Message::class.java)
                message?.messageId = children.key!!
                message?.let { messageList.add(it) }
            }

            groupMessageAdapter.setData(messageList)

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
    }

    private fun sendImageMessage(message: Message, imageUri: Uri) {

        val reference = firebaseStorage.reference.child(FirebaseUtils.GROUPCHATS).child(Calendar.getInstance().timeInMillis.toString())
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
        mBinding.etMessageBox.text.clear()

        firebaseDatabase.reference
            .child(FirebaseUtils.PUBLIC)
            .push()
            .setValue(message)
            .addOnCompleteListener {
                hideProgressDialog()
                if (it.isSuccessful) {
                    //
                } else
                    it.exception?.message?.let { it1 -> showToast(it1) }
            }

        val lastMsgMap = hashMapOf<String, Any>()
        lastMsgMap[FirebaseUtils.LAST_MSG] = message.message
        lastMsgMap[FirebaseUtils.LAST_MSG_TIME] = message.timeStamp
    }

}