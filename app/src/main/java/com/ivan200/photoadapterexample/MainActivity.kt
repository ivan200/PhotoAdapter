package com.ivan200.photoadapterexample

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                this@MainActivity.handleOnBackPressed()
            }
        })

        enableEdgeToEdge()
    }

    override fun setTitle(title: CharSequence) {
        super.setTitle(title)
        supportActionBar?.title = title
    }

    fun handleOnBackPressed() {
        findNavController(R.id.fragment_main).let {
            if (!it.popBackStack()) finish()
        }
    }
}
