package com.example.cicompanion.social

// unused imports are for the yet-to-be-implemented, fleshed-out top bar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.cicompanion.ui.theme.CICompanionTheme
import androidx.compose.material3.TopAppBar
import com.example.cicompanion.ui.Routes
import com.example.cicompanion.ui.theme.AppBackground
import com.example.cicompanion.ui.theme.GrayIcon
import com.example.cicompanion.ui.theme.GreenAccent
import com.example.cicompanion.ui.theme.NavBackground


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {
    Scaffold (
        containerColor = AppBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.USER_SEARCH) },
                shape = CircleShape,
                containerColor = NavBackground,
                contentColor = Color.Black
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Search Users"
                )
            }
        }
    ){ innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(AppBackground) //Background of user info
        ) {
            ProfileHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(
                        width = 1.dp,
                        color = GrayIcon.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(NavBackground)


            )

            // Gray content area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(AppBackground), // Whole screen background

                contentAlignment = Alignment.Center
            ) {

            }
        }
    }
}

@Composable
fun ProfileHeader(modifier: Modifier = Modifier) {
    Surface(
        modifier = Modifier,
        color = AppBackground,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image placeholder
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE6E0F8)), // Light purple background
                contentAlignment = Alignment.Center
            ) {
                // Silhouette
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6750A4)) // Darker purple
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(80.dp, 40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6750A4))
                    )
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column {
                Text(
                    text = "User Name",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.Black
                )
                Text(
                    text = "user@example.com",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Friends: 0",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
            }
        }
    }
    }

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    CICompanionTheme() {
        val navController = rememberNavController()
        ProfileScreen(navController = navController)
    }
}
