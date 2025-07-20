package com.project.aps_tasklist.activity

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.project.aps_tasklist.R

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var forgotPasswordTextView: TextView
    private lateinit var loginButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var rtDb: DatabaseReference

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Inisialisasi FirebaseAuth & Realtime DB
        auth = FirebaseAuth.getInstance()
        rtDb = FirebaseDatabase.getInstance().reference

        // Bind views
        usernameEditText     = findViewById(R.id.usernameEditText)
        passwordEditText     = findViewById(R.id.passwordEditText)
        forgotPasswordTextView = findViewById(R.id.forgotPasswordText)
        loginButton          = findViewById(R.id.loginButton)

        // Edge‑to‑edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginlayout)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        // Signup prompt
        findViewById<TextView>(R.id.signUpPrompt).setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        // Login button
        loginButton.setOnClickListener {
            val input = usernameEditText.text.toString().trim()
            val pass  = passwordEditText.text.toString().trim()

            if (input.isEmpty() || pass.isEmpty()) {
                if (input.isEmpty()) usernameEditText.error = "Email/Username tidak boleh kosong"
                if (pass.isEmpty())  passwordEditText.error  = "Password tidak boleh kosong"
                return@setOnClickListener
            }

            if (input.contains("@")) {
                // langsung pakai email
                signInWithEmail(input, pass)
            } else {
                // lookup username → uid
                rtDb.child("usernames")
                    .child(input)
                    .get()
                    .addOnSuccessListener { snap ->
                        val uid = snap.getValue(String::class.java)
                        if (uid.isNullOrEmpty()) {
                            usernameEditText.error = "Username tidak ditemukan"
                        } else {
                            // ambil email dari /users/{uid}
                            rtDb.child("users")
                                .child(uid)
                                .child("email")
                                .get()
                                .addOnSuccessListener { emailSnap ->
                                    val email = emailSnap.getValue(String::class.java)
                                    if (email.isNullOrEmpty()) {
                                        Toast.makeText(this, "Data email tidak valid", Toast.LENGTH_LONG).show()
                                    } else {
                                        signInWithEmail(email, pass)
                                    }
                                }
                                .addOnFailureListener { ex ->
                                    Toast.makeText(this, ex.message, Toast.LENGTH_LONG).show()
                                }
                        }
                    }
                    .addOnFailureListener { ex ->
                        Toast.makeText(this, ex.message, Toast.LENGTH_LONG).show()
                    }
            }
        }

        // Toggle password visibility
        passwordEditText.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val edit = v as EditText
                val drawableEnd = edit.compoundDrawablesRelative[2] ?: return@setOnTouchListener false
                val startX = edit.width - edit.paddingEnd - drawableEnd.intrinsicWidth
                if (event.x >= startX) {
                    v.performClick()  // agar lint aksesibilitas tidak warning
                    isPasswordVisible = !isPasswordVisible
                    edit.transformationMethod = if (isPasswordVisible)
                        HideReturnsTransformationMethod.getInstance()
                    else
                        PasswordTransformationMethod.getInstance()
                    edit.setSelection(edit.text.length)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }


    private fun signInWithEmail(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { res ->
                val uid = res.user!!.uid

                // Pastikan username di-sync ke kedua DB
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        val userName = doc.getString("username") ?: ""
                        if (userName.isNotBlank()) {
                            // tulis ulang juga ke RTDB
                            FirebaseDatabase.getInstance().getReference("users")
                                .child(uid).child("username").setValue(userName)
                        }
                        // lanjut buka MainActivity
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        // meski gagal, kita tetap buka MainActivity
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
            }
            .addOnFailureListener { ex ->
                passwordEditText.error = ex.message
            }
    }

}
