package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.*
import com.example.data.repository.MonicoRepository
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: MonicoRepository,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onOpenUrl: (title: String, url: String) -> Unit,
    onOpenImageViewer: (title: String, images: List<String>) -> Unit,
    onOpenAdminPanel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val currentUser by repository.currentUser.collectAsState()
    val menus by repository.menus.collectAsState()
    val sliders by repository.sliders.collectAsState()
    val notices by repository.notices.collectAsState()
    val popups by repository.popups.collectAsState()
    val productCards by repository.productCards.collectAsState()
    val trainingCards by repository.trainingCards.collectAsState()
    val targetSheets by repository.targetSheets.collectAsState()
    val sidebarMenus by repository.sidebarMenus.collectAsState()
    val lastSyncTime by repository.lastSyncTime.collectAsState()
    val isSyncing by repository.isSyncing.collectAsState()

    var showProfileDialog by remember { mutableStateOf(false) }
    var activePopupNotice by remember { mutableStateOf<PopupNotice?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf("ALL") }

    // Initial popup display
    LaunchedEffect(popups) {
        val active = popups.firstOrNull { it.status == "active" }
        if (active != null && activePopupNotice == null) {
            activePopupNotice = active
        }
    }

    val userRole = (currentUser?.role ?: "MPO").trim().uppercase()
    val isSuperAdmin = userRole == "SUPER_ADMIN" || userRole == "SUPER ADMIN"
    val isDIC = userRole == "DIC"

    // Role restriction: MPO, AM, RSM, TM, DA, ADIC cannot see or access sidemenu
    val canViewSidebar = isSuperAdmin || isDIC

    // Role-based access helper
    val primaryRoles = listOf("MPO", "AM", "RSM", "TM", "SUPER_ADMIN")
    val specialRoles = listOf("DIC", "ADIC", "DA", "SUPER_ADMIN")

    fun hasAccess(itemAllowedRole: String): Boolean {
        if (isSuperAdmin) return true
        if (itemAllowedRole.isBlank() || itemAllowedRole.equals("ALL", ignoreCase = true)) return true
        val allowed = itemAllowedRole.split(",").map { it.trim().uppercase() }
        if (allowed.contains("ALL") || allowed.contains("PALL+SALL")) return true
        if (userRole == "PALL+SALL") return true
        if (allowed.contains("PALL") && primaryRoles.contains(userRole)) return true
        if (allowed.contains("SALL") && specialRoles.contains(userRole)) return true
        if (allowed.contains(userRole)) return true
        return false
    }

    // Filter menus
    val accessibleMenus = remember(menus, userRole) {
        menus.filter { it.status != "inactive" && hasAccess(it.allowedRole) }
    }

    val filteredMenus = remember(accessibleMenus, searchQuery, selectedMonth) {
        accessibleMenus.filter { item ->
            val matchQuery = searchQuery.isBlank() || item.title.contains(searchQuery, ignoreCase = true)
            val matchMonth = selectedMonth == "ALL" || item.month.equals(selectedMonth, ignoreCase = true) || item.group == "grid1"
            matchQuery && matchMonth
        }
    }

    val pinnedMenus = remember(filteredMenus) {
        filteredMenus.filter { it.isPinned }
    }

    val grid1Menus = remember(filteredMenus) { filteredMenus.filter { it.group == "grid1" } }
    val grid2Menus = remember(filteredMenus) { filteredMenus.filter { it.group == "grid2" } }
    val grid3Menus = remember(filteredMenus) { filteredMenus.filter { it.group == "grid3" } }
    val grid4Menus = remember(filteredMenus) { filteredMenus.filter { it.group == "grid4" } }
    val grid5Menus = remember(filteredMenus) { filteredMenus.filter { it.group == "grid5" } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = canViewSidebar,
        drawerContent = {
            if (canViewSidebar) {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.width(300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.monico_one_icon_1791604475434),
                                contentDescription = "Logo",
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                            )
                            Column {
                                Text("Monico One", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Enterprise Admin Portal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = MonicoBlue) },
                            label = { Text("অ্যাডমিন প্যানেল (Admin Console)", fontWeight = FontWeight.SemiBold) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onOpenAdminPanel()
                            }
                        )

                        Text("সাইডবার লিংক ও পোর্টাল", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        sidebarMenus.filter { it.status == "active" }.forEach { sm ->
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Link, contentDescription = null, tint = MonicoBlue) },
                                label = { Text(sm.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                selected = false,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    if (sm.url.startsWith("http")) {
                                        onOpenUrl(sm.name, sm.url)
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Version 1.0.0 (Mobile Firebase)\n© 2026 Monico One",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp
                ) {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Hamburger icon only visible to SUPER_ADMIN and DIC
                                if (canViewSidebar) {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                                    }
                                }

                                Image(
                                    painter = painterResource(id = R.drawable.monico_one_icon_1791604475434),
                                    contentDescription = "Monico One",
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )

                                AnimatedVisibility(visible = !isSearchActive) {
                                    Column {
                                        Text(
                                            text = currentUser?.fullName ?: (currentUser?.employeeId ?: "Employee"),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "ID: ${currentUser?.employeeId} • ${currentUser?.role}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MonicoBlue,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = isSearchActive, modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("মেনু বা ফাইল খুঁজুন...", fontSize = 12.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                searchQuery = ""
                                                isSearchActive = false
                                            }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (!isSearchActive) {
                                    IconButton(onClick = { isSearchActive = true }) {
                                        Icon(Icons.Default.Search, contentDescription = "Search")
                                    }
                                }

                                // Sync button
                                IconButton(
                                    onClick = { scope.launch { repository.syncAll() } },
                                    enabled = !isSyncing
                                ) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MonicoBlue)
                                    } else {
                                        Icon(Icons.Default.Sync, contentDescription = "Sync", tint = MonicoBlue)
                                    }
                                }

                                // Dark mode toggle
                                IconButton(onClick = onToggleDarkMode) {
                                    Icon(
                                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = "Toggle theme"
                                    )
                                }

                                // Admin panel button (Only for SUPER_ADMIN or DIC)
                                if (isSuperAdmin || isDIC) {
                                    IconButton(onClick = onOpenAdminPanel) {
                                        Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Panel", tint = MonicoAmber)
                                    }
                                }

                                // Profile button
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .clickable { showProfileDialog = true }
                                ) {
                                    val pic = currentUser?.profilePic ?: ""
                                    if (pic.isNotBlank() && (pic.startsWith("http") || pic.startsWith("data:"))) {
                                        AsyncImage(
                                            model = pic,
                                            contentDescription = "Profile",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = R.drawable.monico_one_icon_1791604475434),
                                            contentDescription = "Profile",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }

                        // Sync timestamp bar if available
                        if (lastSyncTime.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "সর্বশেষ সিঙ্ক: $lastSyncTime",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Role: ${currentUser?.role}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MonicoBlue
                                )
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize().testTag("dashboard_screen")
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Sliders Banner Carousel
                if (sliders.isNotEmpty()) {
                    item {
                        SliderCarousel(sliders = sliders, onSliderClick = { s ->
                            if (s.image.isNotBlank()) {
                                onOpenImageViewer(s.title, listOf(s.image))
                            }
                        })
                    }
                }

                // 2. Announcement Ticker (Marquee)
                if (notices.isNotEmpty()) {
                    item {
                        NoticeTicker(notices = notices)
                    }
                }

                // 3. Fast Access / Pinned Items
                if (pinnedMenus.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "পিন করা ফাইল (FAST ACCESS)",
                            icon = Icons.Default.PushPin,
                            color = MonicoAmber
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(pinnedMenus) { item ->
                                MenuItemCard(
                                    item = item,
                                    currentEmpId = currentUser?.employeeId ?: "",
                                    onClick = {
                                        scope.launch { repository.markMenuSeen(item.id) }
                                        handleMenuCardClick(item, onOpenUrl, onOpenImageViewer)
                                    }
                                )
                            }
                        }
                    }
                }

                // 4. Month Filter Bar
                item {
                    MonthFilterBar(
                        selectedMonth = selectedMonth,
                        onMonthSelected = { selectedMonth = it },
                        activeRole = currentUser?.role ?: "MPO"
                    )
                }

                // 5. Grid 1: Primary Menus (Daily Work)
                if (grid1Menus.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "দৈনিক কাজের মেনু (PRIMARY MENUS)",
                            icon = Icons.Default.Work,
                            color = MonicoBlue
                        )
                    }
                    item {
                        PrimaryMenusGrid(
                            menus = grid1Menus,
                            currentEmpId = currentUser?.employeeId ?: "",
                            onMenuClick = { item ->
                                scope.launch { repository.markMenuSeen(item.id) }
                                handleMenuCardClick(item, onOpenUrl, onOpenImageViewer)
                            }
                        )
                    }
                }

                // 6. Grid 2: Flat Bonus Sheet (তারিখ, মাস ও পৃষ্ঠা সংখ্যাসহ বোনাস শিট এবং আনরিড রেড ডট ইন্ডিকেটর)
                item {
                    SectionHeader(
                        title = "ফ্ল্যাট বোনাস শিট (FLAT BONUS SHEET)",
                        icon = Icons.Default.CardGiftcard,
                        color = MonicoAmber
                    )
                    if (grid2Menus.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(grid2Menus) { item ->
                                MenuItemCard(
                                    item = item,
                                    currentEmpId = currentUser?.employeeId ?: "",
                                    onClick = {
                                        scope.launch { repository.markMenuSeen(item.id) }
                                        handleMenuCardClick(item, onOpenUrl, onOpenImageViewer)
                                    }
                                )
                            }
                        }
                    } else {
                        EmptySectionNotice("ফ্ল্যাট বোনাস শিটের কোনো ফাইল নেই।")
                    }
                }

                // 7. Grid 3: Head Office Circular (প্রধান কার্যালয়ের সার্কুলার ও অফিস আদেশ)
                item {
                    SectionHeader(
                        title = "হেড অফিস সার্কুলার (HEAD OFFICE CIRCULAR)",
                        icon = Icons.Default.Campaign,
                        color = MonicoBlue
                    )
                    if (grid3Menus.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(grid3Menus) { item ->
                                MenuItemCard(
                                    item = item,
                                    currentEmpId = currentUser?.employeeId ?: "",
                                    onClick = {
                                        scope.launch { repository.markMenuSeen(item.id) }
                                        handleMenuCardClick(item, onOpenUrl, onOpenImageViewer)
                                    }
                                )
                            }
                        }
                    } else {
                        EmptySectionNotice("হেড অফিস সার্কুলারের কোনো ফাইল নেই।")
                    }
                }

                // 8. Grid 4: Salary TA Bill (বেতন ও টিএ বিল ডকুমেন্টস)
                item {
                    SectionHeader(
                        title = "স্যালারি ও টিএ বিল (SALARY TA BILL)",
                        icon = Icons.Default.Payments,
                        color = MonicoPurple
                    )
                    if (grid4Menus.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(grid4Menus) { item ->
                                MenuItemCard(
                                    item = item,
                                    currentEmpId = currentUser?.employeeId ?: "",
                                    onClick = {
                                        scope.launch { repository.markMenuSeen(item.id) }
                                        handleMenuCardClick(item, onOpenUrl, onOpenImageViewer)
                                    }
                                )
                            }
                        }
                    } else {
                        EmptySectionNotice("স্যালারি ও টিএ বিলের কোনো ফাইল নেই।")
                    }
                }

                // 9. Grid 5: Sales Report (PDF) (সেলস ও অ্যানালিটিক্স রিপোর্ট)
                item {
                    SectionHeader(
                        title = "সেলস রিপোর্ট (SALES REPORT PDF)",
                        icon = Icons.Default.PictureAsPdf,
                        color = MonicoRed
                    )
                    if (grid5Menus.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(grid5Menus) { item ->
                                MenuItemCard(
                                    item = item,
                                    currentEmpId = currentUser?.employeeId ?: "",
                                    onClick = {
                                        scope.launch { repository.markMenuSeen(item.id) }
                                        handleMenuCardClick(item, onOpenUrl, onOpenImageViewer)
                                    }
                                )
                            }
                        }
                    } else {
                        EmptySectionNotice("সেলস রিপোর্টের কোনো PDF ফাইল নেই।")
                    }
                }

                // 10. Product Training Section
                item {
                    SectionHeader(
                        title = "প্রোডাক্ট ট্রেনিং (PRODUCT TRAINING)",
                        icon = Icons.Default.School,
                        color = MonicoPurple
                    )
                    if (trainingCards.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(trainingCards) { card ->
                                ContentCardItem(
                                    card = card,
                                    onClick = {
                                        val imgs = if (card.images.isNotEmpty()) card.images else listOf(card.image)
                                        if (card.url.isNotBlank() && card.url.startsWith("http")) {
                                            onOpenUrl(card.title, card.url)
                                        } else {
                                            onOpenImageViewer(card.title, imgs)
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        EmptySectionNotice("কোনো ট্রেনিং কার্ড পাওয়া যায়নি।")
                    }
                }

                // 11. New Product Launched Section
                item {
                    SectionHeader(
                        title = "নতুন পণ্য উদ্বোধন (NEW PRODUCT LAUNCHED)",
                        icon = Icons.Default.RocketLaunch,
                        color = MonicoGreen
                    )
                    if (productCards.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(productCards) { card ->
                                ContentCardItem(
                                    card = card,
                                    onClick = {
                                        val imgs = if (card.images.isNotEmpty()) card.images else listOf(card.image)
                                        if (card.url.isNotBlank() && card.url.startsWith("http")) {
                                            onOpenUrl(card.title, card.url)
                                        } else {
                                            onOpenImageViewer(card.title, imgs)
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        EmptySectionNotice("নতুন কোনো প্রোডাক্ট কার্ড নেই।")
                    }
                }

                // 12. Product wise Target Sheet Section
                item {
                    SectionHeader(
                        title = "টার্গেট শিট (PRODUCT TARGET SHEET)",
                        icon = Icons.Default.TrackChanges,
                        color = MonicoAmber
                    )
                    if (targetSheets.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(targetSheets) { card ->
                                ContentCardItem(
                                    card = card,
                                    onClick = {
                                        val imgs = if (card.images.isNotEmpty()) card.images else listOf(card.image)
                                        if (card.url.isNotBlank() && card.url.startsWith("http")) {
                                            onOpenUrl(card.title, card.url)
                                        } else {
                                            onOpenImageViewer(card.title, imgs)
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        EmptySectionNotice("কোনো টার্গেট শিট ফাইল নেই।")
                    }
                }

                // Footer
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "© 2026 Monico One Mobile Edition",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "ডিজাইন ও ডেভেলপমেন্ট: মোঃ সাজ্জাদ আহমেদ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }

    // Profile Dialog
    if (showProfileDialog && currentUser != null) {
        ProfileDialog(
            user = currentUser!!,
            onDismiss = { showProfileDialog = false },
            onUpdateProfile = { name, pass ->
                scope.launch {
                    repository.updateProfile(name, pass)
                    repository.syncAll()
                }
            },
            onLogout = {
                repository.logout()
            }
        )
    }

    // Popup Announcement Dialog
    activePopupNotice?.let { popup ->
        UserPopupDialog(
            popup = popup,
            onDismiss = { activePopupNotice = null }
        )
    }
}

@Composable
private fun EmptySectionNotice(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(12.dp),
            textAlign = TextAlign.Center
        )
    }
}

private fun handleMenuCardClick(
    item: MenuItem,
    onOpenUrl: (title: String, url: String) -> Unit,
    onOpenImageViewer: (title: String, images: List<String>) -> Unit
) {
    if (item.group == "grid1") {
        if (item.url.isNotBlank()) {
            onOpenUrl(item.title, item.url)
        }
    } else if (item.group == "grid5") {
        // PDF
        if (item.url.isNotBlank()) {
            onOpenUrl(item.title, item.url)
        }
    } else {
        // Image gallery
        val imgList = if (item.images.isNotEmpty()) item.images else if (item.url.isNotBlank()) listOf(item.url) else emptyList()
        if (imgList.isNotEmpty()) {
            onOpenImageViewer(item.title, imgList)
        } else if (item.url.isNotBlank()) {
            onOpenUrl(item.title, item.url)
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SliderCarousel(sliders: List<SliderItem>, onSliderClick: (SliderItem) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { sliders.size })

    // Auto-scroll effect
    LaunchedEffect(sliders.size) {
        while (true) {
            delay(4500)
            if (sliders.isNotEmpty()) {
                val next = (pagerState.currentPage + 1) % sliders.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val slide = sliders[page]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onSliderClick(slide) }
                ) {
                    AsyncImage(
                        model = slide.image,
                        contentDescription = slide.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xCC020617))
                                )
                            )
                    )
                    Text(
                        text = slide.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    )
                }
            }

            // Indicator dots
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(sliders.size) { idx ->
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == idx) 8.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (pagerState.currentPage == idx) MonicoBlue else Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

@Composable
private fun NoticeTicker(notices: List<NoticeItem>) {
    val combinedText = remember(notices) {
        notices.joinToString("   ★★★   ") { it.text }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MonicoAmber.copy(alpha = 0.15f),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Notice",
                        tint = MonicoAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "নোটিশ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MonicoAmber
                    )
                }
            }

            Text(
                text = combinedText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthFilterBar(
    selectedMonth: String,
    onMonthSelected: (String) -> Unit,
    activeRole: String
) {
    var expanded by remember { mutableStateOf(false) }
    val months = listOf(
        "ALL" to "সকল মাস (All Months 2026)",
        "January" to "জানুয়ারি (January)",
        "February" to "ফেব্রুয়ারি (February)",
        "March" to "মার্চ (March)",
        "April" to "এপ্রিল (April)",
        "May" to "মে (May)",
        "June" to "জুন (June)",
        "July" to "জুলাই (July)",
        "August" to "আগস্ট (August)",
        "September" to "সেপ্টেম্বর (September)",
        "October" to "অক্টোবর (October)",
        "November" to "নভেম্বর (November)",
        "December" to "ডিসেম্বর (December)"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        ) {
            OutlinedTextField(
                value = months.firstOrNull { it.first == selectedMonth }?.second ?: selectedMonth,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.menuAnchor().fillMaxWidth().height(46.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                months.forEach { (key, label) ->
                    DropdownMenuItem(
                        text = { Text(label, fontSize = 12.sp) },
                        onClick = {
                            onMonthSelected(key)
                            expanded = false
                        }
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MonicoBlue.copy(alpha = 0.12f),
            modifier = Modifier.height(46.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 14.dp)
            ) {
                Text(
                    text = "ROLE: $activeRole",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MonicoBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun PrimaryMenusGrid(
    menus: List<MenuItem>,
    currentEmpId: String,
    onMenuClick: (MenuItem) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        menus.chunked(3).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        PrimaryMenuItemCard(
                            item = item,
                            currentEmpId = currentEmpId,
                            onClick = { onMenuClick(item) }
                        )
                    }
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PrimaryMenuItemCard(
    item: MenuItem,
    currentEmpId: String,
    onClick: () -> Unit
) {
    val hasSeen = item.seenBy[currentEmpId] == true

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .height(105.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!hasSeen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MonicoRed)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(MonicoBlue, MonicoNavy)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconForName(item.icon),
                        contentDescription = item.title,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MenuItemCard(
    item: MenuItem,
    currentEmpId: String,
    onClick: () -> Unit
) {
    val hasSeen = item.seenBy[currentEmpId] == true
    val imageList = if (item.images.isNotEmpty()) item.images else if (item.url.isNotBlank() && !item.url.endsWith(".pdf", ignoreCase = true)) listOf(item.url) else emptyList()
    val isPdfGroup = item.group == "grid5" || item.icon.contains("pdf", ignoreCase = true) || item.url.endsWith(".pdf", ignoreCase = true)
    val isImageCard = imageList.isNotEmpty() && (imageList[0].startsWith("http") || imageList[0].startsWith("data:"))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .width(120.dp)
            .height(130.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isImageCard && !isPdfGroup) {
                AsyncImage(
                    model = imageList[0],
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xE6020617))
                            )
                        )
                )
            } else if (isPdfGroup) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFDC2626), Color(0xFF991B1B))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "PDF REPORT",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )
            }

            // Page count badge (for images)
            if (!isPdfGroup && imageList.size > 1) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xCC0F172A),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "${imageList.size} Pages",
                        fontSize = 8.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // Unread Red Dot
            if (!hasSeen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(MonicoRed)
                )
            }

            // Title, Month & Date at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isImageCard || isPdfGroup) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.month.isNotBlank() || item.date.isNotBlank()) {
                    val dateLabel = if (item.month.isNotBlank()) "${item.month.take(3)}-26" else item.date
                    Text(
                        text = dateLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isImageCard || isPdfGroup) Color(0xFFFFD54F) else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentCardItem(
    card: ContentCard,
    onClick: () -> Unit
) {
    val imageList = if (card.images.isNotEmpty()) card.images else if (card.image.isNotBlank()) listOf(card.image) else emptyList()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .width(120.dp)
            .height(130.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageList.isNotEmpty()) {
                AsyncImage(
                    model = imageList[0],
                    contentDescription = card.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xE6020617))
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(MonicoPurple, MonicoNavy)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            if (imageList.size > 1) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xCC0F172A),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "${imageList.size} Pages",
                        fontSize = 8.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = card.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}

private fun getIconForName(iconName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        iconName.contains("cart", ignoreCase = true) || iconName.contains("order", ignoreCase = true) -> Icons.Default.ShoppingCart
        iconName.contains("dcc", ignoreCase = true) || iconName.contains("check", ignoreCase = true) -> Icons.Default.CheckCircle
        iconName.contains("calendar", ignoreCase = true) || iconName.contains("date", ignoreCase = true) -> Icons.Default.CalendarMonth
        iconName.contains("money", ignoreCase = true) || iconName.contains("bill", ignoreCase = true) -> Icons.Default.AttachMoney
        iconName.contains("user", ignoreCase = true) || iconName.contains("profile", ignoreCase = true) -> Icons.Default.Person
        iconName.contains("chart", ignoreCase = true) || iconName.contains("report", ignoreCase = true) -> Icons.Default.BarChart
        iconName.contains("pdf", ignoreCase = true) -> Icons.Default.PictureAsPdf
        iconName.contains("bullhorn", ignoreCase = true) || iconName.contains("bell", ignoreCase = true) -> Icons.Default.Campaign
        iconName.contains("truck", ignoreCase = true) || iconName.contains("delivery", ignoreCase = true) -> Icons.Default.LocalShipping
        iconName.contains("route", ignoreCase = true) || iconName.contains("tour", ignoreCase = true) -> Icons.Default.NearMe
        else -> Icons.Default.Folder
    }
}
