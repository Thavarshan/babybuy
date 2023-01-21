package edu.sunderland.babybuy

import android.content.Context
import android.content.SharedPreferences

class Auth(context: Context) {
    private val preferences: SharedPreferences
    private val editor: SharedPreferences.Editor

    fun createLoginSession() {
        editor.putBoolean(IS_LOGGED_IN, true)
        editor.commit()
        editor.apply()
    }

    fun logoutSession() {
        editor.putBoolean(IS_LOGGED_IN, false)
        editor.commit()
        editor.apply()
    }

    companion object {
        private const val IS_LOGGED_IN = "IsLoggedIn"
    }

    init {
        preferences = context.getSharedPreferences("BabyBuy", 0)
        editor = preferences.edit()
    }
}