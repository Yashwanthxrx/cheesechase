package com.example.cheesechase

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Add the game view
        val gameView = GameView(this)
        setContentView(gameView)
    }

    fun showGameOverDialog(score: Int) {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.gameover, null)

        // Set up the dialog view
        val playAgainButton: Button = view.findViewById(R.id.btnPlayAgain)
        val homeButton: Button = view.findViewById(R.id.btnHome)

        playAgainButton.setOnClickListener {
            // Restart the game
            recreate()
        }

        homeButton.setOnClickListener {
            // Navigate to the home screen
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Set the score in the dialog
        view.findViewById<TextView>(R.id.tvScore).text = "Score: $score"

        builder.setView(view)
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.show()
    }
}