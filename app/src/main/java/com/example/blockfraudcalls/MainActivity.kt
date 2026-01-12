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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Button
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
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
                NumberInput(dataStoreManager)
            }
        }
    }
}

@Composable
fun NumberInput( dataStoreManager: DataStoreManager) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var savedPhoneNumber by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        dataStoreManager.getText().collect { savedText ->
            phoneNumber = savedText
            savedPhoneNumber = savedText
        }
    }

    Column (
        modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it.filter { ch -> ch.isDigit() } }, // allow digits only
            label = { Text("Starts With") },
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
                dataStoreManager.saveText(if (savedPhoneNumber.isEmpty()) phoneNumber else "")
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
}
