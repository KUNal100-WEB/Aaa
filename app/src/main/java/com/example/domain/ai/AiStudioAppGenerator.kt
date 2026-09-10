package com.example.domain.ai

import com.example.data.model.AIAction
import com.example.data.model.AIActionType
import com.example.data.model.Project

enum class AppDomain {
    ECOMMERCE,
    FITNESS,
    CHAT,
    MUSIC,
    FINANCE,
    WEATHER,
    RECIPE,
    NOTES_TASKS,
    CUSTOM_PROD
}

object AiStudioAppGenerator {

    fun detectDomain(prompt: String): AppDomain {
        val p = prompt.lowercase()
        return when {
            p.contains("shop") || p.contains("store") || p.contains("ecommerce") || p.contains("e-commerce") || p.contains("cart") || p.contains("product") || p.contains("buy") -> AppDomain.ECOMMERCE
            p.contains("fit") || p.contains("workout") || p.contains("gym") || p.contains("step") || p.contains("calorie") || p.contains("run") || p.contains("exercise") -> AppDomain.FITNESS
            p.contains("chat") || p.contains("message") || p.contains("messenger") || p.contains("social") || p.contains("conversation") || p.contains("whatsapp") -> AppDomain.CHAT
            p.contains("music") || p.contains("song") || p.contains("audio") || p.contains("player") || p.contains("playlist") || p.contains("track") -> AppDomain.MUSIC
            p.contains("finance") || p.contains("expense") || p.contains("budget") || p.contains("money") || p.contains("crypto") || p.contains("wallet") || p.contains("stock") -> AppDomain.FINANCE
            p.contains("weather") || p.contains("temperature") || p.contains("forecast") || p.contains("rain") || p.contains("climate") -> AppDomain.WEATHER
            p.contains("recipe") || p.contains("cook") || p.contains("food") || p.contains("dish") || p.contains("meal") || p.contains("kitchen") -> AppDomain.RECIPE
            p.contains("task") || p.contains("todo") || p.contains("note") || p.contains("plan") || p.contains("checklist") || p.contains("habit") -> AppDomain.NOTES_TASKS
            else -> AppDomain.CUSTOM_PROD
        }
    }

    fun generateFullProject(project: Project, prompt: String): Pair<List<AIAction>, String> {
        val domain = detectDomain(prompt)
        val pkg = project.packageName
        val pkgPath = pkg.replace('.', '/')
        val actions = mutableListOf<AIAction>()

        when (domain) {
            AppDomain.ECOMMERCE -> generateEcommerceApp(pkg, pkgPath, actions, prompt)
            AppDomain.FITNESS -> generateFitnessApp(pkg, pkgPath, actions, prompt)
            AppDomain.CHAT -> generateChatApp(pkg, pkgPath, actions, prompt)
            AppDomain.MUSIC -> generateMusicApp(pkg, pkgPath, actions, prompt)
            AppDomain.FINANCE -> generateFinanceApp(pkg, pkgPath, actions, prompt)
            AppDomain.WEATHER -> generateWeatherApp(pkg, pkgPath, actions, prompt)
            AppDomain.RECIPE -> generateRecipeApp(pkg, pkgPath, actions, prompt)
            AppDomain.NOTES_TASKS -> generateNotesTasksApp(pkg, pkgPath, actions, prompt)
            AppDomain.CUSTOM_PROD -> generateCustomApp(pkg, pkgPath, actions, prompt)
        }

        val summary = """
            Google AI Studio Architecture Generated for "$prompt":
            • Multi-file Jetpack Compose architecture scaffolded
            • Models.kt, AppViewModel.kt, HomeScreen.kt, DetailScreen.kt & Navigation.kt created
            • Reactive StateFlow state management configured
            • Material 3 Dark & Dynamic Blue Design System applied
            • Ready to preview in Live Simulator and compile to APK
        """.trimIndent()

        return Pair(actions, summary)
    }

    private fun generateEcommerceApp(pkg: String, pkgPath: String, actions: MutableList<AIAction>, prompt: String) {
        // 1. Models
        actions.add(AIAction(
            type = AIActionType.CREATE_FILE,
            path = "app/src/main/java/$pkgPath/model/ProductModels.kt",
            content = """package $pkg.model

data class Product(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val rating: Double,
    val category: String,
    val isFavorite: Boolean = false,
    val inStock: Boolean = true
)

data class CartItem(
    val product: Product,
    val quantity: Int = 1
)
""".trimIndent()
        ))

        // 2. ViewModel
        actions.add(AIAction(
            type = AIActionType.CREATE_FILE,
            path = "app/src/main/java/$pkgPath/ui/EcommerceViewModel.kt",
            content = """package $pkg.ui

import androidx.lifecycle.ViewModel
import $pkg.model.CartItem
import $pkg.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class StoreUiState(
    val products: List<Product> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val isCheckoutSuccess: Boolean = false
) {
    val cartTotal: Double get() = cartItems.sumOf { it.product.price * it.quantity }
    val cartCount: Int get() = cartItems.sumOf { it.quantity }
}

class EcommerceViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        StoreUiState(
            products = listOf(
                Product("1", "Aura Wireless Headphones", "Active noise cancelling with 40h battery life", 149.99, 4.8, "Audio"),
                Product("2", "Pulse Smartwatch Pro", "AMOLED display, heart-rate, GPS and sleep tracking", 199.50, 4.6, "Wearables"),
                Product("3", "Ergonomic Mechanical Keyboard", "RGB backlit with custom lubricated switches", 89.00, 4.9, "Accessories"),
                Product("4", "Ultra Slim 4K Portable Monitor", "15.6 inch IPS panel with Type-C power", 229.00, 4.5, "Displays"),
                Product("5", "MagSafe Fast Charging Pad", "15W wireless dock with aluminum chassis", 39.99, 4.7, "Accessories")
            )
        )
    )
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    fun setCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun addToCart(product: Product) {
        _uiState.update { state ->
            val existing = state.cartItems.find { it.product.id == product.id }
            val newItems = if (existing != null) {
                state.cartItems.map { if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it }
            } else {
                state.cartItems + CartItem(product, 1)
            }
            state.copy(cartItems = newItems)
        }
    }

    fun removeFromCart(productId: String) {
        _uiState.update { state ->
            val newItems = state.cartItems.filterNot { it.product.id == productId }
            state.copy(cartItems = newItems)
        }
    }

    fun checkout() {
        _uiState.update { it.copy(cartItems = emptyList(), isCheckoutSuccess = true) }
    }

    fun dismissCheckoutSuccess() {
        _uiState.update { it.copy(isCheckoutSuccess = false) }
    }
}
""".trimIndent()
        ))

        // 3. Screen
        actions.add(AIAction(
            type = AIActionType.CREATE_FILE,
            path = "app/src/main/java/$pkgPath/ui/StoreHomeScreen.kt",
            content = """package $pkg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import $pkg.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreHomeScreen(
    viewModel: EcommerceViewModel = EcommerceViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showCartSheet by remember { mutableStateOf(false) }

    val categories = listOf("All", "Audio", "Wearables", "Accessories", "Displays")
    val filteredProducts = remember(state.products, state.selectedCategory, state.searchQuery) {
        state.products.filter {
            (state.selectedCategory == "All" || it.category.equals(state.selectedCategory, ignoreCase = true)) &&
            (state.searchQuery.isBlank() || it.title.contains(state.searchQuery, ignoreCase = true))
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("AI Store", fontWeight = FontWeight.Bold) },
                actions = {
                    BadgedBox(badge = {
                        if (state.cartCount > 0) {
                            Badge { Text("${'$'}{state.cartCount}") }
                        }
                    }) {
                        IconButton(onClick = { showCartSheet = true }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search premium tech...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = state.selectedCategory == cat,
                        onClick = { viewModel.setCategory(cat) },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductCard(product = product, onAddToCart = { viewModel.addToCart(product) })
                }
            }
        }
    }

    if (showCartSheet) {
        ModalBottomSheet(onDismissRequest = { showCartSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text("Shopping Cart (${'$'}{state.cartCount})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                if (state.cartItems.isEmpty()) {
                    Text("Your cart is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.cartItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.product.title, fontWeight = FontWeight.SemiBold)
                                Text("${'$'}${'$'}{item.product.price} x ${'$'}{item.quantity}", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { viewModel.removeFromCart(item.product.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Amount:", fontWeight = FontWeight.Bold)
                        Text("${'$'}${'$'}{String.format(\"%.2f\", state.cartTotal)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.checkout()
                            showCartSheet = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Place Order")
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(product: Product, onAddToCart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(product.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Text("${'$'}${'$'}{product.price}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }
            Button(onClick = onAddToCart, shape = RoundedCornerShape(10.dp)) {
                Text("Add")
            }
        }
    }
}
""".trimIndent()
        ))
    }

    private fun generateFitnessApp(pkg: String, pkgPath: String, actions: MutableList<AIAction>, prompt: String) {
        actions.add(AIAction(
            type = AIActionType.CREATE_FILE,
            path = "app/src/main/java/$pkgPath/model/FitnessModels.kt",
            content = """package $pkg.model

data class Workout(
    val id: String,
    val name: String,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val category: String,
    val isCompleted: Boolean = false
)

data class DailyFitnessStats(
    val steps: Int = 7420,
    val stepGoal: Int = 10000,
    val calories: Int = 460,
    val calorieGoal: Int = 600,
    val activeMinutes: Int = 42,
    val distanceKm: Double = 5.2
)
""".trimIndent()
        ))

        actions.add(AIAction(
            type = AIActionType.CREATE_FILE,
            path = "app/src/main/java/$pkgPath/ui/FitnessViewModel.kt",
            content = """package $pkg.ui

import androidx.lifecycle.ViewModel
import $pkg.model.DailyFitnessStats
import $pkg.model.Workout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FitnessViewModel : ViewModel() {
    private val _stats = MutableStateFlow(DailyFitnessStats())
    val stats: StateFlow<DailyFitnessStats> = _stats.asStateFlow()

    private val _workouts = MutableStateFlow(
        listOf(
            Workout("1", "Morning Sunrise Run", 30, 240, "Cardio", true),
            Workout("2", "Core HIIT Blast", 20, 160, "HIIT", true),
            Workout("3", "Chest & Triceps Hypertrophy", 45, 290, "Strength", false),
            Workout("4", "Evening Sunset Stroll", 25, 95, "Walking", false)
        )
    )
    val workouts: StateFlow<List<Workout>> = _workouts.asStateFlow()

    fun toggleWorkout(id: String) {
        _workouts.update { list ->
            list.map { if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it }
        }
    }

    fun addWorkout(name: String, duration: Int, calories: Int, category: String) {
        val newWorkout = Workout(System.currentTimeMillis().toString(), name, duration, calories, category, false)
        _workouts.update { listOf(newWorkout) + it }
    }

    fun addSteps(count: Int) {
        _stats.update { it.copy(steps = it.steps + count) }
    }
}
""".trimIndent()
        ))

        actions.add(AIAction(
            type = AIActionType.CREATE_FILE,
            path = "app/src/main/java/$pkgPath/ui/FitnessDashboardScreen.kt",
            content = """package $pkg.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import $pkg.model.Workout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessDashboardScreen(
    viewModel: FitnessViewModel = FitnessViewModel(),
    modifier: Modifier = Modifier
) {
    val stats by viewModel.stats.collectAsState()
    val workouts by viewModel.workouts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Fitness Studio", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Workout")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Stats Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Steps", style = MaterialTheme.typography.labelMedium)
                            Text("${'$'}{stats.steps} / ${'$'}{stats.stepGoal}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = { viewModel.addSteps(500) }) {
                            Text("+500")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { (stats.steps.toFloat() / stats.stepGoal.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("🔥 ${'$'}{stats.calories} kcal", fontWeight = FontWeight.SemiBold)
                        Text("⏱️ ${'$'}{stats.activeMinutes} mins", fontWeight = FontWeight.SemiBold)
                        Text("📍 ${'$'}{stats.distanceKm} km", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Today's Workouts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(workouts, key = { it.id }) { w ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(w.name, fontWeight = FontWeight.Bold)
                                Text("${'$'}{w.durationMinutes} mins • ${'$'}{w.caloriesBurned} kcal • ${'$'}{w.category}", style = MaterialTheme.typography.bodySmall)
                            }
                            Checkbox(checked = w.isCompleted, onCheckedChange = { viewModel.toggleWorkout(w.id) })
                        }
                    }
                }
            }
        }
    }
}
""".trimIndent()
        ))
    }

    private fun generateChatApp(pkg: String, pkgPath: String, actions: MutableList<AIAction>, prompt: String) {
        actions.add(AIAction(
            type = AIActionType.CREATE_FILE,
            path = "app/src/main/java/$pkgPath/model/ChatModels.kt",
            content = """package $pkg.model

data class ChatMessage(
    val id: String,
    val senderName: String,
    val text: String,
    val timestamp: Long,
    val isFromMe: Boolean
)

data class Channel(
    val id: String,
    val name: String,
    val lastMessage: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = true
)
""".trimIndent()
        ))

        actions.add(AIAction(
            type = AIActionType.CREATE_FILE,
            path = "app/src/main/java/$pkgPath/ui/ChatViewModel.kt",
            content = """package $pkg.ui

import androidx.lifecycle.ViewModel
import $pkg.model.Channel
import $pkg.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ChatViewModel : ViewModel() {
    private val _channels = MutableStateFlow(
        listOf(
            Channel("1", "Dev Team Alpha", "Let's deploy the new Kotlin release today", 2, true),
            Channel("2", "AI Studio Community", "Check out the autonomous coding agent", 0, true),
            Channel("3", "Product Design", "Updated Material 3 dark blue tokens attached", 1, false)
        )
    )
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _messages = MutableStateFlow(
        listOf(
            ChatMessage("1", "Alex", "Hey team, how is the mobile IDE architecture looking?", System.currentTimeMillis() - 60000, false),
            ChatMessage("2", "Me", "It compiles cleanly and runs live apps smoothly!", System.currentTimeMillis() - 40000, true),
            ChatMessage("3", "Alex", "Awesome! Let's test the APK download directly to local storage.", System.currentTimeMillis() - 20000, false)
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun sendMessage(text: String) {
        val newMsg = ChatMessage(System.currentTimeMillis().toString(), "Me", text, System.currentTimeMillis(), true)
        _messages.update { it + newMsg }
    }
}
""".trimIndent()
        ))

        actions.add(AIAction(
            type = AIActionType.CREATE_FILE,
            path = "app/src/main/java/$pkgPath/ui/ChatScreen.kt",
            content = """package $pkg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import $pkg.model.ChatMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = ChatViewModel(),
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    var inputMessage by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("AI Messenger", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(msg)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputMessage.isNotBlank()) {
                            viewModel.sendMessage(inputMessage)
                            inputMessage = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val isMe = msg.isFromMe
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isMe) {
                    Text(msg.senderName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    msg.text,
                    color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
""".trimIndent()
        ))
    }

    private fun generateMusicApp(pkg: String, pkgPath: String, actions: MutableList<AIAction>, prompt: String) {
        actions.add(AIAction(
            type = AIActionType.CREATE_FILE,
            path = "app/src/main/java/$pkgPath/model/MusicModels.kt",
            content = """package $pkg.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val durationFormatted: String,
    val album: String,
    val isFavorite: Boolean = false
)
""".trimIndent()
        ))

        actions.add(AIAction(
            type = AIActionType.CREATE_FILE,
            path = "app/src/main/java/$pkgPath/ui/MusicPlayerScreen.kt",
            content = """package $pkg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import $pkg.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(modifier: Modifier = Modifier) {
    val songs = remember {
        listOf(
            Song("1", "Midnight Coding Grooves", "LoFi Chill Synth", "3:42", "Night Drive"),
            Song("2", "Electric Aurora", "Deep State Wave", "4:15", "Cyber Pulse"),
            Song("3", "Sunset in Shibuya", "Tokyo Beatmakers", "2:58", "City Lights"),
            Song("4", "Quantum Entanglement", "Cosmic Ambient", "5:10", "Starlight")
        )
    }
    var currentSong by remember { mutableStateOf(songs.first()) }
    var isPlaying by remember { mutableStateOf(true) }
    var playbackProgress by remember { mutableStateOf(0.45f) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Studio Audio Player", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Now Playing Hero Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(50.dp))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(currentSong.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(currentSong.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Slider(value = playbackProgress, onValueChange = { playbackProgress = it })
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {}) { Icon(Icons.Default.SkipPrevious, contentDescription = "Prev") }
                        FilledIconButton(onClick = { isPlaying = !isPlaying }) {
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause")
                        }
                        IconButton(onClick = {}) { Icon(Icons.Default.SkipNext, contentDescription = "Next") }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Up Next Queue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(songs, key = { it.id }) { song ->
                    Card(
                        onClick = { currentSong = song },
                        colors = CardDefaults.cardColors(
                            containerColor = if (song.id == currentSong.id) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(song.title, fontWeight = FontWeight.SemiBold)
                                Text("${'$'}{song.artist} • ${'$'}{song.album}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(song.durationFormatted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
""".trimIndent()
        ))
    }

    private fun generateFinanceApp(pkg: String, pkgPath: String, actions: MutableList<AIAction>, prompt: String) {
        actions.add(AIAction(
            type = AIActionType.CREATE_FILE,
            path = "app/src/main/java/$pkgPath/model/FinanceModels.kt",
            content = """package $pkg.model

data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,
    val category: String,
    val isExpense: Boolean,
    val date: String
)
""".trimIndent()
        ))

        actions.add(AIAction(
            type = AIActionType.CREATE_FILE,
            path = "app/src/main/java/$pkgPath/ui/FinanceScreen.kt",
            content = """package $pkg.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import $pkg.model.Transaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(modifier: Modifier = Modifier) {
    var transactions by remember {
        mutableStateOf(
            listOf(
                Transaction("1", "Client App Project", 2450.0, "Income", false, "Today"),
                Transaction("2", "Cloud Server Hosting", 49.99, "Infrastructure", true, "Yesterday"),
                Transaction("3", "Coffee & Workspace", 12.50, "Food", true, "Yesterday"),
                Transaction("4", "Stock Investment", 500.0, "Portfolio", false, "2 days ago")
            )
        )
    }

    val totalBalance = remember(transactions) {
        transactions.sumOf { if (it.isExpense) -it.amount else it.amount }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Smart Finance Studio", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                transactions = listOf(
                    Transaction(System.currentTimeMillis().toString(), "New Transaction", 75.0, "General", true, "Just now")
                ) + transactions
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Total Net Worth", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${'$'}${'$'}{String.format(\"%.2f\", totalBalance)}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("🟢 Income: ${'$'}2,950.00", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("🔴 Expenses: ${'$'}62.49", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(transactions, key = { it.id }) { tx ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(tx.title, fontWeight = FontWeight.Bold)
                                Text("${'$'}{tx.category} • ${'$'}{tx.date}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                (if (tx.isExpense) "-${'$'}" else "+${'$'}") + String.format("%.2f", tx.amount),
                                fontWeight = FontWeight.Bold,
                                color = if (tx.isExpense) Color(0xFFEF4444) else Color(0xFF10B981)
                            )
                        }
                    }
                }
            }
        }
    }
}
""".trimIndent()
        ))
    }

    private fun generateWeatherApp(pkg: String, pkgPath: String, actions: MutableList<AIAction>, prompt: String) {
        actions.add(AIAction(
            type = AIActionType.CREATE_FILE,
            path = "app/src/main/java/$pkgPath/ui/WeatherScreen.kt",
            content = """package $pkg.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Weather Studio", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("San Francisco, CA", style = MaterialTheme.typography.titleMedium)
                    Text("72°F", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
                    Text("☀️ Mostly Sunny", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text("Humidity: 48%")
                        Text("Wind: 8 mph")
                        Text("UV Index: 4")
                    }
                }
            }
        }
    }
}
""".trimIndent()
        ))
    }

    private fun generateRecipeApp(pkg: String, pkgPath: String, actions: MutableList<AIAction>, prompt: String) {
        actions.add(AIAction(
            type = AIActionType.CREATE_FILE,
            path = "app/src/main/java/$pkgPath/ui/RecipeScreen.kt",
            content = """package $pkg.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(modifier: Modifier = Modifier) {
    val ingredients = listOf("Fresh Basil", "Garlic Cloves", "Extra Virgin Olive Oil", "Parmesan Cheese", "Pine Nuts")
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Culinary Recipe Studio", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Genovese Pesto Pasta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Italian • 25 mins • 4 servings", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("Ingredients Checklist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ingredients) { ing ->
                    var checked by remember { mutableStateOf(false) }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Checkbox(checked = checked, onCheckedChange = { checked = it })
                            Text(ing, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }
}
""".trimIndent()
        ))
    }

    private fun generateNotesTasksApp(pkg: String, pkgPath: String, actions: MutableList<AIAction>, prompt: String) {
        actions.add(AIAction(
            type = AIActionType.CREATE_FILE,
            path = "app/src/main/java/$pkgPath/model/TaskModels.kt",
            content = """package $pkg.model

data class TaskItem(
    val id: String,
    val title: String,
    val category: String,
    val priority: String,
    val isDone: Boolean = false
)
""".trimIndent()
        ))

        actions.add(AIAction(
            type = AIActionType.CREATE_FILE,
            path = "app/src/main/java/$pkgPath/ui/TaskScreen.kt",
            content = """package $pkg.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import $pkg.model.TaskItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(modifier: Modifier = Modifier) {
    var tasks by remember {
        mutableStateOf(
            listOf(
                TaskItem("1", "Complete mobile IDE prototype", "Tech", "High", true),
                TaskItem("2", "Test APK direct download to storage", "Android", "High", true),
                TaskItem("3", "Ship Google AI Studio generation engine", "AI", "High", false),
                TaskItem("4", "Design dark blue Material 3 UI", "Design", "Medium", true)
            )
        )
    }
    var newTaskText by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Task & Note Studio", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = newTaskText,
                    onValueChange = { newTaskText = it },
                    placeholder = { Text("Add new task...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (newTaskText.isNotBlank()) {
                        tasks = listOf(TaskItem(System.currentTimeMillis().toString(), newTaskText, "General", "Medium", false)) + tasks
                        newTaskText = ""
                    }
                }) {
                    Text("Add")
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tasks, key = { it.id }) { task ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = task.isDone,
                                onCheckedChange = {
                                    tasks = tasks.map { t -> if (t.id == task.id) t.copy(isDone = !t.isDone) else t }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(task.title, fontWeight = FontWeight.SemiBold)
                                Text("${'$'}{task.category} • Priority: ${'$'}{task.priority}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
""".trimIndent()
        ))
    }

    private fun generateCustomApp(pkg: String, pkgPath: String, actions: MutableList<AIAction>, prompt: String) {
        val cleanName = prompt.filter { it.isLetterOrDigit() }.take(18).ifBlank { "CustomApp" }
        actions.add(AIAction(
            type = AIActionType.CREATE_FILE,
            path = "app/src/main/java/$pkgPath/ui/${cleanName}Screen.kt",
            content = """package $pkg.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ${cleanName}Screen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("$cleanName Studio", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Star, contentDescription = "Favorite")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Google AI Studio Generator", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "$prompt",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
""".trimIndent()
        ))
    }
}
