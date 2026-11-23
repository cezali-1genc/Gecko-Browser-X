package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.engine.BrowserTab
import com.example.ui.theme.OperaCrimson
import com.example.ui.theme.OperaCrimsonDark
import com.example.ui.theme.PrivateSurface
import com.example.ui.theme.PrivateViolet
import com.example.ui.theme.VerifiedEmerald

@Composable
fun BottomControlBar(
    tab: BrowserTab,
    tabCount: Int,
    isVpnActive: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onCenterAction: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (tab.isPrivate) PrivateSurface else MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(60.dp)
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    enabled = tab.canGoBack || !tab.isHomePage,
                    modifier = Modifier.testTag("bottom_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }

                IconButton(
                    onClick = onForward,
                    enabled = tab.canGoForward,
                    modifier = Modifier.testTag("bottom_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.cd_forward)
                    )
                }

                // Opera-style Center Hub Button (Home when browsing / Focus single search when on Start Page)
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onCenterAction)
                        .testTag("bottom_home_button"),
                    shape = CircleShape,
                    border = BorderStroke(
                        1.5.dp,
                        if (tab.isPrivate) PrivateViolet.copy(alpha = 0.7f)
                        else Color.White.copy(alpha = 0.16f)
                    ),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier.background(
                            Brush.linearGradient(
                                colors = if (tab.isPrivate) {
                                    listOf(PrivateViolet, Color(0xFF6D28D9))
                                } else {
                                    listOf(OperaCrimson, OperaCrimsonDark)
                                }
                            )
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (tab.isHomePage) Icons.Filled.Search else Icons.Filled.Home,
                            contentDescription = stringResource(R.string.cd_home),
                            tint = Color.White,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                TabCountButton(
                    tabCount = tabCount,
                    isPrivate = tab.isPrivate,
                    onClick = onOpenTabs,
                    modifier = Modifier.testTag("tabs_switcher_button")
                )

                Box(contentAlignment = Alignment.TopEnd) {
                    IconButton(
                        onClick = onOpenMenu,
                        modifier = Modifier.testTag("browser_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.cd_menu)
                        )
                    }
                    if (isVpnActive) {
                        Box(
                            modifier = Modifier
                                .padding(top = 10.dp, end = 10.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(VerifiedEmerald)
                        )
                    }
                }
            }
        }
    }
}
