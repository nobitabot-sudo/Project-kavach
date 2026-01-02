private fun loadDashboardData() {
        val recentThreats = dbHelper.getRecentThreats(5)
        val stats = dbHelper.getStatistics()
        val totalCount = stats["Total"] ?: 0

        // Score logic: Jitne scam, utna kam score
        val calculatedScore = (100 - (totalCount * 10)).coerceIn(0, 100)
        animateSafetyScoreTo(calculatedScore)
        
        if (recentThreats.isEmpty()) {
            scamHistoryRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            scamHistoryRecyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            threatAdapter.updateThreats(recentThreats)
        }
}
