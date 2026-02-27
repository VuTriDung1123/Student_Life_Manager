package com.personal.studentlifemanager

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                StudentLifeApp()
            }
        }
    }
}

@Composable
fun StudentLifeApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
    var userName by remember { mutableStateOf(auth.currentUser?.displayName ?: "") }

    if (!isLoggedIn) {
        LoginScreen(
            onLoginSuccess = { name ->
                userName = name
                isLoggedIn = true
            }
        )
    } else {
        HomeScreen(
            userName = userName,
            onLogout = {
                auth.signOut()
                isLoggedIn = false
                userName = ""
            }
        )
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val auth = FirebaseAuth.getInstance()

    // --- CẤU HÌNH GOOGLE SIGN-IN ---
    // DÁN WEB CLIENT ID CỦA BẠN VÀO ĐÂY:
    val webClientId = context.getString(R.string.default_web_client_id)

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    // Luồng lắng nghe kết quả trả về từ Google
    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                // Bắn token lên Firebase để xác thực
                auth.signInWithCredential(credential).addOnCompleteListener(activity!!) { signInTask ->
                    if (signInTask.isSuccessful) {
                        onLoginSuccess(auth.currentUser?.displayName ?: "Người dùng Google")
                    } else {
                        Toast.makeText(context, "Lỗi Firebase: ${signInTask.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Lỗi Google Sign-In: Lỗi số ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Student Life\nManager",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Nút Google Thật
            Button(
                onClick = {
                    googleSignInClient.signOut().addOnCompleteListener {
                        // Mở popup chọn tài khoản Google
                        googleAuthLauncher.launch(googleSignInClient.signInIntent)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Đăng nhập bằng Google", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nút GitHub Thật
            OutlinedButton(
                onClick = {
                    if (activity != null) {
                        val provider = OAuthProvider.newBuilder("github.com")
                        provider.scopes = listOf("user:email")
                        auth.startActivityForSignInWithProvider(activity, provider.build())
                            .addOnSuccessListener { result ->
                                val name = result.user?.displayName ?: result.user?.email ?: "Người dùng GitHub"
                                onLoginSuccess(name)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Lỗi GitHub: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Đăng nhập bằng GitHub", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HomeScreen(userName: String, onLogout: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Đăng nhập thành công!", fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = userName,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(onClick = onLogout) {
                Text("Đăng xuất")
            }
        }
    }
}