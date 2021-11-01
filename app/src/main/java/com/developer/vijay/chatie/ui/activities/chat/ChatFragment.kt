package com.developer.vijay.chatie.ui.activities.chat

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.developer.vijay.chatie.R
import com.developer.vijay.chatie.databinding.FragmentChatBinding
import com.developer.vijay.chatie.models.User
import com.developer.vijay.chatie.ui.activities.home.UserAdapter
import com.developer.vijay.chatie.ui.activities.view_image.ViewImageActivity
import com.developer.vijay.chatie.utils.BaseActivity
import com.developer.vijay.chatie.utils.Constants
import com.developer.vijay.chatie.utils.FirebaseUtils
import com.developer.vijay.chatie.utils.PrefUtils
import com.google.gson.Gson

class ChatFragment : Fragment(R.layout.fragment_chat) {

    private val mBinding: FragmentChatBinding by lazy { FragmentChatBinding.bind(requireView()) }
    private var userList = arrayListOf<User>()
    private val userAdapter by lazy {
        UserAdapter { view, position ->
            when (view.id) {
                R.id.ivProfilePic -> {
                    val intent = Intent(requireContext(), ViewImageActivity::class.java).apply { putExtra(FirebaseUtils.IMAGE, userList[position].profileImage) }
                    val transitionName = getString(R.string.transition_name)
                    val activityOption = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view, transitionName)
                    ActivityCompat.startActivity(requireContext(), intent, activityOption.toBundle())
                }
                else -> startActivity(Intent(requireContext(), ChatActivity::class.java).apply { putExtra(Constants.USER, Gson().toJson(userList[position])) })
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.rvUsers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = userAdapter
        }
        mBinding.rvUsers.showShimmerAdapter()

        (requireActivity() as BaseActivity).getChildValues(FirebaseUtils.USERS) { snapshot ->
            userList.clear()

            for (children in snapshot.children) {
                val user = children.getValue(User::class.java)
                if (!user?.uid.equals(PrefUtils.getUser()?.uid))
                    user?.let { userList.add(it) }
            }

            mBinding.rvUsers.hideShimmerAdapter()
            userAdapter.setData(userList)
        }
    }

    fun searchInData(query: String) {
        val newList = arrayListOf<User>()
        for (user in userList) {
            if (user.name.lowercase().contains(query.lowercase()))
                newList.add(user)
        }

        if (query.isEmpty() && newList.isEmpty())
            userAdapter.setData(userList)
        else
            userAdapter.setData(newList)
    }
}