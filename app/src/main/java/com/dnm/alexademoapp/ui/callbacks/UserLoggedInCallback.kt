package com.dnm.alexademoapp.ui.callbacks

interface UserLoggedInCallback {

    fun userIsLoggedIn(accessToken : String)

    fun userIsLoggedOut()
}