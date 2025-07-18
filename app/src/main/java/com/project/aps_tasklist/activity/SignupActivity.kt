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
import com.project.aps_tasklist.R

class SignupActivity : AppCompatActivity() {

    private lateinit var signupUsernameEditText: EditText
    private lateinit var signupEmailEditText: EditText
    private lateinit var signupPasswordEditText: EditText
    private lateinit var signupConfirmPasswordEditText: EditText
    private lateinit var signupButton: Button

    private var isPasswordVisible = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)

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
        //toggle password visibility
        signupPasswordEditText.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val edit = v as EditText
                val drawableEnd = edit.compoundDrawablesRelative[2]
                if (drawableEnd != null) {
                    val touchAreaStart = edit.width - edit.paddingEnd - drawableEnd.intrinsicWidth
                    if (event.x >= touchAreaStart) {
                        isPasswordVisible = !isPasswordVisible
                        edit.transformationMethod = if (isPasswordVisible) {
                            HideReturnsTransformationMethod.getInstance()
                        } else {
                            PasswordTransformationMethod.getInstance()
                        }
                        edit.setSelection(edit.text.length)
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
    }

    private fun validateInputs(): Boolean {
        val username = signupUsernameEditText.text.toString().trim()
        val email    = signupEmailEditText.text.toString().trim()
        val pass     = signupPasswordEditText.text.toString()
        val confirm  = signupConfirmPasswordEditText.text.toString()

        return when {
            username.isEmpty() -> {
                Toast.makeText(this, "Username tidak boleh kosong", Toast.LENGTH_SHORT).show()
                false
            }
            email.isEmpty() -> {
                Toast.makeText(this, "Email tidak boleh kosong", Toast.LENGTH_SHORT).show()
                false
            }
            pass.isEmpty() -> {
                Toast.makeText(this, "Password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                false
            }
            confirm.isEmpty() -> {
                Toast.makeText(this, "Konfirmasi password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                false
            }
            pass != confirm -> {
                Toast.makeText(this, "Password dan konfirmasi tidak cocok", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }
}