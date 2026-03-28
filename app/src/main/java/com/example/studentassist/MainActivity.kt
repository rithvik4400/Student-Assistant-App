package com.example.studentassist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import java.util.*
import com.google.firebase.Timestamp

// --- Data Models ---
data class Note(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "General",
    val createdAt: Timestamp? = null
)

data class Assignment(
    val id: String = "",
    val title: String = "",
    val subject: String = "",
    val dueDate: String = "",
    val status: String = "pending"
)

data class Attendance(
    val id: String = "",
    val subject: String = "",
    val attended: Int = 0,
    val total: Int = 0
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF6750A4),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFEADDFF),
                    onPrimaryContainer = Color(0xFF21005D),
                    surface = Color(0xFFFEF7FF),
                    onSurface = Color(0xFF1D1B20)
                )
            ) {
                SmartStudentApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartStudentApp() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Dashboard,
        Screen.Notes,
        Screen.Assignments,
        Screen.Attendance,
        Screen.AI,
        Screen.Profile
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Assist", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title, fontSize = 10.sp) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        },

        ) { innerPadding ->
        NavHost(
            navController,
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") { LoginScreen(navController) }
            composable(Screen.Dashboard.route) { DashboardScreen(navController) }
            composable(Screen.Notes.route) { NotesScreen() }
            composable(Screen.Assignments.route) { AssignmentsScreen() }
            composable(Screen.Attendance.route) { AttendanceScreen() }
            composable(Screen.AI.route) { DoubtSolverScreen()}
            composable(Screen.Profile.route) { ProfileScreen(navController) }
        }
    }
}

// --- Screens ---
@Composable
fun LoginScreen(navController: NavHostController) {

    val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text("Student Assist Login", fontSize = 26.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(20.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        navController.navigate("dashboard")
                    }
                    .addOnFailureListener {
                        println(it.message)
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        navController.navigate("dashboard")
                    }
                    .addOnFailureListener {
                        println(it.message)
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Up")
        }
    }
}

@Composable
fun DashboardScreen(navController: NavHostController) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Welcome back!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Notes", "12", Icons.Default.StickyNote2, Modifier.weight(1f))
            StatCard("Tasks", "5", Icons.Default.CheckCircle, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Icon(Icons.Default.Psychology, "AI", tint = MaterialTheme.colorScheme.primary)
                Text("AI Doubt Solver", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Text("Get instant help with your studies.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = { navController.navigate("ai") }) {
                    Text("Ask Assistant")
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, label, modifier = Modifier.size(20.dp))
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun NotesScreen() {

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    val notes = remember { mutableStateListOf<Note>() }

    var loading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {

        val userId = auth.currentUser?.uid ?: return@LaunchedEffect

        db.collection("notes")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    println("Firestore error: ${error.message}")
                    loading = false
                    return@addSnapshotListener
                }

                notes.clear()

                snapshot?.documents?.forEach { doc ->

                    try {
                        val note = doc.toObject(Note::class.java)?.copy(id = doc.id)

                        if (note != null) {
                            notes.add(note)
                        }

                    } catch (e: Exception) {
                        println("Conversion error: ${e.message}")
                    }

                }

                loading = false
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        if (loading) {

            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )

        } else {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(notes) { note ->

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {

                        Column(modifier = Modifier.padding(16.dp)) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {

                                Text(
                                    note.title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(
                                    onClick = {
                                        db.collection("notes")
                                            .document(note.id)
                                            .delete()
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, "Delete")
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                note.content,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }

    if (showDialog) {

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add Note") },

            text = {

                Column {

                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Content") }
                    )
                }
            },

            confirmButton = {

                Button(onClick = {

                    val userId = auth.currentUser?.uid ?: return@Button

                    val note = hashMapOf(
                        "title" to title,
                        "content" to content,
                        "category" to "General",
                        "userId" to userId,
                        "createdAt" to Date()
                    )

                    db.collection("notes").add(note)

                    title = ""
                    content = ""

                    showDialog = false

                }) {
                    Text("Save")
                }
            },

            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AssignmentsScreen() {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val assignments = remember { mutableStateListOf<Assignment>() }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val userId = auth.currentUser?.uid ?: return@LaunchedEffect
        db.collection("assignments")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                assignments.clear()
                snapshot?.documents?.forEach { doc ->
                    val assignment = doc.toObject(Assignment::class.java)?.copy(id = doc.id)
                    if (assignment != null) assignments.add(assignment)
                }
                loading = false
            }
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(assignments) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (item.status == "completed") MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.status == "completed",
                        onCheckedChange = { checked ->
                            db.collection("assignments").document(item.id).update("status", if (checked) "completed" else "pending")
                        }
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(
                            item.title,
                            fontWeight = FontWeight.Bold,
                            style = if (item.status == "completed") LocalTextStyle.current.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else LocalTextStyle.current
                        )
                        Text(item.subject, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { db.collection("assignments").document(item.id).delete() }) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceScreen() {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val records = remember { mutableStateListOf<Attendance>() }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val userId = auth.currentUser?.uid ?: return@LaunchedEffect
        db.collection("attendance")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                records.clear()
                snapshot?.documents?.forEach { doc ->
                    val record = doc.toObject(Attendance::class.java)?.copy(id = doc.id)
                    if (record != null) records.add(record)
                }
                loading = false
            }
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(records) { record ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(record.subject, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        val percentage = if (record.total > 0) (record.attended.toFloat() / record.total) else 0f
                        LinearProgressIndicator(
                            progress = percentage,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(8.dp),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Attendance: ${record.attended}/${record.total}", fontSize = 12.sp)
                            Text("${(percentage * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DoubtSolverScreen() {
    var query by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("How can I help you today?") }
    var isThinking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Initialize Gemini (Replace with your actual API Key)
    val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = "your_api_key"
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            if (isThinking) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Text(response, modifier = Modifier.verticalScroll(rememberScrollState()))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask a doubt...") },
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            IconButton(
                onClick = {
                    if (query.isBlank()) return@IconButton
                    scope.launch {
                        isThinking = true
                        try {
                            val result = generativeModel.generateContent(query)
                            response = result.text ?: "Sorry, I couldn't process that."
                            query = ""
                        } catch (e: Exception) {
                            response = "Error: ${e.message}"
                        } finally {
                            isThinking = false
                        }
                    }
                },
                enabled = !isThinking
            ) {
                Icon(Icons.Default.Send, "Send", tint = if (isThinking) Color.Gray else MaterialTheme.colorScheme.primary)
            }
        }
    }
}
@Composable
fun ProfileScreen(navController: NavHostController) {

    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Profile",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "User Email",
            fontWeight = FontWeight.Bold
        )

        Text(
            text = user?.email ?: "No Email"
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                auth.signOut()
                navController.navigate("login")
            }
        ) {
            Text("Logout")
        }
    }
}

// --- Navigation Helpers ---
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    object Notes : Screen("notes", "Notes", Icons.Default.StickyNote2)
    object Assignments : Screen("assignments", "Tasks", Icons.Default.CheckCircle)
    object Attendance : Screen("attendance", "Attendance", Icons.Default.Book)
    object AI : Screen("ai", "AI Help", Icons.Default.Psychology)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
}

@Composable
fun rememberScrollState() = androidx.compose.foundation.rememberScrollState()
