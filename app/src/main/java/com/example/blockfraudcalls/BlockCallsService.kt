package com.example.blockfraudcalls

import android.telecom.Call
import android.telecom.CallScreeningService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BlockCallsService : CallScreeningService() {
    private lateinit var dataStoreManager: DataStoreManager

    val whiteList = mutableListOf(
        "+380992307325",
        "+380957664516"
    )

    override fun onCreate() {
        super.onCreate()
        dataStoreManager = DataStoreManager(this)
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle.schemeSpecificPart ?: return

        CoroutineScope(Dispatchers.IO).launch {
            dataStoreManager.getText().collect { savedNumber ->
                val response = if (number.startsWith("+$savedNumber") && number !in whiteList) {
                    CallResponse.Builder()
                        .setRejectCall(true)
                        .setDisallowCall(true)
                        .setSkipNotification(true)
                        .build()
                } else {
                    CallResponse.Builder().build()
                }

                respondToCall(callDetails, response)
            }
        }
    }
}