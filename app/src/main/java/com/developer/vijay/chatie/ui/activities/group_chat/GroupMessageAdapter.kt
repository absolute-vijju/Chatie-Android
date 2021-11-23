package com.developer.vijay.chatie.ui.activities.group_chat

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.developer.vijay.chatie.R
import com.developer.vijay.chatie.databinding.ItemReceiveGroupBinding
import com.developer.vijay.chatie.databinding.ItemSentGroupBinding
import com.developer.vijay.chatie.models.User
import com.developer.vijay.chatie.ui.activities.chat.Message
import com.developer.vijay.chatie.utils.FirebaseUtils
import com.developer.vijay.chatie.utils.GeneralFunctions
import com.github.pgreze.reactions.ReactionPopup
import com.github.pgreze.reactions.ReactionsConfigBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GroupMessageAdapter(private val onClick: (view: View, imageUrl: String) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var messageList = arrayListOf<Message>()
    private val reactionList = intArrayOf(
        R.drawable.ic_add_reaction,
        R.drawable.ic_fb_like,
        R.drawable.ic_fb_love,
        R.drawable.ic_fb_laugh,
        R.drawable.ic_fb_wow,
        R.drawable.ic_fb_sad,
        R.drawable.ic_fb_angry
    )

    private inner class SentViewHolder(val mBinding: ItemSentGroupBinding) :
        RecyclerView.ViewHolder(mBinding.root)

    private inner class ReceiveViewHolder(val mBinding: ItemReceiveGroupBinding) :
        RecyclerView.ViewHolder(mBinding.root)

    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]
        if (message.senderId.equals(FirebaseAuth.getInstance().uid, true))
            return R.layout.item_sent
        return R.layout.item_receive
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == R.layout.item_sent)
            return SentViewHolder(ItemSentGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        return ReceiveViewHolder(ItemReceiveGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val message = messageList[position]

        val config = ReactionsConfigBuilder(holder.itemView.context)
            .withReactions(reactionList)
            .build()

        val popup = ReactionPopup(holder.itemView.context, config) { reactionPosition ->

            if (reactionPosition != -1) {
                if (holder is SentViewHolder)
                    holder.mBinding.ivFeeling.setImageResource(reactionList[reactionPosition])

                if (holder is ReceiveViewHolder)
                    holder.mBinding.ivFeeling.setImageResource(reactionList[reactionPosition])

                message.feeling = reactionPosition
            }

            FirebaseDatabase.getInstance().reference
                .child(FirebaseUtils.PUBLIC)
                .child(message.messageId)
                .setValue(message)


            true
        }

        if (holder is SentViewHolder)
            holder.mBinding.apply {

                ivFeeling.setImageResource(reactionList[message.feeling])

                (this.root.context as GroupChatActivity).firebaseDatabase.reference
                    .child(FirebaseUtils.USERS)
                    .child(message.senderId)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val user = snapshot.getValue(User::class.java)
                            tvUsername.text = user?.name
                        }

                        override fun onCancelled(error: DatabaseError) {

                        }
                    })

                if (message.message.equals(FirebaseUtils.IMAGE, true)) {
                    tvSentMessage.isVisible = false
                    ivSent.isVisible = true
                    GeneralFunctions.loadImage(this.root.context, message.imageUrl, ivSent)
                } else {
                    tvSentMessage.isVisible = true
                    ivSent.isVisible = false
                    tvSentMessage.text = message.message
                }

                ivFeeling.setOnTouchListener { p0, p1 ->
                    popup.onTouch(p0!!, p1!!)
                    false
                }


                if (!message.message.equals("You deleted this message.", true)) {

                    if (!message.message.equals("This message was deleted.", true)) {

                        root.setOnLongClickListener {

                            AlertDialog.Builder(it.context)
                                .setTitle("Delete message?")
                                .setPositiveButton("Delete For Me") { dialogInterface, i ->
                                    FirebaseDatabase.getInstance().reference
                                        .child(FirebaseUtils.PUBLIC)
                                        .child(message.messageId)
                                        .removeValue()
                                }
                                .setNegativeButton("Cancel") { dialogInterface, i ->
                                    dialogInterface.dismiss()
                                }
                                .setNeutralButton("Delete For Everyone") { dialogInterface, i ->

                                    message.message = "You deleted this message."

                                    FirebaseDatabase.getInstance().reference
                                        .child(FirebaseUtils.PUBLIC)
                                        .child(message.messageId)
                                        .setValue(message)

                                    message.message = "This message was deleted."

                                    FirebaseDatabase.getInstance().reference
                                        .child(FirebaseUtils.PUBLIC)
                                        .child(message.messageId)
                                        .setValue(message)
                                }
                                .show()
                            true
                        }
                    }
                }

                root.setOnClickListener {
                    if (message.message.equals(FirebaseUtils.IMAGE, true)) {
                        onClick(root, message.imageUrl)
                    }
                }

            }

        if (holder is ReceiveViewHolder)
            holder.mBinding.apply {

                ivFeeling.setImageResource(reactionList[message.feeling])

                (this.root.context as GroupChatActivity).firebaseDatabase.reference
                    .child(FirebaseUtils.USERS)
                    .child(message.senderId)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val user = snapshot.getValue(User::class.java)
                            tvUsername.text = user?.name
                        }

                        override fun onCancelled(error: DatabaseError) {

                        }
                    })

                if (message.message.equals(FirebaseUtils.IMAGE, true)) {
                    tvReceivedMessage.isVisible = false
                    ivReceive.isVisible = true
                    GeneralFunctions.loadImage(this.root.context, message.imageUrl, ivReceive)
                } else {
                    tvReceivedMessage.isVisible = true
                    ivReceive.isVisible = false
                    tvReceivedMessage.text = message.message
                }

                ivFeeling.setOnTouchListener { p0, p1 ->
                    popup.onTouch(p0!!, p1!!)
                    false
                }

                if (!message.message.equals("This message was deleted.", true)) {

                    root.setOnLongClickListener {

                        AlertDialog.Builder(it.context)
                            .setTitle("Delete message?")
                            .setPositiveButton("Delete For Me") { dialogInterface, i ->
                                FirebaseDatabase.getInstance().reference
                                    .child(FirebaseUtils.PUBLIC)
                                    .child(message.messageId)
                                    .removeValue()
                            }
                            .setNegativeButton("Cancel") { dialogInterface, i ->
                                dialogInterface.dismiss()
                            }
                            .show()
                        true
                    }

                }

                root.setOnClickListener {
                    if (message.message.equals(FirebaseUtils.IMAGE, true)) {
                        onClick(root, message.imageUrl)
                    }
                }
            }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    fun setData(messageList: ArrayList<Message>) {
        this.messageList = messageList
        notifyDataSetChanged()
    }
}