package com.developer.vijay.chatie.ui.activities.home

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.SearchView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.developer.vijay.chatie.R
import com.developer.vijay.chatie.databinding.ActivityHomeBinding
import com.developer.vijay.chatie.ui.activities.setup_profile.SetupProfileActivity
import com.developer.vijay.chatie.ui.activities.call.CallFragment
import com.developer.vijay.chatie.ui.activities.chat.ChatFragment
import com.developer.vijay.chatie.ui.activities.group_chat.GroupChatActivity
import com.developer.vijay.chatie.ui.activities.status.StatusFragment
import com.developer.vijay.chatie.utils.*
import com.google.android.material.tabs.TabLayout

class HomeActivity : BaseActivity(), SearchView.OnQueryTextListener {

    private val mBinding by lazy { ActivityHomeBinding.inflate(layoutInflater) }
    private val fragmentList = arrayListOf(ChatFragment(), StatusFragment(), CallFragment())
    private val pagerAdapter by lazy { PagerAdapter(fragmentList, supportFragmentManager, lifecycle) }
    private var currentPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        setupTab()
        init()
        getBackgroundFromFirebase(mBinding.viewPager)
        setDeviceToken()

    }

    private fun setupTab() {

        mBinding.viewPager.apply {
            adapter = pagerAdapter
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    mBinding.tabLayout.selectTab(mBinding.tabLayout.getTabAt(position))
                }
            })
        }

        mBinding.tabLayout.apply {
            addTab(newTab().setText("Chats"))
            addTab(newTab().setText("Status"))
            addTab(newTab().setText("Calls"))
            tabGravity = TabLayout.GRAVITY_FILL

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.let {
                        currentPosition = it.position
                        mBinding.viewPager.currentItem = it.position
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        }

    }

    private fun setDeviceToken() {
        firebaseMessaging.token.addOnCompleteListener { response ->
            if (response.isSuccessful)
                response.result?.let { it ->
                    PrefUtils.set(FirebaseUtils.DEVICE_TOKEN, it)
                    val map = hashMapOf<String, Any>()
                    map[FirebaseUtils.DEVICE_TOKEN] = it

                    PrefUtils.getUser()?.uid?.let { it1 ->
                        firebaseDatabase.reference.child(FirebaseUtils.USERS)
                            .child(it1)
                            .updateChildren(map)
                    }
                }
            else
                PrefUtils.set(FirebaseUtils.DEVICE_TOKEN, "")
        }
    }

    private fun init() {
        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener {
            if (it.isSuccessful) {
                val backgroundImageUrl = firebaseRemoteConfig.getString(FirebaseUtils.BACKGROUND_IMAGE_URL)
                val backgroundColor = firebaseRemoteConfig.getString(FirebaseUtils.BACKGROUND_COLOR)
                val showBackgroundImage = firebaseRemoteConfig.getBoolean(FirebaseUtils.SHOW_BACKGROUND_IMAGE)

                if (showBackgroundImage) {
                    Glide.with(this).load(backgroundImageUrl).into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            mBinding.viewPager.background = resource
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {

                        }

                    })
                } else {
                    mBinding.viewPager.setBackgroundColor(Color.parseColor(backgroundColor))
                }
            } else
                it.exception?.message?.let { it1 -> showToast(it1) }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mSearch -> {

            }
            R.id.mGroup -> {
                startActivity(Intent(this, GroupChatActivity::class.java))
            }
            R.id.mInvite -> {
                showToast("Coming soon...")
            }
            R.id.mSetting -> {
                startActivity(Intent(this, SetupProfileActivity::class.java).apply {
                    putExtra(Constants.IS_FIRST_TIME, false)
                })
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private var searchView: SearchView? = null

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.topmenu, menu)
        val search = menu?.findItem(R.id.mSearch)
        searchView = search?.actionView as? SearchView
        searchView?.apply {
            isSubmitButtonEnabled = true
            setOnQueryTextListener(this@HomeActivity)
            queryHint = "type friend name..."
            imeOptions = EditorInfo.IME_ACTION_SEARCH
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        if (currentPosition == 0) {
            val chatFragment = fragmentList[0] as ChatFragment
            query?.let { chatFragment.searchInData(it) }
        }
        hideKeyboard()
        return true
    }

    override fun onQueryTextChange(query: String?): Boolean {
        if (currentPosition == 0) {
            val chatFragment = fragmentList[0] as ChatFragment
            query?.let { chatFragment.searchInData(it) }
        }
        return true
    }
}