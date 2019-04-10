package com.dnm.alexademoapp.ui

import android.content.Context
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.support.annotation.RequiresApi
import android.support.v4.app.Fragment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.api.Listener
import com.amazon.identity.auth.device.api.authorization.*
import com.amazon.identity.auth.device.api.workflow.RequestContext
import com.amazon.identity.auth.device.api.authorization.AuthorizeResult
import com.dnm.alexademoapp.AlexaApplication
import com.dnm.alexademoapp.BuildConfig
import com.dnm.alexademoapp.R
import com.dnm.alexademoapp.ui.callbacks.AdapterInterface
import com.dnm.alexademoapp.ui.callbacks.UserLoggedInCallback
import com.dnm.alexademoapp.ui.fragments.AudioSenderFragment
import com.dnm.alexademoapp.ui.fragments.LWAFragment
import com.dnm.alexademoapp.utils.Constants.Companion.PRODUCT_ID
import com.dnm.alexademoapp.utils.SigningKey
import org.json.JSONException
import org.json.JSONObject
import com.jamitlabs.alexaconnect.libraries.alexa.AlexaManager
import com.jamitlabs.alexaconnect.libraries.alexa.callbacks.AuthorizationCallback
import java.lang.Exception


class MainActivity :  UserLoggedInCallback, AdapterInterface, BaseActivity() {
    override fun startListening() {
        val fragment = supportFragmentManager.findFragmentByTag("AudioSenderFragment")
        if (fragment != null && fragment.isVisible) {
            if (fragment is BaseListenerFragment) {
                fragment.startListening()
            }
        }
    }

    override fun stateListening() {

    }

    override fun stateProcessing() {

    }

    override fun stateSpeaking() {

    }

    override fun stateFinished() {

    }

    override fun statePrompting() {

    }

    override fun stateNone() {

    }


    private var requestContext:RequestContext? = null

    private var alexaManager: AlexaManager? = null

    private val productID = "voice_enabled_resturant_app" // spell check
    private val productDSN = "12345"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.base_activity)

        alexaManager = AlexaManager.getInstance(this)

        if(BuildConfig.DEBUG) {
            Log.i("Key", SigningKey.getCertificateMD5Fingerprint(this))
        }

        requestContext = RequestContext.create(this)

        requestContext?.registerListener(AuthorizationListenerImpl())

        setAdapterTitle("LWAFragment",null)
    }

    fun loginWithAmazon(){
        try {

            val scopeData  = JSONObject()
            val productInstanceAttributes = JSONObject()

            productInstanceAttributes.put("deviceSerialNumber",productDSN)
            scopeData.put("productInstanceAttributes", productInstanceAttributes)
            scopeData.put("productID",productID)

            AuthorizationManager.authorize( AuthorizeRequest.Builder(requestContext)
                    .addScopes(ScopeFactory.scopeNamed("alexa:voice_service:pre_auth"),
                            ScopeFactory.scopeNamed("alexa:all", scopeData))
                    .forGrantType(AuthorizeRequest.GrantType.ACCESS_TOKEN)
                    .shouldReturnUserData(false)
                    .build())
        }catch (jsonException: JSONException){

        }
    }


    override fun setAdapterTitle(fragmentName: String, attrs: Bundle?) {
        when(fragmentName){

            "AudioSenderFragment" -> {
                openFragment(AudioSenderFragment().apply {
                    arguments = attrs
                })
            }

            "LWAFragment" -> {
                openFragment(LWAFragment().apply {
                    arguments = attrs
                })
            }

            else -> {
                Toast.makeText(this,"$fragmentName does not exist",Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openFragment(fragment: Fragment){

        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container,fragment,fragment.tag)
                .addToBackStack(null)
                .commit()
    }

    private var menu: Menu? = null

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.my_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when (item?.itemId){
            R.id.logout_btn -> {
                logoutWithAmazon()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun logoutWithAmazon(){
        AuthorizationManager.signOut(applicationContext, object : Listener<Void, AuthError> {
            override fun onSuccess(response: Void?) {
                // Set logged out state in UI
                userIsLoggedOut()
                val prefs = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE).edit()

                prefs?.putBoolean("logged_in",false)
                prefs?.apply()

                runOnUiThread {
                    Toast.makeText(this@MainActivity,"Successfully logged out",Toast.LENGTH_LONG).show()
                }


            }

            override fun onError(authError: AuthError) {
                // Log the error
                runOnUiThread {
                    Toast.makeText(this@MainActivity,"Could not logout user",Toast.LENGTH_LONG).show()
                }
            }
        })


    }


    override fun onResume() {
        super.onResume()

        requestContext?.onResume()
    }

    override fun onStart() {
        super.onStart()
        // Check if user is logged in
        AuthorizationManager.getToken(this, arrayOf(ScopeFactory.scopeNamed("alexa:all") ),object : Listener<AuthorizeResult, AuthError> {

            override fun onSuccess(result: AuthorizeResult) {
                if (result.accessToken != null) {
                     //The user is signed in

                    Log.e("MainActivity", result.accessToken)
                    runOnUiThread {
                       userIsLoggedIn(result.accessToken)
                    }
                } else {
                    runOnUiThread {
                        userIsLoggedOut()
                        Toast.makeText(this@MainActivity,"The user is not signed in",Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onError(ae: AuthError) {
                runOnUiThread {
                    userIsLoggedOut()
                    Toast.makeText(this@MainActivity,"The user is not signed in",Toast.LENGTH_LONG).show()
                }
            }
        })
    }


    inner class AuthorizationListenerImpl: AuthorizeListener(){
        override fun onSuccess(authorizeResult: AuthorizeResult?) {

            val accessToken = authorizeResult?.accessToken

            userIsLoggedIn(accessToken?:"")


            /**
             * Make a POST network request here to https://api.amazon.com/auth/O2/token
             * Request Parameters:
             * hashMap["grant_type"] = authorizationCode
             * hashMap["redirect_uri"] = redirectUri
             * hashMap["client_id"] = clientId
             * hashMap["code"] = // Auth code received from the app
             * hashMap["code_verifier"] = // The code verifier that was initially generated by the product. Encrypt codeChallenge
             *
             * Response:
             * access_token //The access token is valid only for 5 seconds
             * refresh_token //
             * token_type // bearer
             * expires_in // is in seconds
             * */

            Log.e("Alexa","$accessToken")
        }

        override fun onCancel(p0: AuthCancellation?) {
            userIsLoggedOut()
        }

        override fun onError(p0: AuthError?) {
            userIsLoggedOut()
        }

    }

    /*fun hashUsingSHA256(stringToBeEncrypted: String): String{

        var encryptedString = stringToBeEncrypted

        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(stringToBeEncrypted.toByteArray(StandardCharsets.UTF_8))
        if (hash != null) {
            encryptedString = Base64.encodeToString(hash,Base64.NO_PADDING or Base64.URL_SAFE or Base64.NO_WRAP)
        }

        return encryptedString
    }*/


    override fun userIsLoggedIn(accessToken: String) {
        if (accessToken != ""){
            AlexaApplication.globalAccessToken = accessToken

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                if (isInMainThread()) {
                    makeLogoutBtnVisible(true)
                    val bundle = Bundle()
                    bundle.putString("accessToken",accessToken)
                    setAdapterTitle("AudioSenderFragment",bundle)
                } else {
                    runOnUiThread {
                        makeLogoutBtnVisible(true)
                        val bundle = Bundle()
                        bundle.putString("accessToken",accessToken)
                        setAdapterTitle("AudioSenderFragment",bundle)
                    }
                }
            }
        }
    }


    override fun userIsLoggedOut() {
        AlexaApplication.globalAccessToken = ""

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (isInMainThread()) {
                makeLogoutBtnVisible(false)
                setAdapterTitle("LWAFragment", Bundle())
            } else {
                runOnUiThread {
                    makeLogoutBtnVisible(false)
                    setAdapterTitle("LWAFragment",Bundle())
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun isInMainThread(): Boolean{
        return Looper.getMainLooper().isCurrentThread
    }

    private fun makeLogoutBtnVisible(loggedIn: Boolean):Boolean{
        val register = menu?.findItem(R.id.logout_btn)
        register?.isVisible = loggedIn  //userRegistered is boolean, pointing if the user has registered or not.
        return true
    }
}
