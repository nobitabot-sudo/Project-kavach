
package com.projectkavach.guardian

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var safetyScoreProgress: ProgressBar
    private lateinit var safetyScoreText: TextView
    private lateinit var scoreStatusText: TextView
    private lateinit var scoreGlow: View
    private lateinit var financialShieldToggle: SwitchMaterial
    private lateinit var personalShieldToggle: SwitchMaterial
    private lateinit var deviceShieldToggle: SwitchMaterial
    private lateinit var financialScoreText: TextView
    private lateinit var personalScoreText: TextView
    private lateinit var deviceScoreText: TextView
    private lateinit var scamHistoryRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var viewAllButton: TextView
    private lateinit var settingsButton: ImageView
    
    private lateinit var dbHelper: ScamDatabaseHelper
    private lateinit var threatAdapter: ThreatHistoryAdapter
    private val maxRecentThreats = 5
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        dbHelper = ScamDatabaseHelper(this)
        
        initializeViews()
        setupRecyclerView()
        setupListeners()
        loadDashboardData()
    }
    
    private fun initializeViews() {
        safetyScoreProgress = findViewById(R.id.safetyScoreProgress)
        safetyScoreText = findViewById(R.id.safetyScoreText)
        scoreStatusText = findViewById(R.id.scoreStatusText)
        scoreGlow = findViewById(R.id.scoreGlow)
        financialShieldToggle = findViewById(R.id.financialShieldToggle)
        personalShieldToggle = findViewById(R.id.personalShieldToggle)
        deviceShieldToggle = findViewById(R.id.deviceShieldToggle)
        financialScoreText = findViewById(R.id.financialScoreText)
        personalScoreText = findViewById(R.id.personalScoreText)
        deviceScoreText = findViewById(R.id.deviceScoreText)
        scamHistoryRecyclerView = findViewById(R.id.scamHistoryRecyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        viewAllButton = findViewById(R.id.viewAllButton)
        settingsButton = findViewById(R.id.settingsButton)
    }
    
    private fun setupRecyclerView() {
        threatAdapter = ThreatHistoryAdapter(emptyList()) { threat ->
            // Detail view functionality will be added in Phase 2
        }
        
        scamHistoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = threatAdapter
        }
    }
    
    private fun setupListeners() {
        financialShieldToggle.setOnCheckedChangeListener { _, isChecked -> updateShieldStatus("financial", isChecked) }
        personalShieldToggle.setOnCheckedChangeListener { _, isChecked -> updateShieldStatus("personal", isChecked) }
        deviceShieldToggle.setOnCheckedChangeListener { _, isChecked -> updateShieldStatus("device", isChecked) }
        
        // Temporarily disabled until screens are created
        viewAllButton.setOnClickListener { 
            Toast.makeText(this, "History feature coming soon", Toast.LENGTH_SHORT).show()
        }
        settingsButton.setOnClickListener { 
            Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadDashboardData() {
        val stats = dbHelper.getStatistics()
        animateSafetyScoreTo(85) // Placeholder score for now
        
        financialScoreText.text = "Score: 100/100"
        personalScoreText.text = "Score: 100/100"
        deviceScoreText.text = "Score: 100/100"
        
        scamHistoryRecyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE
    }
    
    private fun animateSafetyScoreTo(targetScore: Int) {
        ValueAnimator.ofInt(0, targetScore).apply {
            duration = 2000
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                safetyScoreProgress.progress = value
                safetyScoreText.text = value.toString()
                scoreStatusText.text = if (value >= 80) "Protected" else "At Risk"
            }
            start()
        }
        animateGlow()
    }
    
    private fun animateGlow() {
        ValueAnimator.ofFloat(0.3f, 0.8f).apply {
            duration = 2000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation -> scoreGlow.alpha = animation.animatedValue as Float }
            start()
        }
    }
    
    private fun updateShieldStatus(category: String, enabled: Boolean) {
        val status = if (enabled) "enabled" else "disabled"
        Toast.makeText(this, "$category Shield $status", Toast.LENGTH_SHORT).show()
    }
}

data class ThreatData(
    val id: Long,
    val title: String,
    val category: String,
    val severity: String,
    val timestamp: Long,
    val confidence: Int
)

class ThreatHistoryAdapter(
    private var threats: List<ThreatData>,
    private val onThreatClick: (ThreatData) -> Unit
) : RecyclerView.Adapter<ThreatHistoryAdapter.ThreatViewHolder>() {

    class ThreatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val threatCategory: TextView = view.findViewById(R.id.threatCategory)
        val threatTime: TextView = view.findViewById(R.id.threatTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_threat_history, parent, false)
        return ThreatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThreatViewHolder, position: Int) {
        val threat = threats[position]
        holder.threatCategory.text = threat.category
        holder.threatTime.text = "Just now"
    }

    override fun getItemCount() = threats.size
}
