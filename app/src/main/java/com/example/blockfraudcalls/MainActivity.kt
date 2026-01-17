package com.example.blockfraudcalls

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.blockfraudcalls.ui.theme.BlockFraudCallsTheme
import android.app.role.RoleManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.collectAsState

import java.util.UUID

import com.example.blockfraudcalls.model.WhitelistNumber
import kotlinx.coroutines.flow.first

import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var requestRoleLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        dataStoreManager = DataStoreManager(this)
        requestRoleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                showAppUI(dataStoreManager)
            }
        }

        val roleManager = getSystemService(RoleManager::class.java)

        if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            requestRoleLauncher.launch(intent)
        } else {
            showAppUI(dataStoreManager)
        }
    }

    private fun showAppUI(dataStoreManager: DataStoreManager) {
        setContent {
            BlockFraudCallsTheme {
                AppUi(dataStoreManager)
            }
        }
    }
}

@Composable
fun AppUi( dataStoreManager: DataStoreManager) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val colorGreen = Color(12, 181, 0)

    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var savedPhoneNumber by rememberSaveable { mutableStateOf("") }

    val whitelist by dataStoreManager
        .getWhitelist()
        .collectAsState(initial = emptyList())

    var whitelistNumber by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    LaunchedEffect(Unit) {
        dataStoreManager.getBlockedNumber().collect { savedText ->
            phoneNumber = savedText
            savedPhoneNumber = savedText
        }
    }

    Column(
        modifier = Modifier
            .padding(10.dp, 50.dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Whitelist",
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp,
            color = colorGreen
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(0.dp, 50.dp)
                .fillMaxWidth()
        ) {
            items(whitelist) { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .padding(vertical = 4.dp, horizontal = 20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "+${item.number}",
                        color = colorGreen,
                        modifier = Modifier.padding(end = 30.dp).weight(1f),
                        maxLines = 1
                    )

                    IconButton (
                        onClick = {
                            coroutineScope.launch {
                                val current = dataStoreManager.getWhitelist().first()
                                val updatedWhitelist = current.filter { it.id != item.id }
                                dataStoreManager.saveToWhitelist(updatedWhitelist)
                            }
                        },
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = Color(255, 0, 30, 255)
                        )
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Column (
                modifier = Modifier
                    .weight(1f)
                    .padding(0.dp, 0.dp, 10.dp),
            ) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it.filter { ch -> ch.isDigit() } }, // allow digits only
                    label = { Text("Block By Prefix") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = !savedPhoneNumber.isEmpty(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            dataStoreManager.saveBlockedNumber(if (savedPhoneNumber.isEmpty()) phoneNumber else "")
                            focusManager.clearFocus()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(if (savedPhoneNumber.isEmpty()) "Block" else "Unblock")
                }
            }

            Column (
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp, 0.dp, 0.dp),
            ) {
                OutlinedTextField(
                    value = whitelistNumber,
                    onValueChange = { input ->
                        val filtered = input.text.filter { it.isDigit() }.take(15)

                        whitelistNumber = TextFieldValue(
                            text = filtered,
                            selection = TextRange(filtered.length)
                        )
                    },
                    label = { Text("Add To Whitelist") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (!whitelistNumber.text.isEmpty()) {
                            val uniqueId = UUID.randomUUID().toString()
                            val newWhitelistNumber = WhitelistNumber(uniqueId, whitelistNumber.text)

                            coroutineScope.launch {
                                val current = dataStoreManager.getWhitelist().first()
                                val updatedWhitelist = current + newWhitelistNumber

                                if (current.none { it.number == whitelistNumber.text }) {
                                    dataStoreManager.saveToWhitelist(updatedWhitelist)
                                }

                                focusManager.clearFocus()

                                whitelistNumber = TextFieldValue("")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("Add")
                }
            }
        }
    }
}
