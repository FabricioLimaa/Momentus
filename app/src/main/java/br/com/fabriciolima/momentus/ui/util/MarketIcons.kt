package br.com.fabriciolima.momentus.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun getIconForMarketItem(id: String?): ImageVector? {
    if (id == null) return null
    return when (id) {
        "stk_rocket" -> Icons.Default.RocketLaunch
        "stk_coffee" -> Icons.Default.Coffee
        "stk_zen" -> Icons.Default.SelfImprovement
        "stk_bolt" -> Icons.Default.Bolt
        "med_time" -> Icons.Default.Timer
        "med_streak" -> Icons.Default.LocalFireDepartment
        else -> Icons.Default.Inventory2
    }
}
