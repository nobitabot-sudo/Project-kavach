package com.projectkavach.guardian

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * Main Dashboard Activity - Control Center
 * 
 * Features:
 * - Animated safety score meter (0-100) with pulsating glow
 * - Protection shield toggles (Financial, Personal, Device)
 * - Scrollable scam history from SQLite database
 * - Real-time statistics
 */
class MainActivity : AppCompatActivity() {

    // UI Components
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
    
    // Data
    private lateinit var dbHelper: ScamDatabaseHelper
    private lateinit var threatAdapter: ThreatHistoryAdapter
    private val maxRecentThreats = 5
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize database
        dbHelper = ScamDatabaseHelper(this)
        
        // Initialize UI
        initializeViews()
        setupRecyclerView()
        setupListeners()
        
        // Load and display data
        loadDashboardData()
        animateSafetyScore()
    }
    
    private fun initializeViews() {
        // Safety Score Meter
        safetyScoreProgress = findViewById(R.id.safetyScoreProgress)
        safetyScoreText = findViewById(R.id.safetyScoreText)
        scoreStatusText = findViewById(R.id.scoreStatusText)
        scoreGlow = findViewById(R.id.scoreGlow)
        
        // Shield Toggles
        financialShieldToggle = findViewById(R.id.financialShieldToggle)
        personalShieldToggle = findViewById(R.id.personalShieldToggle)
        deviceShieldToggle = findViewById(R.id.deviceShieldToggle)
        
        // Score Text Views
        financialScoreText = findViewById(R.id.financialScoreText)
        personalScoreText = findViewById(R.id.personalScoreText)
        deviceScoreText = findViewById(R.id.deviceScoreText)
        
        // Scam History
        scamHistoryRecyclerView = findViewById(R.id.scamHistoryRecyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        viewAllButton = findViewById(R.id.viewAllButton)
        settingsButton = findViewById(R.id.settingsButton)
    }
    
    private fun setupRecyclerView() {
        threatAdapter = ThreatHistoryAdapter(emptyList()) { threat ->
            // Handle threat click - open detail view
            openThreatDetail(threat)
        }
        
        scamHistoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = threatAdapter
            isNestedScrollingEnabled = false
        }
    }
    
    private fun setupListeners() {
        // Shield toggle listeners
        financialShieldToggle.setOnCheckedChangeListener { _, isChecked ->
            updateShieldStatus("financial", isChecked)
        }
        
        personalShieldToggle.setOnCheckedChangeListener { _, isChecked ->
            updateShieldStatus("personal", isChecked)
        }
        
        deviceShieldToggle.setOnCheckedChangeListener { _, isChecked ->
            updateShieldStatus("device", isChecked)
        }
        
        // View All button
        viewAllButton.setOnClickListener {
            openFullHistory()
        }
        
        // Settings button
        settingsButton.setOnClickListener {
            openSettings()
        }
    }
    
    private fun loadDashboardData() {
        // Calculate overall safety score
        val stats = dbHelper.getStatistics()
        val overallScore = calculateOverallScore(stats)
        
        // Update individual category scores
        val financialScore = calculateCategoryScore("financial")
        val personalScore = calculateCategoryScore("personal")
        val deviceScore = calculateCategoryScore("device")
        
        financialScoreText.text = "Score: $financialScore/100"
        personalScoreText.text = "Score: $personalScore/100"
        deviceScoreText.text = "Score: $deviceScore/100"
        
        // Load recent threats
        val recentThreats = dbHelper.getRecentThreats(maxRecentThreats)
        
        if (recentThreats.isEmpty()) {
            scamHistoryRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            scamHistoryRecyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            threatAdapter.updateThreats(recentThreats)
        }
        
        // Animate score to target value
        animateSafetyScoreTo(overallScore)
    }
    
    private fun calculateOverallScore(stats: Map<String, Any>): Int {
        // Base score starts at 100
        var score = 100
        
        // Deduct points for each threat (more severe = more deduction)
        val criticalCount = stats["criticalCount"] as? Int ?: 0
        val highCount = stats["highCount"] as? Int ?: 0
        val mediumCount = stats["mediumCount"] as? Int ?: 0
        
        score -= (criticalCount * 15)  // -15 per critical threat
        score -= (highCount * 8)       // -8 per high threat
        score -= (mediumCount * 3)     // -3 per medium threat
        
        // Ensure score is between 0-100
        return score.coerceIn(0, 100)
    }
    
    private fun calculateCategoryScore(category: String): Int {
        // Get threats for specific category
        val categoryThreats = dbHelper.getThreatsByCategory(category)
        
        var score = 100
        categoryThreats.forEach { threat ->
            when (threat.severity) {
                "critical" -> score -= 20
                "high" -> score -= 10
                "medium" -> score -= 5
            }
        }
        
        return score.coerceIn(0, 100)
    }
    
    private fun animateSafetyScoreTo(targetScore: Int) {
        // Animate progress bar
        val progressAnimator = ValueAnimator.ofInt(0, targetScore).apply {
            duration = 2000
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                safetyScoreProgress.progress = value
                safetyScoreText.text = value.toString()
                
                // Update status text based on score
                scoreStatusText.text = when {
                    value >= 90 -> "Excellent Protection"
                    value >= 75 -> "Good Protection"
                    value >= 60 -> "Fair Protection"
                    else -> "At Risk"
                }
            }
        }
        progressAnimator.start()
        
        // Pulsating glow effect
        animateGlow()
    }
    
    private fun animateSafetyScore() {
        // Initial animation when screen loads
        loadDashboardData()
    }
    
    private fun animateGlow() {
        // Pulsating alpha animation for glow effect
        val glowAnimator = ValueAnimator.ofFloat(0.3f, 0.8f).apply {
            duration = 2000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                scoreGlow.alpha = animation.animatedValue as Float
            }
        }
        glowAnimator.start()
    }
    
    private fun updateShieldStatus(category: String, enabled: Boolean) {
        // Store shield preference
        val prefs = getSharedPreferences("kavach_settings", MODE_PRIVATE)
        prefs.edit().putBoolean("shield_${category}", enabled).apply()
        
        // Show toast feedback
        val status = if (enabled) "enabled" else "disabled"
        Toast.makeText(
            this,
            "${category.capitalize()} Shield $status",
            Toast.LENGTH_SHORT
        ).show()
        
        // Update accessibility service configuration if needed
        if (!enabled) {
            // Optionally notify service to ignore this category
        }
    }
    
    private fun openThreatDetail(threat: ThreatData) {
        val intent = Intent(this, ThreatDetailActivity::class.java).apply {
            putExtra("threat_id", threat.id)
        }
        startActivity(intent)
    }
    
    private fun openFullHistory() {
        val intent = Intent(this, ThreatHistoryActivity::class.java)
        startActivity(intent)
    }
    
    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this screen
        loadDashboardData()
    }
}

/**
 * Data class for threat display
 */
data class ThreatData(
    val id: Long,
    val title: String,
    val category: String,
    val severity: String,
    val timestamp: Long,
    val confidence: Int
)

/**
 * RecyclerView Adapter for threat history
 */
class ThreatHistoryAdapter(
    private var threats: List<ThreatData>,
    private val onThreatClick: (ThreatData) -> Unit
) : RecyclerView.Adapter<ThreatHistoryAdapter.ThreatViewHolder>() {

    class ThreatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val threatTitle: TextView = view.findViewById(R.id.threatTitle)
        val threatCategory: TextView = view.findViewById(R.id.threatCategory)
        val threatTime: TextView = view.findViewById(R.id.threatTime)
        val severityBadge: View = view.findViewById(R.id.severityBadge)
        val confidenceText: TextView = view.findViewById(R.id.confidenceText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_threat_history, parent, false)
        return ThreatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThreatViewHolder, position: Int) {
        val threat = threats[position]
        
        holder.threatTitle.text = threat.title
        holder.threatCategory.text = threat.category.uppercase()
        holder.confidenceText.text = "${threat.confidence}%"
        
        // Format timestamp
        val timeAgo = formatTimeAgo(threat.timestamp)
        holder.threatTime.text = timeAgo
        
        // Set severity badge color
        val badgeColor = when (threat.severity) {
            "critical" -> Color.parseColor("#FF0000")
            "high" -> Color.parseColor("#FFB800")
            else -> Color.parseColor("#00F2FF")
        }
        holder.severityBadge.setBackgroundColor(badgeColor)
        
        // Click listener
        holder.itemView.setOnClickListener {
            onThreatClick(threat)
        }
    }

    override fun getItemCount() = threats.size

    fun updateThreats(newThreats: List<ThreatData>) {
        threats = newThreats
        notifyDataSetChanged()
    }
    
    private fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            else -> "${diff / 86400000}d ago"
        }
    }
}
