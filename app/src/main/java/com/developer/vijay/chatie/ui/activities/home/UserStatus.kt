package com.developer.vijay.chatie.ui.activities.home

data class UserStatus(
    var name: String = "",
    var profileImage: String = "",
    var lastUpdated: Long = 0L,
    var statuses: ArrayList<Status> = arrayListOf()
)
