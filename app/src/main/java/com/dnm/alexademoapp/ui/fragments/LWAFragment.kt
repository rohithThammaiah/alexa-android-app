package com.dnm.alexademoapp.ui.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dnm.alexademoapp.R
import com.dnm.alexademoapp.ui.callbacks.AdapterInterface
import com.jamitlabs.alexaconnect.libraries.alexa.AlexaManager
import com.jamitlabs.alexaconnect.libraries.alexa.callbacks.AuthorizationCallback
import kotlinx.android.synthetic.main.activity_main.view.*
import java.lang.Exception

class LWAFragment: Fragment() {

    private var vw: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        vw = View.inflate(context,R.layout.activity_main,null)

        vw?.login_with_amazon_test?.setOnClickListener {
            /*(activity as? MainActivity)?.loginWithAmazon()*/

            val alexaManager = AlexaManager.getInstance(context)

            alexaManager.logIn(object : AuthorizationCallback{
                override fun onCancel() {

                }

                override fun onSuccess() {
                    (activity as AdapterInterface).setAdapterTitle("AudioSenderFragment",Bundle())
                }

                override fun onError(error: Exception?) {

                }

            })
        }

        return vw
    }
}