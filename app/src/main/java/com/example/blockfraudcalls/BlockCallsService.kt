package com.example.blockfraudcalls

import android.telecom.Call
import android.telecom.CallScreeningService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BlockCallsService : CallScreeningService() {
    private lateinit var dataStoreManager: DataStoreManager

    override fun onCreate() {
        super.onCreate()
        dataStoreManager = DataStoreManager(this)
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle.schemeSpecificPart ?: return

        CoroutineScope(Dispatchers.IO).launch {
            dataStoreManager.getBlockedNumber().collect { savedNumber ->
                val whitelist = dataStoreManager.getWhitelist().first()
                val response = if (
                    number.startsWith("+$savedNumber")
                    && whitelist.none { it.number == number }
                    ) {
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