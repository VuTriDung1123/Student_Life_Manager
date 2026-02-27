package com.personal.studentlifemanager.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.personal.studentlifemanager.R // Đảm bảo đúng package R của dự án

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current as? Activity
    val auth = FirebaseAuth.getInstance()

    // --- CẤU HÌNH GOOGLE SIGN-IN ---
    // Lấy Client ID từ file strings.xml (được tạo tự động bởi Google Service plugin)
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

                // Xác thực với Firebase
                auth.signInWithCredential(credential).addOnCompleteListener { signInTask ->
                    if (signInTask.isSuccessful) {
                        onLoginSuccess(auth.currentUser?.displayName ?: "Người dùng Google")
                    } else {
                        Toast.makeText(context, "Lỗi Firebase: ${signInTask.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Lỗi Google: ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Tên App với Style hiện đại
            Text(
                text = "Student Life\nManager",
                fontSize = 40.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Sáng tạo - Hiệu suất - Kỷ luật",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Nút Đăng nhập Google
            Button(
                onClick = {
                    googleSignInClient.signOut().addOnCompleteListener {
                        googleAuthLauncher.launch(googleSignInClient.signInIntent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Đăng nhập bằng Google", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nút Đăng nhập GitHub
            OutlinedButton(
                onClick = {
                    activity?.let {
                        val provider = OAuthProvider.newBuilder("github.com")
                        provider.scopes = listOf("user:email")
                        auth.startActivityForSignInWithProvider(it, provider.build())
                            .addOnSuccessListener { result ->
                                val name = result.user?.displayName ?: result.user?.email ?: "Người dùng GitHub"
                                onLoginSuccess(name)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Lỗi GitHub: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Đăng nhập bằng GitHub", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}