
package com.poema.runnerapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser



//if (auth.currentUser != null) // är lika med inte inloggad

class MainActivity : AppCompatActivity() {

    lateinit var textEmail : EditText
    lateinit var textPassword : EditText
    lateinit var auth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_page)
        textEmail = findViewById(R.id.EmailEditText)
        textPassword = findViewById(R.id.passwordEditText)
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null){goIn()}
        val createButton = findViewById<Button>(R.id.createButton)

        createButton.setOnClickListener{
            createUser()
        }
        val loginButton = findViewById<Button>(R.id.loginButton)
        loginButton.setOnClickListener{
            loginUser()
        }
    }
    fun createUser(){
    auth.createUserWithEmailAndPassword(textEmail.text.toString(), textPassword.text.toString())
        .addOnCompleteListener(this) {task ->
            if (task.isSuccessful){
                println("!!! User Created")
            } else {
                println("!!! User not created")
            }
        }
    }
    fun loginUser(){
        auth.signInWithEmailAndPassword(textEmail.text.toString(), textPassword.text.toString())
            .addOnCompleteListener(this) {task ->
                if (task.isSuccessful){
                    println("!!! User logged in")
                    goIn()
                } else {
                    println("!!! User not logged in")
                }
            }
    }
    fun goIn(){
        val intent = Intent(this,StartPageActivity::class.java)
        startActivity(intent)
    }
}