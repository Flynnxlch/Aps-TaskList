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

class SignupActivity : AppCompatActivity() {

    private lateinit var signupUsernameEditText: EditText
    private lateinit var signupEmailEditText: EditText
    private lateinit var signupPasswordEditText: EditText
    private lateinit var signupConfirmPasswordEditText: EditText
    private lateinit var signupButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var rtDb: DatabaseReference

    private var isPasswordVisible = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        rtDb = FirebaseDatabase.getInstance().reference

        signupUsernameEditText = findViewById(R.id.signupUsernameEditText)
        signupEmailEditText = findViewById(R.id.signupEmailEditText)
        signupPasswordEditText = findViewById(R.id.signupPasswordEditText)
        signupConfirmPasswordEditText = findViewById(R.id.signupConfirmPasswordEditText)
        signupButton = findViewById(R.id.signupButton)

        //Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Signuplayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Sign up prompt
        val loginPrompt = findViewById<TextView>(R.id.loginPrompt)
        loginPrompt.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        signupButton.setOnClickListener {
            val username = signupUsernameEditText.text.toString().trim()
            val email = signupEmailEditText.text.toString().trim()
            val pass = signupPasswordEditText.text.toString()
            val cpass = signupConfirmPasswordEditText.text.toString()


            if (username.isEmpty() || email.isEmpty() || pass.isEmpty() || cpass.isEmpty()) {
                if (username.isEmpty()) signupUsernameEditText.error = "Username wajib diisi"
                if (email   .isEmpty()) signupEmailEditText.error = "Email wajib diisi"
                if (pass    .isEmpty()) signupPasswordEditText.error = "Password wajib diisi"
                if (cpass .isEmpty()) signupConfirmPasswordEditText.error = "Konfirmasi wajib diisi"
                return@setOnClickListener
        }
            val pwdrgx = Regex("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$")
            if (!pwdrgx.matches(pass)) {
                signupPasswordEditText.error = "Minimal 8 karakter, harus huruf & angka"
                return@setOnClickListener
            }
            if (pass != cpass){
                signupConfirmPasswordEditText.error = "Konfirmasi password tidak cocok"
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener { res ->
                    val uid = res.user!!.uid
                    val userMap = mapOf("username" to username, "email" to email)

                    rtDb.child("users").child(uid).setValue(userMap)
                    rtDb.child("usernames").child(username).setValue(uid)

                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .set(userMap)

                    // langsung masuk MainActivity
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener { ex ->
                    Toast.makeText(this, ex.message, Toast.LENGTH_LONG).show()
                }

        }


        listOf(signupPasswordEditText, signupConfirmPasswordEditText).forEach { edit ->
            edit.setOnTouchListener { v, ev ->
                if (ev.action == MotionEvent.ACTION_UP) {
                    val dEnd = edit.compoundDrawablesRelative[2] ?: return@setOnTouchListener false
                    val startX = edit.width - edit.paddingEnd - dEnd.intrinsicWidth
                    if (ev.x >= startX) {
                        v.performClick()  // accesibility
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
    }
}