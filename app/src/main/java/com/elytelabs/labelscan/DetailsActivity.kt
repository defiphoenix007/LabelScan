package com.elytelabs.labelscan

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class DetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        val imgProduct: ImageView = findViewById(R.id.imgProduct)
        val txtProductName: TextView = findViewById(R.id.txtProductName)
        val txtIngredients: TextView = findViewById(R.id.txtIngredients)
        val txtSideEffects: TextView = findViewById(R.id.txtSideEffects)
        val trafficLightsLayout: LinearLayout = findViewById(R.id.nutritionTrafficLights)

        // Get product info from intent
        val productName = intent.getStringExtra("name") ?: "Unknown Product"
        val imageUrl = intent.getStringExtra("image")
        val ingredients = intent.getStringExtra("ingredients") ?: "No data"
        val sideEffects = intent.getStringExtra("side_effects") ?: "No known side effects"

        // Nutrition levels (low/medium/high)
        val fatLevel = intent.getStringExtra("fat_level") ?: "unknown"
        val sugarLevel = intent.getStringExtra("sugar_level") ?: "unknown"
        val saltLevel = intent.getStringExtra("salt_level") ?: "unknown"
        val satFatLevel = intent.getStringExtra("sat_fat_level") ?: "unknown" // 👈 fixed key

        // Set UI
        txtProductName.text = productName
        txtIngredients.text = ingredients

        // ✅ Color-coded side effects
        val spannable = SpannableString(sideEffects)
        sideEffects.lines().forEach { line ->
            val start = spannable.indexOf(line)
            val end = start + line.length
            if (line.contains("Contains", true) || line.contains("May cause", true)) {
                spannable.setSpan(
                    ForegroundColorSpan(Color.RED),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else {
                spannable.setSpan(
                    ForegroundColorSpan(Color.GREEN),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        txtSideEffects.text = spannable

        // Load product image
        imageUrl?.let {
            Glide.with(this).load(it).into(imgProduct)
        }

        // Add traffic light indicators
        addTrafficLight(trafficLightsLayout, "Fat", fatLevel)
        addTrafficLight(trafficLightsLayout, "Sugars", sugarLevel)
        addTrafficLight(trafficLightsLayout, "Salt", saltLevel)
        addTrafficLight(trafficLightsLayout, "Sat. Fat", satFatLevel)
    }

    /** Add a traffic light circle + label */
    private fun addTrafficLight(parent: LinearLayout, label: String, level: String) {
        // Container for circle + label
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = 8
                marginEnd = 8
            }
        }

        // Circle (colored background)
        val circle = TextView(this).apply {
            text = "" // empty circle
            background = getDrawable(R.drawable.circle_background)
            background?.setTint(getTrafficColor(level))
            layoutParams = LinearLayout.LayoutParams(80, 80) // circle size
        }

        // Label (below circle)
        val labelView = TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 6
            }
        }

        // Click listener → show popup info
        container.setOnClickListener {
            showNutrientInfo(label, level)
        }

        // Add circle + label to container
        container.addView(circle)
        container.addView(labelView)

        // Add container to parent row
        parent.addView(container)
    }

    /** Decide circle color based on nutrient level */
    private fun getTrafficColor(level: String): Int {
        return when (level.lowercase()) {
            "low" -> Color.parseColor("#4CAF50")   // Green
            "moderate", "medium" -> Color.parseColor("#FFC107") // Amber
            "high" -> Color.parseColor("#F44336")  // Red
            else -> Color.GRAY
        }
    }

    /** Show popup with info + recommendations */
    private fun showNutrientInfo(label: String, level: String) {
        val info = when (label.lowercase()) {
            "fat" -> "Fat provides energy, but high intake can increase cholesterol levels."
            "sugars" -> "High sugar intake is linked to obesity, diabetes, and tooth decay."
            "salt" -> "Excess salt raises blood pressure and increases risk of heart disease."
            "sat. fat" -> "Saturated fat increases LDL cholesterol, linked to heart problems."
            else -> "General nutrition information."
        }

        val recommendation = when (label.lowercase()) {
            "fat" -> "Recommended: Less than 70g per day"
            "sugars" -> "Recommended: Less than 30g free sugars per day"
            "salt" -> "Recommended: Less than 6g per day"
            "sat. fat" -> "Recommended: Less than 20g per day"
            else -> "Recommended: Balanced intake"
        }

        AlertDialog.Builder(this)
            .setTitle("$label - ${level.replaceFirstChar { it.uppercase() }}")
            .setMessage("$info\n\n$recommendation")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
