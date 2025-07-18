package com.project.aps_tasklist.activity

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.project.aps_tasklist.R

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var forgotPasswordTextView: TextView
    private lateinit var loginButton: Button

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        forgotPasswordTextView = findViewById(R.id.forgotPasswordText)
        loginButton = findViewById(R.id.loginButton)


        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginlayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //signup prompt
        val signUpPrompt =  findViewById<TextView>(R.id.signUpPrompt)
        signUpPrompt.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        //login button
        loginButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        //passwordedittext
        passwordEditText.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val edit = v as EditText
                val drawableEnd = edit.compoundDrawablesRelative[2]
                if (drawableEnd != null) {
                    val touchAreaStart = edit.width - edit.paddingEnd - drawableEnd.intrinsicWidth
                    if (event.x >= touchAreaStart) {
                        isPasswordVisible = !isPasswordVisible
                        edit.transformationMethod = if (isPasswordVisible)
                            HideReturnsTransformationMethod.getInstance()
                        else
                            PasswordTransformationMethod.getInstance()
                        edit.setSelection(edit.text.length)
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }


    }
}