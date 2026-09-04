package com.example.sprocket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sprocket.theme.SprocketBg
import com.example.sprocket.theme.SprocketDivider
import com.example.sprocket.theme.SprocketInk
import com.example.sprocket.theme.SprocketSurface
import com.example.sprocket.theme.sprocketLeftBorder
import com.example.sprocket.theme.sprocketTopBorder

enum class SprocketTab(val title: String) {
    GARAGE("GARAGE"),
    COSTS("COSTS"),
    SETUP("SETUP")
}

@Composable
fun SprocketBottomNav(
    currentTab: SprocketTab,
    garageSub: String,
    costsSub: String,
    setupSub: String,
    onTabSelected: (SprocketTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        Triple(SprocketTab.GARAGE, SprocketTab.GARAGE.title, garageSub),
        Triple(SprocketTab.COSTS, SprocketTab.COSTS.title, costsSub),
        Triple(SprocketTab.SETUP, SprocketTab.SETUP.title, setupSub)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SprocketSurface)
            .sprocketTopBorder(SprocketDivider, 2.dp)
            .navigationBarsPadding()
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, (tab, title, sub) ->
                val isSelected = currentTab == tab
                val bgColor = if (isSelected) SprocketInk else Color.Transparent
                val fgColor = if (isSelected) SprocketBg else SprocketInk

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(bgColor)
                        .then(if (index > 0) Modifier.sprocketLeftBorder(SprocketDivider, 2.dp) else Modifier)
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 14.dp, vertical = 13.dp)
                ) {
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp,
                            letterSpacing = 1.3.sp,
                            color = fgColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sub,
                            fontWeight = FontWeight.Normal,
                            fontSize = 10.sp,
                            color = fgColor.copy(alpha = 0.75f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
