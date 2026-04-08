package com.example.cicompanion.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.cicompanion.ui.theme.AppBackground
import com.example.cicompanion.ui.theme.CICompanionTheme

//Dummy data model
//Can expand with firebase needs
data class DummyUser(
    val id: String,
    val name: String,
    val email: String,
    val major: String
)

@Composable
fun UserSearchScreen(navController: NavHostController) {
    //Dummy user data
    val dummyUsers = remember {
        listOf(
            DummyUser("1","Alice Johnson", "alice@students.ci.edu", "Computer Science"),
            DummyUser("2","Brandon Lee", "brandon@students.ci.edu", "Information Systems"),
            DummyUser("3","Cynthia Park", "cynthia@students.ci.edu", "Data Science"),
            DummyUser("4","Daniel Smith", "daniel@students.ci.edu", "Computer Science"),
            DummyUser("5","Eva Martinez", "eva@students.ci.edu", "Software Engineering")
        )
    }

    var searchQuery by remember { mutableStateOf("") }

    //Dummy state for now, add firebase functionality
    val addedUsers = remember { mutableStateMapOf<String, Boolean>() }

    val filteredUsers = if (searchQuery.isBlank()) {
        emptyList()
    } else {
        //Dummy source
        dummyUsers.filter { user ->
            user.name.contains(searchQuery, ignoreCase = true) ||
                    user.email.contains(searchQuery, ignoreCase = true) ||
                    user.major.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = AppBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search for a user...") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                items(filteredUsers) { user ->
                    UserSearchResultItem(
                        user = user,
                        isAdded = addedUsers[user.id] == true, //Dummy feature, local state only
                        onClick = {
                            // Placeholder for future Firebase/user profile navigation
                            // Example later:
                            // navController.navigate("user_profile/${user.id}")
                            // Maybe
                        },
                        onAddClick = {
                            // Dummy add button functionality for now
                            // Replace with Firebase add friend / request logic later
                            addedUsers[user.id] = true
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun UserSearchResultItem(
    user: DummyUser,
    isAdded: Boolean,
    onClick: () -> Unit,
    onAddClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = user.name,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                text = "${user.email} • ${user.major}",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        },
        trailingContent = {
            Button(
                onClick = onAddClick,
                enabled = !isAdded
            ) {
                //Dummy UI, only reflects local dummy state, not persistent
                Text(if (isAdded) "Added" else "Add")
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Preview(showBackground = true)
@Composable
fun UserSearchScreenPreview() {
    CICompanionTheme {
        val navController = rememberNavController()
        UserSearchScreen(navController = navController)
    }
}