package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.utils.IconUtils
import com.example.utils.ImageUtils
import com.example.utils.PdfUtils
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
    onOpenAdminPanel: () -> Unit,
    initialShowNotifications: Boolean = false
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
    val notifications by repository.notifications.collectAsState()
    val lastSyncTime by repository.lastSyncTime.collectAsState()
    val isSyncing by repository.isSyncing.collectAsState()

    var showProfileDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(initialShowNotifications) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var activePopupNotice by remember { mutableStateOf<PopupNotice?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf("ALL") }

    var readNotificationIds by remember {
        mutableStateOf(repository.getReadNotificationIds(currentUser?.employeeId ?: ""))
    }

    LaunchedEffect(currentUser) {
        val emp = currentUser?.employeeId ?: ""
        readNotificationIds = repository.getReadNotificationIds(emp)
    }

    val userRole = (currentUser?.role ?: "MPO").trim().uppercase()
    val isSuperAdmin = userRole == "SUPER_ADMIN" || userRole == "SUPER ADMIN"
    val isDIC = userRole == "DIC"

    val unreadNotificationCount = remember(notifications, currentUser, readNotificationIds) {
        val emp = currentUser?.employeeId?.uppercase() ?: ""
        val role = userRole
        notifications.count { notif ->
            val matches = notif.targetRole.equals("ALL", ignoreCase = true) ||
                    notif.targetRole.contains(role, ignoreCase = true) ||
                    notif.targetRole.contains("PALL", ignoreCase = true)
            val isRead = readNotificationIds.contains(notif.id) || (notif.readBy[emp] == true)
            matches && !isRead
        }
    }

    // Initial popup display: Active hole sobar app open korle dekhte pabe, r Off hole dekha jabe na
    LaunchedEffect(popups) {
        val active = popups.firstOrNull { it.status.equals("active", ignoreCase = true) }
        if (active != null && activePopupNotice == null) {
            activePopupNotice = active
        }
    }

    // Role restriction: MPO, AM, RSM, TM, DA, ADIC cannot see or access sidemenu
    val canViewSidebar = isSuperAdmin || isDIC

    // Role & User Group Configuration:
    // 1. Super Admin: সম্পূর্ণ অ্যাপের নিয়ন্ত্রণ
    // 2. Everyone: MPO, AM, TM, RSM, DA, ADIC, DIC, Super Admin
    // 3. MPO: MPO-এর জন্য নির্ধারিত Feature ও Access
    // 4. Supervisor: AM, TM, RSM
    // 5. DA (Delivery Assistant): নির্ধারিত Delivery Assistant Feature
    val supervisorRoles = listOf("SUPERVISOR", "AM", "TM", "RSM")

    fun hasAccess(itemAllowedRole: String): Boolean {
        if (isSuperAdmin) return true
        val roleStr = itemAllowedRole.trim()
        if (roleStr.isBlank() ||
            roleStr.equals("ALL", ignoreCase = true) ||
            roleStr.equals("EVERYONE", ignoreCase = true) ||
            roleStr.equals("PALL+SALL", ignoreCase = true)) {
            return true
        }

        val tokens = roleStr.split(",", "+").map { it.trim().uppercase() }
        if (tokens.any { it == "ALL" || it == "EVERYONE" || it == "PALL+SALL" }) {
            return true
        }

        // Supervisor group
        val isUserSupervisor = userRole in supervisorRoles
        if (tokens.any { it == "SUPERVISOR" || it == "SUPERVISORS" } && isUserSupervisor) {
            return true
        }

        // MPO specific
        if (tokens.contains("MPO") && userRole == "MPO") {
            return true
        }

        // DA specific
        val isUserDA = userRole == "DA" || userRole == "DELIVERY_ASSISTANT" || userRole == "DELIVERY ASSISTANT"
        if (tokens.any { it == "DA" || it == "DELIVERY_ASSISTANT" || it == "DELIVERY ASSISTANT" } && isUserDA) {
            return true
        }

        // Legacy groups
        val primaryRoles = listOf("MPO", "AM", "RSM", "TM", "SUPER_ADMIN", "SUPERVISOR")
        val specialRoles = listOf("DIC", "ADIC", "DA", "SUPER_ADMIN")
        if (tokens.contains("PALL") && userRole in primaryRoles) return true
        if (tokens.contains("SALL") && userRole in specialRoles) return true

        return tokens.contains(userRole)
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

    // Z to A and 30-29-28-27 sorting helper
    val sortDescendingZtoA: (List<MenuItem>) -> List<MenuItem> = remember {
        { list ->
            list.sortedWith(Comparator { a, b ->
                if (a.isPinned != b.isPinned) {
                    return@Comparator if (a.isPinned) -1 else 1
                }
                val aDigits = a.date.filter { it.isDigit() }.toIntOrNull()
                val bDigits = b.date.filter { it.isDigit() }.toIntOrNull()
                if (aDigits != null && bDigits != null && aDigits != bDigits) {
                    return@Comparator bDigits.compareTo(aDigits) // 30, 29, 28, 27 descending
                }
                if (aDigits != null && bDigits == null) return@Comparator -1
                if (aDigits == null && bDigits != null) return@Comparator 1

                val titleCmp = b.title.compareTo(a.title, ignoreCase = true)
                if (titleCmp != 0) return@Comparator titleCmp

                b.id.compareTo(a.id)
            })
        }
    }

    val pinnedMenus = remember(filteredMenus) {
        sortDescendingZtoA(filteredMenus.filter { it.isPinned })
    }

    val grid1Menus = remember(filteredMenus) { sortDescendingZtoA(filteredMenus.filter { it.group == "grid1" || it.group == "grid-1" || it.group == "grid_1" || it.group.equals("primary", ignoreCase = true) }) }
    val grid2Menus = remember(filteredMenus) { sortDescendingZtoA(filteredMenus.filter { it.group == "grid2" }) }
    val grid3Menus = remember(filteredMenus) { sortDescendingZtoA(filteredMenus.filter { it.group == "grid3" }) }
    val grid4Menus = remember(filteredMenus) { sortDescendingZtoA(filteredMenus.filter { it.group == "grid4" }) }
    val grid5Menus = remember(filteredMenus) { sortDescendingZtoA(filteredMenus.filter { it.group == "grid5" }) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = canViewSidebar,
        drawerContent = {
            if (canViewSidebar) {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.width(300.dp)
                ) {
                    val drawerScrollState = rememberScrollState()
                    val activeSortedSidebarMenus = remember(sidebarMenus) {
                        sidebarMenus.filter { it.status == "active" }
                            .sortedWith(compareBy<SidebarMenuItem> { it.order }.thenBy { it.name })
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(drawerScrollState)
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

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MonicoGreen) },
                            label = { Text("অ্যাপ অনুমতি (Permissions)", fontWeight = FontWeight.SemiBold) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                showPermissionsDialog = true
                            }
                        )

                        Text("সাইডবার লিংক ও পোর্টাল", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        activeSortedSidebarMenus.forEach { sm ->
                            NavigationDrawerItem(
                                icon = {
                                    if (sm.icon.startsWith("http") || sm.icon.startsWith("data:")) {
                                        AsyncImage(
                                            model = ImageUtils.resolveImageModel(sm.icon),
                                            contentDescription = sm.name,
                                            modifier = Modifier.size(20.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Icon(IconUtils.getIconForName(sm.icon), contentDescription = null, tint = MonicoBlue)
                                    }
                                },
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

                        Spacer(modifier = Modifier.height(24.dp))
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
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        // Fixed clean 52dp compact Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (isSearchActive) {
                                // Full width search bar
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("মেনু বা ফাইল খুঁজুন...", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            searchQuery = ""
                                            isSearchActive = false
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close search", modifier = Modifier.size(18.dp))
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                )
                            } else {
                                // Left side: Hamburger (if admin) + Logo + User Info
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    if (canViewSidebar) {
                                        IconButton(
                                            onClick = { scope.launch { drawerState.open() } },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                                        }
                                    }

                                    Image(
                                        painter = painterResource(id = R.drawable.monico_one_icon_1791604475434),
                                        contentDescription = "Monico One",
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )

                                    // User Name and ID (Guaranteed single line, never wraps vertically)
                                    Column(
                                        modifier = Modifier.widthIn(max = 140.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = currentUser?.fullName ?: (currentUser?.employeeId ?: "Employee"),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            ),
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "ID: ${currentUser?.employeeId} • ${currentUser?.role}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MonicoBlue,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Right side: Compact Icon Buttons
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    IconButton(
                                        onClick = { isSearchActive = true },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(20.dp))
                                    }

                                    IconButton(
                                        onClick = { scope.launch { repository.syncAll() } },
                                        enabled = !isSyncing,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        if (isSyncing) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MonicoBlue)
                                        } else {
                                            Icon(Icons.Default.Sync, contentDescription = "Sync", tint = MonicoBlue, modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    IconButton(
                                        onClick = onToggleDarkMode,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = "Toggle theme",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Notification Icon with pending count badge (disappears when 0/read)
                                    IconButton(
                                        onClick = { showNotificationsDialog = true },
                                        modifier = Modifier.size(34.dp).testTag("header_notifications_button")
                                    ) {
                                        if (unreadNotificationCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(
                                                        containerColor = MaterialTheme.colorScheme.error,
                                                        contentColor = MaterialTheme.colorScheme.onError
                                                    ) {
                                                        Text(
                                                            text = if (unreadNotificationCount > 99) "99+" else "$unreadNotificationCount",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Notifications,
                                                    contentDescription = "Notifications",
                                                    tint = MonicoBlue,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.NotificationsNone,
                                                contentDescription = "Notifications",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    if (isSuperAdmin || isDIC) {
                                        IconButton(
                                            onClick = onOpenAdminPanel,
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Panel", tint = MonicoAmber, modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable { showProfileDialog = true }
                                    ) {
                                        val pic = currentUser?.profilePic ?: ""
                                        if (pic.isNotBlank()) {
                                            AsyncImage(
                                                model = ImageUtils.resolveImageModel(pic),
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
                                        handleMenuCardClick(item, context, onOpenUrl, onOpenImageViewer)
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
                                handleMenuCardClick(item, context, onOpenUrl, onOpenImageViewer)
                            }
                        )
                    }
                }

                // 6. Grid 2: Flat Bonus Sheet
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
                                        handleMenuCardClick(item, context, onOpenUrl, onOpenImageViewer)
                                    }
                                )
                            }
                        }
                    } else {
                        EmptySectionNotice("ফ্ল্যাট বোনাস শিটের কোনো ফাইল নেই।")
                    }
                }

                // 7. Grid 3: Head Office Circular
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
                                        handleMenuCardClick(item, context, onOpenUrl, onOpenImageViewer)
                                    }
                                )
                            }
                        }
                    } else {
                        EmptySectionNotice("হেড অফিস সার্কুলারের কোনো ফাইল নেই।")
                    }
                }

                // 8. Grid 4: Salary TA Bill
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
                                        handleMenuCardClick(item, context, onOpenUrl, onOpenImageViewer)
                                    }
                                )
                            }
                        }
                    } else {
                        EmptySectionNotice("স্যালারি ও টিএ বিলের কোনো ফাইল নেই।")
                    }
                }

                // 9. Grid 5: Sales Report (PDF)
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
                                        handleMenuCardClick(item, context, onOpenUrl, onOpenImageViewer)
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
                                    onClick = { handleContentCardClick(card, context, onOpenUrl, onOpenImageViewer) }
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
                                    onClick = { handleContentCardClick(card, context, onOpenUrl, onOpenImageViewer) }
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
                                    onClick = { handleContentCardClick(card, context, onOpenUrl, onOpenImageViewer) }
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
            onUpdateProfile = { name, pass, newPic ->
                scope.launch {
                    repository.updateProfile(name, pass, newPic)
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

    // Notifications Dialog
    if (showNotificationsDialog) {
        NotificationsDialog(
            currentUser = currentUser,
            notifications = notifications,
            readNotificationIds = readNotificationIds,
            onDismiss = { showNotificationsDialog = false },
            onMarkAsRead = { id ->
                val emp = currentUser?.employeeId ?: ""
                scope.launch {
                    repository.markNotificationAsRead(id, emp)
                    readNotificationIds = repository.getReadNotificationIds(emp)
                }
            },
            onMarkAllAsRead = {
                val emp = currentUser?.employeeId ?: ""
                scope.launch {
                    repository.markAllNotificationsAsRead(emp)
                    readNotificationIds = repository.getReadNotificationIds(emp)
                }
            },
            onSendNotification = { title, message, targetRole, type, targetUrl ->
                scope.launch {
                    repository.sendNotification(
                        title = title,
                        message = message,
                        targetRole = targetRole,
                        type = type,
                        senderName = currentUser?.fullName ?: "Super Admin",
                        targetUrl = targetUrl
                    )
                }
            },
            onOpenUrl = onOpenUrl
        )
    }
}

private fun isPdfFile(url: String): Boolean {
    return PdfUtils.isPdf(url)
}

private fun isImageFile(url: String): Boolean {
    if (url.isBlank()) return false
    if (url.startsWith("data:image", ignoreCase = true)) return true
    val lower = url.lowercase()
    return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.contains("image")
}

private fun isImageUrl(url: String): Boolean = isImageFile(url)

private fun handleMenuCardClick(
    item: MenuItem,
    context: Context,
    onOpenUrl: (title: String, url: String) -> Unit,
    onOpenImageViewer: (title: String, images: List<String>) -> Unit
) {
    // 1. GRID 1: PRIMARY MENUS (DAILY WORK) - STRICTLY URL/LINK IN WEBVIEW OR BROWSER
    val isGrid1 = item.group == "grid1" || item.group == "grid-1" || item.group == "grid_1" || 
        item.group.equals("primary", ignoreCase = true) || item.url.startsWith("go:") ||
        item.title.contains("order", ignoreCase = true) || item.title.contains("অর্ডার", ignoreCase = true)

    if (isGrid1) {
        val targetUrl = when {
            item.url.isNotBlank() -> item.url
            item.images.isNotEmpty() && !isImageFile(item.images.first()) -> item.images.first()
            else -> ""
        }
        val resolved = resolveGoUrl(targetUrl)
        if (resolved.isNotBlank()) {
            onOpenUrl(item.title, resolved)
        } else {
            Toast.makeText(context, "${item.title}: কোনো লিংক দেওয়া নেই", Toast.LENGTH_SHORT).show()
        }
        return
    }

    // 2. GRID 5: SALES REPORT - STRICTLY PDF ONLY
    if (item.group == "grid5") {
        val pdfUrl = when {
            item.url.isNotBlank() -> resolveGoUrl(item.url)
            item.images.isNotEmpty() -> item.images.first()
            else -> ""
        }
        if (pdfUrl.isNotBlank()) {
            if (pdfUrl.startsWith("http://") || pdfUrl.startsWith("https://")) {
                onOpenUrl(item.title, pdfUrl)
            } else {
                onOpenImageViewer(item.title, listOf(pdfUrl))
            }
        } else {
            Toast.makeText(context, "${item.title}: কোনো সেলস রিপোর্ট PDF যুক্ত নেই", Toast.LENGTH_SHORT).show()
        }
        return
    }

    // 3. IF URL IS A DIRECT WEB PORTAL / SYSTEM COMMAND AND NOT AN IMAGE
    val trimmedUrl = item.url.trim()
    if (trimmedUrl.isNotBlank() && !isPdfFile(trimmedUrl) && !isImageUrl(trimmedUrl) && 
        (trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://") || trimmedUrl.startsWith("go:"))) {
        onOpenUrl(item.title, resolveGoUrl(trimmedUrl))
        return
    }

    // 4. GRIDS 2, 3, 4: BONUS SHEETS, CIRCULARS, SALARY BILLS - IMAGES ONLY
    val allImages = mutableListOf<String>()
    item.images.forEach { img ->
        val trimmed = img.trim()
        if (trimmed.isNotBlank()) {
            if (!isImageUrl(trimmed) && (trimmed.startsWith("http://") || trimmed.startsWith("https://")) && !trimmed.contains("base64")) {
                // If a non-image URL ended up in images, open URL directly instead of failing in image viewer
                if (allImages.isEmpty() && item.images.size == 1) {
                    onOpenUrl(item.title, resolveGoUrl(trimmed))
                    return
                }
            } else {
                allImages.add(trimmed)
            }
        }
    }

    if (allImages.isEmpty()) {
        if (item.url.isNotBlank() && isImageUrl(item.url)) {
            val eff = resolveGoUrl(item.url)
            allImages.add(eff)
        }
    }
    if (allImages.isEmpty() && item.icon.isNotBlank() && (item.icon.startsWith("http") || item.icon.startsWith("data:"))) {
        allImages.add(item.icon)
    }

    if (allImages.isNotEmpty()) {
        onOpenImageViewer(item.title, allImages)
    } else {
        Toast.makeText(context, "${item.title}: কোনো ছবি যুক্ত নেই", Toast.LENGTH_SHORT).show()
    }
}

private fun resolveGoUrl(url: String): String {
    val clean = url.trim()
    return when (clean) {
        "go:order" -> "http://103.153.241.44/monicoapp"
        "go:Tour" -> "https://sprgroupbd.github.io/SmartManager/monicoone.html"
        "go:delivery" -> "https://sprgroupbd.github.io/SmartManager/depo_bill.html"
        "go:bill" -> "https://sprgroupbd.github.io/SmartManager/admin"
        "go:att1" -> "https://sprgroupbd.github.io/SmartManager/att.html"
        "go:dcc" -> "https://sprgroupbd.github.io/SmartManager/dcs.html"
        else -> {
            if (clean.startsWith("go:")) {
                "https://sprgroupbd.github.io/SmartManager/" + clean.removePrefix("go:").trim()
            } else {
                clean
            }
        }
    }
}

private fun handleContentCardClick(
    card: ContentCard,
    context: Context,
    onOpenUrl: (title: String, url: String) -> Unit,
    onOpenImageViewer: (title: String, images: List<String>) -> Unit
) {
    val validImgs = mutableListOf<String>()
    card.images.forEach { if (it.isNotBlank()) validImgs.add(it) }
    if (validImgs.isEmpty() && card.image.isNotBlank()) {
        validImgs.add(card.image)
    }

    if (validImgs.isNotEmpty()) {
        onOpenImageViewer(card.title, validImgs)
    } else if (card.url.isNotBlank()) {
        val u = card.url.trim().lowercase()
        if (u.endsWith(".png") || u.endsWith(".jpg") || u.endsWith(".jpeg") || u.endsWith(".webp") || u.startsWith("data:image")) {
            onOpenImageViewer(card.title, listOf(card.url.trim()))
        } else {
            onOpenUrl(card.title, card.url.trim())
        }
    } else {
        Toast.makeText(context, "${card.title}: কোনো ছবি বা লিংক যুক্ত নেই", Toast.LENGTH_SHORT).show()
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
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 500,
                        velocity = 40.dp
                    )
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
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .height(105.dp),
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
                    if (item.icon.startsWith("http") || item.icon.startsWith("data:")) {
                        AsyncImage(
                            model = ImageUtils.resolveImageModel(item.icon),
                            contentDescription = item.title,
                            modifier = Modifier.size(24.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = IconUtils.getIconForName(item.icon),
                            contentDescription = item.title,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
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
    val isGrid1 = item.group == "grid1" || item.group == "grid-1" || item.group == "grid_1" || item.group.equals("primary", ignoreCase = true) || item.url.startsWith("go:")
    val isPdf = item.group == "grid5"

    val imageList = remember(item, isPdf, isGrid1) {
        if (isPdf || isGrid1) {
            emptyList()
        } else {
            val list = mutableListOf<String>()
            item.images.forEach { if (it.isNotBlank()) list.add(it) }
            if (list.isEmpty()) {
                if (item.url.isNotBlank() && isImageUrl(item.url)) {
                    list.add(item.url)
                } else if (item.icon.isNotBlank() && (item.icon.startsWith("http") || item.icon.startsWith("data:"))) {
                    list.add(item.icon)
                }
            }
            list
        }
    }

    val isImageCard = !isPdf && !isGrid1
    val hasImagePreview = isImageCard && imageList.isNotEmpty()

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .width(120.dp)
            .height(130.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasImagePreview) {
                // Image preview with overlay
                AsyncImage(
                    model = ImageUtils.resolveImageModel(imageList[0]),
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
            } else if (isGrid1) {
                // Primary menu style with Icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(MonicoBlue, MonicoNavy)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (item.icon.startsWith("http") || item.icon.startsWith("data:")) {
                            AsyncImage(
                                model = ImageUtils.resolveImageModel(item.icon),
                                contentDescription = item.title,
                                modifier = Modifier.size(32.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(
                                imageVector = IconUtils.getIconForName(item.icon),
                                contentDescription = item.title,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "PRIMARY MENU",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            } else if (isPdf) {
                // PDF Card styling with PDF Icon
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
                            contentDescription = "PDF Icon",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
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
                // Image card placeholder with Image Icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF0D9488), Color(0xFF0F766E))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Image Icon",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "IMAGE FILE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Top-left type badge: Link icon on Grid 1, PDF icon on PDF cards, Image icon on Image cards
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xCC0F172A),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (isGrid1) {
                        Icon(
                            imageVector = getIconForName(item.icon),
                            contentDescription = "Link",
                            tint = Color(0xFF67E8F9),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "LINK",
                            fontSize = 8.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (isPdf) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "PDF",
                            fontSize = 8.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Image",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = if (imageList.size > 1) "${imageList.size} Pages" else "IMAGE",
                            fontSize = 8.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                        color = Color.White
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
                        color = Color(0xFFFFD54F)
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
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .width(120.dp)
            .height(130.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageList.isNotEmpty()) {
                AsyncImage(
                    model = ImageUtils.resolveImageModel(imageList[0]),
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "PRODUCT CARD",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Image badge in top-left
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xCC0F172A),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val isPdf = card.url.lowercase().endsWith(".pdf")
                    val isLink = card.url.isNotBlank() && imageList.isEmpty()
                    val badgeIcon = if (isPdf) Icons.Default.Description else if (isLink) Icons.Default.Link else Icons.Default.Image
                    val badgeTint = if (isPdf) Color(0xFFEF4444) else if (isLink) Color(0xFF10B981) else Color(0xFF38BDF8)
                    val badgeText = if (isPdf) "PDF" else if (isLink) "LINK" else if (imageList.size > 1) "${imageList.size} Pages" else "IMAGE"

                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = badgeText,
                        tint = badgeTint,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = badgeText,
                        fontSize = 8.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
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
    return IconUtils.getIconForName(iconName)
}
