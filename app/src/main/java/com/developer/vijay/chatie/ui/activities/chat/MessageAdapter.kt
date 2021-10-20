package com.developer.vijay.chatie.ui.activities.chat

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.developer.vijay.chatie.R
import com.developer.vijay.chatie.databinding.ItemReceiveBinding
import com.developer.vijay.chatie.databinding.ItemSentBinding
import com.developer.vijay.chatie.utils.FirebaseUtils
import com.github.pgreze.reactions.ReactionPopup
import com.github.pgreze.reactions.ReactionsConfigBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class MessageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var messageList = arrayListOf<Message>()
    private val reactionList = intArrayOf(
        R.drawable.ic_fb_like,
        R.drawable.ic_fb_love,
        R.drawable.ic_fb_laugh,
        R.drawable.ic_fb_wow,
        R.drawable.ic_fb_sad,
        R.drawable.ic_fb_angry
    )
    private var senderRoom = ""
    private var receiverRoom = ""

    private inner class SentViewHolder(val mBinding: ItemSentBinding) :
        RecyclerView.ViewHolder(mBinding.root)

    private inner class ReceiveViewHolder(val mBinding: ItemReceiveBinding) :
        RecyclerView.ViewHolder(mBinding.root)

    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]
        if (message.senderId.equals(FirebaseAuth.getInstance().uid, true))
            return R.layout.item_sent
        return R.layout.item_receive
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == R.layout.item_sent)
            return SentViewHolder(
                ItemSentBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        return ReceiveViewHolder(
            ItemReceiveBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val message = messageList[position]

        val config = ReactionsConfigBuilder(holder.itemView.context)
            .withReactions(reactionList)
            .build()

        if (message.feeling != -1) {
            if (holder is SentViewHolder) {
                holder.mBinding.ivFeeling.isVisible = true
                holder.mBinding.ivFeeling.setImageResource(reactionList[message.feeling])
            }
            if (holder is ReceiveViewHolder) {
                holder.mBinding.ivFeeling.isVisible = true
                holder.mBinding.ivFeeling.setImageResource(reactionList[message.feeling])
            }
        } else {
            if (holder is SentViewHolder) {
                holder.mBinding.ivFeeling.isVisible = false
            }
            if (holder is ReceiveViewHolder) {
                holder.mBinding.ivFeeling.isVisible = false
            }
        }


        val popup = ReactionPopup(holder.itemView.context, config) { reactionPosition ->

            if (holder is SentViewHolder) {
                holder.mBinding.ivFeeling.isVisible = true
                if (reactionPosition != -1)
                    holder.mBinding.ivFeeling.setImageResource(reactionList[reactionPosition])
            }
            if (holder is ReceiveViewHolder) {
                holder.mBinding.ivFeeling.isVisible = true
                if (reactionPosition != -1)
                    holder.mBinding.ivFeeling.setImageResource(reactionList[reactionPosition])
            }

            message.feeling = reactionPosition

            FirebaseDatabase.getInstance().reference
                .child(FirebaseUtils.CHATS)
                .child(senderRoom)
                .child(FirebaseUtils.MESSAGES)
                .child(message.messageId)
                .setValue(message)

            FirebaseDatabase.getInstance().reference
                .child(FirebaseUtils.CHATS)
                .child(receiverRoom)
                .child(FirebaseUtils.MESSAGES)
                .child(message.messageId)
                .setValue(message)


            true
        }

        if (holder is SentViewHolder)
            holder.mBinding.apply {
                tvSentMessage.text = message.message

                tvSentMessage.setOnTouchListener { p0, p1 ->
                    popup.onTouch(p0!!, p1!!)
                    false
                }
            }

        if (holder is ReceiveViewHolder)
            holder.mBinding.apply {
                tvReceivedMessage.text = message.message

                tvReceivedMessage.setOnTouchListener { p0, p1 ->
                    popup.onTouch(p0!!, p1!!)
                    false
                }
            }

    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    fun setData(messageList: ArrayList<Message>, senderRoom: String, receiverRoom: String) {
        this.messageList = messageList
        this.senderRoom = senderRoom
        this.receiverRoom = receiverRoom
        notifyDataSetChanged()
    }
}