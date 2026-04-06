package com.example.sprint1homeui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.sprint1homeui.ui.theme.CoralRed
import com.example.sprint1homeui.ui.theme.HotPink
import com.example.sprint1homeui.ui.theme.SunsetOrange
import com.example.sprint1homeui.ui.theme.WarmWhite

/**
 * Holds the display information for one quick access card.
 */
private data class QuickAccessItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val route: String
)

/**
 * Top-level home screen for the app.
 * This function only assembles the major sections of the page.
 */
@Composable
fun HomeScreen(navController: NavHostController) {
    HomeScreenContainer {
        HomeHeader(navController)
        HomeContent(navController)
    }
}

/**
 * Provides the full-screen background and outer page layout.
 */
@Composable
private fun HomeScreenContainer(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(homeScreenBackgroundBrush())
    ) {
        content()
    }
}

/**
 * Returns the background gradient used by the home screen.
 */
private fun homeScreenBackgroundBrush(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFEE2E2),
            Color(0xFFFED7AA),
            Color(0xFFFCE7F3)
        )
    )
}

/**
 * Displays the content area below the home header.
 */
@Composable
private fun HomeContent(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        QuickAccessSection(navController)

        Spacer(modifier = Modifier.height(24.dp))

        RecentActivitySection()
    }
}

/**
 * Displays the top home header with greeting, profile button, and stats.
 */
@Composable
private fun HomeHeader(navController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .clip(homeHeaderShape())
            .background(homeHeaderBrush())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column {
            HeaderTopRow(navController)

            Spacer(modifier = Modifier.height(16.dp))

            HeaderStatsCard()
        }
    }
}

/**
 * Returns the rounded shape used at the bottom of the home header.
 */
private fun homeHeaderShape(): RoundedCornerShape {
    return RoundedCornerShape(28.dp)
}

/**
 * Returns the gradient used by the home header.
 */
private fun homeHeaderBrush(): Brush {
    return Brush.horizontalGradient(
        colors = listOf(CoralRed, HotPink, SunsetOrange)
    )
}

/**
 * Displays the greeting text and the profile button in the header.
 */
@Composable
private fun HeaderTopRow(navController: NavHostController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        WelcomeMessage()
        ProfileButton(navController)
    }
}

/**
 * Displays the welcome text shown on the left side of the header.
 */
@Composable
private fun WelcomeMessage() {
    Column {
        Text(
            text = "Welcome back!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = WarmWhite
        )
        Text(
            text = "Spring 2026 Semester",
            fontSize = 14.sp,
            color = WarmWhite.copy(alpha = 0.85f)
        )
    }
}

/**
 * Displays the circular profile button used in the header.
 */
@Composable
private fun ProfileButton(navController: NavHostController) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = WarmWhite.copy(alpha = 0.2f),
        onClick = { navController.navigate("profile") }
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Profile",
            tint = WarmWhite,
            modifier = Modifier.padding(8.dp)
        )
    }
}

/**
 * Displays the translucent stats card inside the header.
 */
@Composable
private fun HeaderStatsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = WarmWhite.copy(alpha = 0.14f)
    ) {
        HeaderStatsRow()
    }
}

/**
 * Displays the row of header stats.
 */
@Composable
private fun HeaderStatsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        HeaderStatItem(label = "Campus Map", value = "Open")
        HeaderStatItem(label = "Calendar", value = "Events")
        HeaderStatItem(label = "Study Hub", value = "Rooms")
    }
}

/**
 * Displays one stat item used inside the header.
 */
@Composable
private fun HeaderStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = WarmWhite.copy(alpha = 0.72f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = WarmWhite
        )
    }
}

/**
 * Displays the quick access section title and grid.
 */
@Composable
private fun QuickAccessSection(navController: NavHostController) {
    Column {
        SectionTitle(text = "Quick Access")
        QuickAccessGrid(
            items = quickAccessItems(),
            onItemClick = { route -> navController.navigate(route) }
        )
    }
}

/**
 * Displays a reusable section title.
 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

/**
 * Returns the list of quick access cards shown on the home screen.
 */
private fun quickAccessItems(): List<QuickAccessItem> {
    return listOf(
        QuickAccessItem(
            title = "Campus Map",
            description = "Navigate buildings",
            icon = Icons.Default.Place,
            color = Color(0xFFDC2626),
            route = "map"
        ),
        QuickAccessItem(
            title = "Calendar",
            description = "Campus activities",
            icon = Icons.Default.CalendarMonth,
            color = Color(0xFFE11D48),
            route = "calendar"
        ),
        QuickAccessItem(
            title = "Study Hub",
            description = "Find study spaces",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            color = Color(0xFFF97316),
            route = "studyRoom"
        )
    )
}

/**
 * Displays the grid of quick access cards.
 */
@Composable
private fun QuickAccessGrid(
    items: List<QuickAccessItem>,
    onItemClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(260.dp),
        userScrollEnabled = false
    ) {
        items(items) { item ->
            QuickAccessCard(
                title = item.title,
                description = item.description,
                icon = item.icon,
                color = item.color,
                onClick = { onItemClick(item.route) }
            )
        }
    }
}

/**
 * Displays one quick access card.
 */
@Composable
private fun QuickAccessCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = color,
        shadowElevation = 4.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            QuickAccessIcon(icon = icon, contentDescription = title)

            Spacer(modifier = Modifier.height(8.dp))

            QuickAccessText(title = title, description = description)
        }
    }
}

/**
 * Displays the icon used inside a quick access card.
 */
@Composable
private fun QuickAccessIcon(icon: ImageVector, contentDescription: String) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = Color.White,
        modifier = Modifier.size(24.dp)
    )
}

/**
 * Displays the title and description used inside a quick access card.
 */
@Composable
private fun QuickAccessText(title: String, description: String) {
    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Text(
            text = description,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

/**
 * Displays the recent activity section.
 */
@Composable
private fun RecentActivitySection() {
    Column {
        SectionTitle(text = "Recent Activity")

        ActivityItem(
            title = "Campus tools are ready to use",
            time = "Home page updated",
            category = "Info",
            icon = Icons.Default.Notifications
        )
    }
}

/**
 * Displays one recent activity card.
 */
@Composable
private fun ActivityItem(
    title: String,
    time: String,
    category: String,
    icon: ImageVector
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActivityIcon(icon = icon)

            Spacer(modifier = Modifier.width(12.dp))

            ActivityText(
                title = title,
                time = time,
                modifier = Modifier.weight(1f)
            )

            ActivityCategoryBadge(category = category)
        }
    }
}

/**
 * Displays the circular activity icon on the left side of the activity card.
 */
@Composable
private fun ActivityIcon(icon: ImageVector) {
    Surface(
        modifier = Modifier.size(36.dp),
        shape = CircleShape,
        color = Color(0xFFF43F5E).copy(alpha = 0.1f)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFF43F5E),
            modifier = Modifier.padding(8.dp)
        )
    }
}

/**
 * Displays the main text content inside an activity card.
 */
@Composable
private fun ActivityText(
    title: String,
    time: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = time,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

/**
 * Displays the category badge on the right side of an activity card.
 */
@Composable
private fun ActivityCategoryBadge(category: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFEE2E2)
    ) {
        Text(
            text = category,
            fontSize = 12.sp,
            color = Color(0xFFDC2626),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}