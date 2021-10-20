package com.developer.vijay.chatie.ui.activities.chat

import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.developer.vijay.chatie.R
import com.developer.vijay.chatie.databinding.ActivityChatBinding
import com.developer.vijay.chatie.models.User
import com.developer.vijay.chatie.utils.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import java.util.*

class ChatActivity : BaseActivity() {

    private val mBinding by lazy { ActivityChatBinding.inflate(layoutInflater) }
    private val messageAdapter by lazy { MessageAdapter() }
    private val messageList = arrayListOf<Message>()

    private var senderRoom = ""
    private var receiverRoom = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        supportActionBar?.hide()

        val user = Gson().fromJson(intent.getStringExtra(Constants.USER), User::class.java)
        val receiverUid = intent.getStringExtra(Constants.UID)
        val senderUid = firebaseAuth.uid

        senderRoom = senderUid + receiverUid
        receiverRoom = receiverUid + senderUid

        mBinding.tvName.text = user.name

        GeneralFunctions.loadImage(this, user.profileImage, mBinding.ivProfilePic)

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
        }

        mBinding.btnSend.setOnClickListener {
            val message = mBinding.etMessageBox.text.toString()

            if (message.isEmpty()) {
                mBinding.etMessageBox.error = "Please type message."
                return@setOnClickListener
            }

            val messageObj =
                Message(message = message, senderId = senderUid.toString(), timeStamp = Date().time)

            val randomKey = firebaseDatabase.reference.push().key.toString()

            mBinding.etMessageBox.text.clear()

            firebaseDatabase.reference.child(FirebaseUtils.CHATS)
                .child(senderRoom)
                .child(FirebaseUtils.MESSAGES)
                .child(randomKey)
                .setValue(messageObj)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        firebaseDatabase.reference.child(FirebaseUtils.CHATS)
                            .child(receiverRoom)
                            .child(FirebaseUtils.MESSAGES)
                            .child(randomKey)
                            .setValue(messageObj)
                            .addOnCompleteListener {
                                if (it.isSuccessful)
                                else
                                    it.exception?.message?.let { it1 -> showToast(it1) }
                            }


                    } else
                        it.exception?.message?.let { it1 -> showToast(it1) }
                }

            val lastMsgMap = hashMapOf<String, Any>()
            lastMsgMap[FirebaseUtils.LASTMSG] = messageObj.message
            lastMsgMap[FirebaseUtils.LASTMSGTIME] = messageObj.timeStamp

            firebaseDatabase.reference.child(FirebaseUtils.CHATS).child(senderRoom).updateChildren(lastMsgMap)
            firebaseDatabase.reference.child(FirebaseUtils.CHATS).child(receiverRoom).updateChildren(lastMsgMap)
        }

        mBinding.ivBack.setOnClickListener { finish() }
    }
}