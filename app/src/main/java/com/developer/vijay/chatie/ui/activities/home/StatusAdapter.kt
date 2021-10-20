package com.developer.vijay.chatie.ui.activities.home

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.developer.vijay.chatie.R
import com.developer.vijay.chatie.databinding.ItemConversationBinding
import com.developer.vijay.chatie.databinding.ItemStatusBinding
import com.developer.vijay.chatie.models.User
import com.developer.vijay.chatie.utils.BaseActivity
import com.developer.vijay.chatie.utils.GeneralFunctions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import omari.hamza.storyview.model.MyStory
import java.text.SimpleDateFormat
import java.util.*
import omari.hamza.storyview.callback.StoryClickListeners

import omari.hamza.storyview.StoryView


class StatusAdapter(val onClick: (position: Int) -> Unit) :
    RecyclerView.Adapter<StatusAdapter.StatusViewHolder>() {

    private var userStatuses = arrayListOf<UserStatus>()

    inner class StatusViewHolder(val mBinding: ItemStatusBinding) :
        RecyclerView.ViewHolder(mBinding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusViewHolder {
        return StatusViewHolder(
            ItemStatusBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int) {

        val userStatus = userStatuses[position]

        holder.mBinding.apply {

            if (userStatus.statuses.isNotEmpty())
                GeneralFunctions.loadImage(
                    root.context,
                    userStatus.statuses[userStatus.statuses.size - 1].imageUrl,
                    ivThumb
                )

            csvStatus.setPortionsCount(userStatus.statuses.size)

            rlStatus.setOnClickListener {

                val myStories = arrayListOf<MyStory>()
                for (status in userStatus.statuses)
                    myStories.add(MyStory(status.imageUrl))

                StoryView.Builder((holder.mBinding.root.context as HomeActivity).supportFragmentManager)
                    .setStoriesList(myStories) // Required
                    .setStoryDuration(5000) // Default is 2000 Millis (2 Seconds)
                    .setTitleText(userStatus.name) // Default is Hidden
                    .setSubtitleText("") // Default is Hidden
                    .setTitleLogoUrl(userStatus.profileImage) // Default is Hidden
                    .setStoryClickListeners(object : StoryClickListeners {
                        override fun onDescriptionClickListener(position: Int) {
                            //your action
                        }

                        override fun onTitleIconClickListener(position: Int) {
                            //your action
                        }
                    }) // Optional Listeners
                    .build() // Must be called before calling show method
                    .show()
            }

        }

    }

    override fun getItemCount(): Int {
        return userStatuses.size
    }

    fun setData(userStatuses: ArrayList<UserStatus>) {
        this.userStatuses = userStatuses
        notifyDataSetChanged()
    }
}