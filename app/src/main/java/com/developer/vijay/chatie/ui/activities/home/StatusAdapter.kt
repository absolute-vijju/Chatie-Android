package com.developer.vijay.chatie.ui.activities.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.developer.vijay.chatie.databinding.ItemStatusBinding
import com.developer.vijay.chatie.utils.GeneralFunctions
import omari.hamza.storyview.StoryView
import omari.hamza.storyview.callback.StoryClickListeners
import omari.hamza.storyview.model.MyStory
import java.util.*

class StatusAdapter :
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