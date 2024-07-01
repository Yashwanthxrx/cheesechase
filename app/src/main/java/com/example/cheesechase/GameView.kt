package com.example.cheesechase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import kotlin.concurrent.fixedRateTimer
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class GameView(context: Context) : View(context) {
    private val paint = Paint()
    private val laneWidth = context.resources.displayMetrics.widthPixels / 3
    private val characterSize = 100
    private val obstacleSize = 150

    private var jerryX = 1
    private var jerryY = context.resources.displayMetrics.heightPixels - 200

    private var tomX = 1
    private var tomY = jerryY - 300 // Tom follows Jerry at a distance

    private var score = 0
    private var highScore = 0
    private var gameEnded = false
    private var tomHits = 0 // Start with 0 hits

    private var obstacles = mutableListOf<Obstacle>()

    private var jerryBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.jerry)
    private var tomBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.tom)
    private var obstacleBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.obstacle)
    private var backgroundBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.back)
    private val vibrator: Vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.example.com") // Replace with your API base URL
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(ChaseDeuxApi::class.java)
    private val handler = Handler(Looper.getMainLooper())

    init {
        jerryBitmap = Bitmap.createScaledBitmap(jerryBitmap, characterSize, characterSize, false)
        tomBitmap = Bitmap.createScaledBitmap(tomBitmap, characterSize, characterSize, false)
        obstacleBitmap = Bitmap.createScaledBitmap(obstacleBitmap, obstacleSize, obstacleSize, false)
        fetchObstacleLimit()
        fetchImages()
        generateObstacles() // Generate initial obstacles
        fixedRateTimer(period = 50) {
            postInvalidate()
        }
        fixedRateTimer(period = 3000) { // Decrease the frequency of obstacle generation
            handler.post { generateObstacles() }
        }
    }

    private fun fetchObstacleLimit() {
        api.getObstacleLimit().enqueue(object : Callback<ObstacleLimitResponse> {
            override fun onResponse(call: Call<ObstacleLimitResponse>, response: Response<ObstacleLimitResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        tomHits = it.limit
                    }
                }
            }

            override fun onFailure(call: Call<ObstacleLimitResponse>, t: Throwable) {
                // Handle failure
            }
        })
    }

    private fun fetchImages() {
        api.getImages().enqueue(object : Callback<ImagesResponse> {
            override fun onResponse(call: Call<ImagesResponse>, response: Response<ImagesResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        loadImage(it.jerry, R.drawable.jerry) { bitmap ->
                            jerryBitmap = Bitmap.createScaledBitmap(bitmap, characterSize, characterSize, false)
                        }
                        loadImage(it.tom, R.drawable.tom) { bitmap ->
                            tomBitmap = Bitmap.createScaledBitmap(bitmap, characterSize, characterSize, false)
                        }
                        loadImage(it.obstacle, R.drawable.obstacle) { bitmap ->
                            obstacleBitmap = Bitmap.createScaledBitmap(bitmap, obstacleSize, obstacleSize, false)
                        }
                        loadImage(it.background, R.drawable.back) { bitmap ->
                            backgroundBitmap = bitmap // Assuming background is full size
                        }
                    }
                }
            }

            override fun onFailure(call: Call<ImagesResponse>, t: Throwable) {
                // Handle failure
            }
        })
    }

    private fun loadImage(url: String, placeholder: Int, callback: (Bitmap) -> Unit) {
        Thread {
            try {
                val inputStream = URL(url).openStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                handler.post { callback(bitmap) }
            } catch (e: Exception) {
                val bitmap = BitmapFactory.decodeResource(resources, placeholder)
                handler.post { callback(bitmap) }
            }
        }.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!gameEnded) {
            drawBackground(canvas)
            drawLanes(canvas)
            drawObstacles(canvas)
            drawJerry(canvas)
            drawTom(canvas)
            updateScore(canvas)
            updateGameState()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (event.x < width / 2) {
                moveJerryLeft()
            } else {
                moveJerryRight()
            }
        }
        return true
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawBitmap(backgroundBitmap, 0f, 0f, paint)
    }

    private fun drawLanes(canvas: Canvas) {
        paint.color = Color.LTGRAY
        for (i in 1 until 3) {
            canvas.drawRect((i * laneWidth).toFloat(), 0f, (i * laneWidth + 20).toFloat(), height.toFloat(), paint)
        }
    }

    private fun drawObstacles(canvas: Canvas) {
        obstacles.forEach { obstacle ->
            canvas.drawBitmap(obstacleBitmap, obstacle.x.toFloat(), obstacle.y.toFloat(), paint)
        }
    }

    private fun drawJerry(canvas: Canvas) {
        val jerryXPos = jerryX * laneWidth + laneWidth / 2 - characterSize / 2
        canvas.drawBitmap(jerryBitmap, jerryXPos.toFloat(), jerryY.toFloat() - characterSize / 2, paint)
    }

    private fun drawTom(canvas: Canvas) {
        val tomXPos = tomX * laneWidth + laneWidth / 2 - characterSize / 2
        canvas.drawBitmap(tomBitmap, tomXPos.toFloat(), tomY.toFloat() - characterSize / 2, paint)
    }

    private fun moveJerryLeft() {
        if (jerryX > 0) jerryX--
    }

    private fun moveJerryRight() {
        if (jerryX < 2) jerryX++
    }

    private fun moveTom() {
        // Tom stays on the screen and moves downwards
        if (tomY < height) {
            tomY += 20
        }

        // Check if Tom needs to avoid an obstacle
        obstacles.forEach { obstacle ->
            val tomXPos = tomX * laneWidth + laneWidth / 2 - characterSize / 2
            if (obstacle.x < tomXPos + characterSize && obstacle.x + obstacleSize > tomXPos - characterSize
                && obstacle.y < tomY + characterSize && obstacle.y + obstacleSize > tomY - characterSize) {
                if (tomX > 0) {
                    tomX--
                } else {
                    tomX++
                }
            }
        }
    }
    private fun triggerVibration() {
        if (vibrator.hasVibrator()) {
            val vibrationEffect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(vibrationEffect)
        }
    }
    private fun generateObstacles() {
        val lane = (0..2).random()
        val y = -100 - (0..700).random()
        obstacles.add(Obstacle(lane * laneWidth + laneWidth / 2 - obstacleSize / 2, y))
    }

    private fun updateScore(canvas: Canvas) {
        paint.color = Color.RED
        paint.textSize = 50f
        canvas.drawText("Score: $score", 50f, 100f, paint)
    }

    private fun updateGameState() {
        obstacles.forEach { it.y += 20 }

        moveTom()
        checkCollisions()

        // Do not remove obstacles, just update their position
        obstacles.filter { it.y > height }.forEach {
            it.y = -obstacleSize
            it.x = (0..2).random() * laneWidth + laneWidth / 2 - obstacleSize / 2
        }

        score++
        if (score > highScore) highScore = score
    }

    private fun checkCollisions() {
        obstacles.forEach { obstacle ->
            val jerryXPos = jerryX * laneWidth + laneWidth / 2 - characterSize / 2
            if (obstacle.x < jerryXPos + characterSize && obstacle.x + obstacleSize > jerryXPos - characterSize
                && obstacle.y < jerryY + characterSize && obstacle.y + obstacleSize > jerryY - characterSize) {
                // End game if Jerry collides with an obstacle
                gameEnded = true
                triggerVibration()
                (context as GameActivity).showGameOverDialog(score)
            }

            val tomXPos = tomX * laneWidth + laneWidth / 2 - characterSize / 2
            if (obstacle.x < tomXPos + characterSize && obstacle.x + obstacleSize > tomXPos - characterSize
                && obstacle.y < tomY + characterSize && obstacle.y + obstacleSize > tomY - characterSize) {
                tomHits++
                if (tomHits >= 2) {
                    gameEnded = true
                    (context as GameActivity).showGameOverDialog(score)
                }
            }
        }
    }

    data class Obstacle(var x: Int, var y: Int)
}