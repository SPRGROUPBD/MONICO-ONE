package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.data.repository.MonicoRepository
import com.example.ui.theme.MonicoAmber
import com.example.ui.theme.MonicoBlue
import com.example.ui.theme.MonicoGreen
import com.example.ui.theme.MonicoRed
import com.example.utils.ImageUtils
import com.example.utils.PdfUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    repository: MonicoRepository,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("মেনু ফাইল (Grids)", "সাইডবার মেনু", "স্লাইডার", "নোটিশ", "পপআপ", "প্রোডাক্ট/ট্রেনিং", "ইউজার", "নোটিফিকেশন")

    val menus by repository.menus.collectAsState()
    val sidebarMenus by repository.sidebarMenus.collectAsState()
    val sliders by repository.sliders.collectAsState()
    val notices by repository.notices.collectAsState()
    val popups by repository.popups.collectAsState()
    val productCards by repository.productCards.collectAsState()
    val trainingCards by repository.trainingCards.collectAsState()
    val targetSheets by repository.targetSheets.collectAsState()
    val allUsers by repository.allUsers.collectAsState()
    val notifications by repository.notifications.collectAsState()
    val isSyncing by repository.isSyncing.collectAsState()

    // Dialog control states
    var editingMenuItem by remember { mutableStateOf<MenuItem?>(null) }
    var showMenuDialog by remember { mutableStateOf(false) }

    var editingSidebarItem by remember { mutableStateOf<SidebarMenuItem?>(null) }
    var showSidebarDialog by remember { mutableStateOf(false) }

    var editingSliderItem by remember { mutableStateOf<SliderItem?>(null) }
    var showSliderDialog by remember { mutableStateOf(false) }

    var editingNoticeItem by remember { mutableStateOf<NoticeItem?>(null) }
    var showNoticeDialog by remember { mutableStateOf(false) }

    var editingPopupItem by remember { mutableStateOf<PopupNotice?>(null) }
    var showPopupDialog by remember { mutableStateOf(false) }

    var editingCardPath by remember { mutableStateOf("product_launch_cards") }
    var editingCardItem by remember { mutableStateOf<ContentCard?>(null) }
    var showCardDialog by remember { mutableStateOf(false) }

    var editingUserItem by remember { mutableStateOf<User?>(null) }
    var showUserDialog by remember { mutableStateOf(false) }

    var showSendNotificationDialog by remember { mutableStateOf(false) }

    // Delete confirmation state
    var itemToDelete by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Monico One অ্যাডমিন কনসোল", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("All File Add • Edit • Delete • Image & PDF Upload", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                repository.syncAll()
                                snackbarHostState.showSnackbar("ডাটাবেজ সিঙ্ক সম্পন্ন হয়েছে।")
                            }
                        },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> { editingMenuItem = null; showMenuDialog = true }
                        1 -> { editingSidebarItem = null; showSidebarDialog = true }
                        2 -> { editingSliderItem = null; showSliderDialog = true }
                        3 -> { editingNoticeItem = null; showNoticeDialog = true }
                        4 -> { editingPopupItem = null; showPopupDialog = true }
                        5 -> { editingCardItem = null; showCardDialog = true }
                        6 -> { editingUserItem = null; showUserDialog = true }
                        7 -> { showSendNotificationDialog = true }
                    }
                },
                containerColor = MonicoBlue,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item")
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        modifier = Modifier.fillMaxSize().testTag("admin_panel_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)) {
                when (selectedTab) {
                    0 -> AdminMenusTab(
                        menus = menus,
                        onAddNew = {
                            editingMenuItem = null
                            showMenuDialog = true
                        },
                        onEdit = { m ->
                            editingMenuItem = m
                            showMenuDialog = true
                        },
                        onDelete = { id ->
                            itemToDelete = "এই মেনু/ফাইলটি মুছে ফেলতে চান?" to {
                                scope.launch {
                                    repository.deleteMenuItem(id)
                                    snackbarHostState.showSnackbar("ফাইলটি মুছে ফেলা হয়েছে।")
                                }
                            }
                        },
                        onToggleStatus = { m ->
                            val newStatus = if (m.status == "active") "inactive" else "active"
                            scope.launch {
                                repository.saveMenuItem(m.copy(status = newStatus))
                                snackbarHostState.showSnackbar("স্ট্যাটাস আপডেট করা হয়েছে।")
                            }
                        }
                    )
                    1 -> AdminSidebarMenusTab(
                        sidebarMenus = sidebarMenus,
                        onEdit = { sm ->
                            editingSidebarItem = sm
                            showSidebarDialog = true
                        },
                        onDelete = { id ->
                            itemToDelete = "এই সাইডবার মেনুটি ডিলিট করতে চান?" to {
                                scope.launch {
                                    repository.deleteSidebarMenu(id)
                                    snackbarHostState.showSnackbar("সাইডবার মেনু মুছে ফেলা হয়েছে।")
                                }
                            }
                        },
                        onMoveUp = { index ->
                            if (index > 0) {
                                val currentList = sidebarMenus.toMutableList()
                                val item = currentList.removeAt(index)
                                currentList.add(index - 1, item)
                                scope.launch {
                                    repository.reorderSidebarMenus(currentList)
                                    snackbarHostState.showSnackbar("সাইডবার মেনু উপরে নেওয়া হয়েছে।")
                                }
                            }
                        },
                        onMoveDown = { index ->
                            if (index < sidebarMenus.size - 1) {
                                val currentList = sidebarMenus.toMutableList()
                                val item = currentList.removeAt(index)
                                currentList.add(index + 1, item)
                                scope.launch {
                                    repository.reorderSidebarMenus(currentList)
                                    snackbarHostState.showSnackbar("সাইডবার মেনু নিচে নেওয়া হয়েছে।")
                                }
                            }
                        }
                    )
                    2 -> AdminSlidersTab(
                        sliders = sliders,
                        onEdit = { s ->
                            editingSliderItem = s
                            showSliderDialog = true
                        },
                        onDelete = { id ->
                            itemToDelete = "স্লাইডারটি মুছে ফেলতে চান?" to {
                                scope.launch {
                                    repository.deleteSlider(id)
                                    snackbarHostState.showSnackbar("স্লাইডার মুছে ফেলা হয়েছে।")
                                }
                            }
                        }
                    )
                    3 -> AdminNoticesTab(
                        notices = notices,
                        onEdit = { n ->
                            editingNoticeItem = n
                            showNoticeDialog = true
                        },
                        onDelete = { id ->
                            itemToDelete = "নোটিশটি মুছে ফেলতে চান?" to {
                                scope.launch {
                                    repository.deleteNotice(id)
                                    snackbarHostState.showSnackbar("নোটিশ মুছে ফেলা হয়েছে।")
                                }
                            }
                        }
                    )
                    4 -> AdminPopupsTab(
                        popups = popups,
                        onAddNew = {
                            editingPopupItem = null
                            showPopupDialog = true
                        },
                        onEdit = { p ->
                            editingPopupItem = p
                            showPopupDialog = true
                        },
                        onDelete = { id ->
                            itemToDelete = "পপআপ নোটিশটি মুছে ফেলতে চান?" to {
                                scope.launch {
                                    repository.deletePopup(id)
                                    snackbarHostState.showSnackbar("পপআপ মুছে ফেলা হয়েছে।")
                                }
                            }
                        },
                        onToggleStatus = { p ->
                            val newStatus = if (p.status.equals("active", ignoreCase = true)) "off" else "active"
                            scope.launch {
                                repository.savePopup(p.copy(status = newStatus))
                                snackbarHostState.showSnackbar("পপআপ স্ট্যাটাস: " + if (newStatus == "active") "Active (চালু)" else "Off (বন্ধ)")
                            }
                        }
                    )
                    5 -> AdminCardsTab(
                        products = productCards,
                        trainings = trainingCards,
                        targets = targetSheets,
                        onEdit = { path, card ->
                            editingCardPath = path
                            editingCardItem = card
                            showCardDialog = true
                        },
                        onDelete = { path, id ->
                            itemToDelete = "এই কার্ডটি মুছে ফেলতে চান?" to {
                                scope.launch {
                                    repository.deleteCard(path, id)
                                    snackbarHostState.showSnackbar("কার্ড মুছে ফেলা হয়েছে।")
                                }
                            }
                        }
                    )
                    6 -> AdminUsersTab(
                        users = allUsers,
                        onEdit = { u ->
                            editingUserItem = u
                            showUserDialog = true
                        },
                        onDelete = { id ->
                            itemToDelete = "ইউজার $id মুছে ফেলতে চান?" to {
                                scope.launch {
                                    repository.deleteUser(id)
                                    snackbarHostState.showSnackbar("ইউজার মুছে ফেলা হয়েছে।")
                                }
                            }
                        }
                    )
                    7 -> AdminNotificationsTab(
                        notifications = notifications,
                        onAddNew = { showSendNotificationDialog = true },
                        onDelete = { id ->
                            itemToDelete = "এই নোটিফিকেশনটি মুছে ফেলতে চান?" to {
                                scope.launch {
                                    repository.deleteNotification(id)
                                    snackbarHostState.showSnackbar("নোটিফিকেশন মুছে ফেলা হয়েছে।")
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { (message, onConfirm) ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("ডিলিট নিশ্চিতকরণ", fontWeight = FontWeight.Bold) },
            text = { Text(message) },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm()
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MonicoRed)
                ) {
                    Text("ডিলিট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("বাতিল") }
            }
        )
    }

    // 1. Menu Dialog (Add / Edit) with Image and PDF Upload from device
    if (showMenuDialog) {
        MenuEditDialog(
            item = editingMenuItem,
            onDismiss = { showMenuDialog = false },
            onSave = { updatedItem ->
                scope.launch {
                    repository.saveMenuItem(updatedItem)
                    snackbarHostState.showSnackbar(if (editingMenuItem == null) "নতুন ফাইল যোগ করা হয়েছে।" else "ফাইল আপডেট করা হয়েছে।")
                    showMenuDialog = false
                }
            }
        )
    }

    // 2. Sidebar Menu Dialog
    if (showSidebarDialog) {
        SidebarEditDialog(
            item = editingSidebarItem,
            onDismiss = { showSidebarDialog = false },
            onSave = { updatedItem ->
                scope.launch {
                    repository.saveSidebarMenu(updatedItem)
                    snackbarHostState.showSnackbar("সাইডবার মেনু সংরক্ষিত হয়েছে।")
                    showSidebarDialog = false
                }
            }
        )
    }

    // 3. Slider Dialog with Image Picker
    if (showSliderDialog) {
        SliderEditDialog(
            item = editingSliderItem,
            onDismiss = { showSliderDialog = false },
            onSave = { updatedItem ->
                scope.launch {
                    repository.saveSlider(updatedItem)
                    snackbarHostState.showSnackbar("স্লাইডার সংরক্ষিত হয়েছে।")
                    showSliderDialog = false
                }
            }
        )
    }

    // 4. Notice Dialog
    if (showNoticeDialog) {
        NoticeEditDialog(
            item = editingNoticeItem,
            onDismiss = { showNoticeDialog = false },
            onSave = { text, id ->
                scope.launch {
                    repository.saveNotice(text, id)
                    snackbarHostState.showSnackbar("নোটিশ সংরক্ষিত হয়েছে।")
                    showNoticeDialog = false
                }
            }
        )
    }

    // 5. Popup Dialog with Image Picker
    if (showPopupDialog) {
        PopupEditDialog(
            item = editingPopupItem,
            onDismiss = { showPopupDialog = false },
            onSave = { updatedItem ->
                scope.launch {
                    repository.savePopup(updatedItem)
                    snackbarHostState.showSnackbar("পপআপ নোটিশ সংরক্ষিত হয়েছে।")
                    showPopupDialog = false
                }
            }
        )
    }

    // 6. Card Dialog (Product, Training, Target) with Image and PDF Picker
    if (showCardDialog) {
        CardEditDialog(
            initialPath = editingCardPath,
            item = editingCardItem,
            onDismiss = { showCardDialog = false },
            onSave = { path, updatedCard ->
                scope.launch {
                    repository.saveCard(path, updatedCard)
                    snackbarHostState.showSnackbar("কার্ড সংরক্ষিত হয়েছে।")
                    showCardDialog = false
                }
            }
        )
    }

    // 7. User Dialog with Photo Picker
    if (showUserDialog) {
        UserEditDialog(
            item = editingUserItem,
            onDismiss = { showUserDialog = false },
            onSave = { updatedUser ->
                scope.launch {
                    repository.saveUser(updatedUser)
                    snackbarHostState.showSnackbar("ইউজার সংরক্ষিত হয়েছে।")
                    showUserDialog = false
                }
            }
        )
    }

    // 8. Notification Broadcast Dialog (Super Admin)
    if (showSendNotificationDialog) {
        SendNotificationDialog(
            onDismiss = { showSendNotificationDialog = false },
            onSend = { title, message, targetRole, type, targetUrl ->
                scope.launch {
                    val success = repository.sendNotification(
                        title = title,
                        message = message,
                        targetRole = targetRole,
                        type = type,
                        senderName = "Super Admin",
                        targetUrl = targetUrl
                    )
                    showSendNotificationDialog = false
                    if (success) {
                        snackbarHostState.showSnackbar("সকল ইউজারের কাছে নোটিফিকেশন সফলভাবে পাঠানো হয়েছে।")
                    } else {
                        snackbarHostState.showSnackbar("নোটিফিকেশন সেভ হয়েছে।")
                    }
                }
            }
        )
    }
}

// -------------------------------------------------------------
// SEPARATE MODULAR DIALOGS TO PREVENT STALE STATE & CRASHES
// -------------------------------------------------------------

@Composable
private fun MenuEditDialog(
    item: MenuItem?,
    onDismiss: () -> Unit,
    onSave: (MenuItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val gridOptions = listOf(
        "grid1" to "GRID 1: Primary Menus (Daily Work)",
        "grid2" to "GRID 2: Flat Bonus Sheet",
        "grid3" to "GRID 3: Head Office Circular",
        "grid4" to "GRID 4: Salary TA Bill",
        "grid5" to "GRID 5: Sales Report (PDF)"
    )
    val monthOptions = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val roleOptions = listOf(
        "ALL" to "Everyone (MPO, AM, TM, RSM, DA, ADIC, DIC, Super Admin)",
        "SUPERVISOR" to "Supervisor (AM, TM, RSM)",
        "MPO" to "MPO (MPO-এর নির্ধারিত Feature)",
        "DA" to "DA (Delivery Assistant Feature)",
        "SUPER_ADMIN" to "Super Admin (সম্পূর্ণ নিয়ন্ত্রণ)",
        "PALL+SALL" to "Everyone (PALL+SALL)",
        "DIC" to "DIC Only"
    )

    var selectedGrid by remember { mutableStateOf(item?.group?.ifBlank { "grid2" } ?: "grid2") }
    var gridMenuOpen by remember { mutableStateOf(false) }

    var selectedMonth by remember { mutableStateOf(item?.month?.ifBlank { "March" } ?: "March") }
    var monthMenuOpen by remember { mutableStateOf(false) }

    val initialRoleSet = remember(item) {
        val r = item?.allowedRole?.trim() ?: "ALL"
        when {
            r.isBlank() || r.equals("ALL", true) || r.equals("EVERYONE", true) || r.equals("PALL+SALL", true) -> {
                setOf("SUPER_ADMIN", "MPO", "AM", "TM", "RSM", "DA", "ADIC", "DIC")
            }
            r.equals("SUPERVISOR", true) -> {
                setOf("AM", "TM", "RSM")
            }
            else -> {
                r.split(",", "+").map { it.trim().uppercase() }.filter { it.isNotBlank() }.toSet()
            }
        }
    }
    var selectedRoleSet by remember { mutableStateOf(initialRoleSet) }

    var title by remember { mutableStateOf(item?.title ?: "") }
    var dateStr by remember { mutableStateOf(item?.date?.ifBlank { "2026-03-25" } ?: "2026-03-25") }
    var icon by remember { mutableStateOf(item?.icon ?: "fa-folder") }
    var isPinned by remember { mutableStateOf(item?.isPinned ?: false) }

    // External URL (clean, only if not base64)
    var externalUrl by remember {
        mutableStateOf(if (item?.url?.startsWith("data:") == true) "" else item?.url ?: "")
    }

    // Attached PDF data (from device or existing item)
    var attachedPdfData by remember {
        mutableStateOf(if (item?.url?.let { PdfUtils.isPdf(it) } == true) item.url else "")
    }
    var attachedPdfName by remember {
        mutableStateOf(if (attachedPdfData.isNotBlank()) "সংযুক্ত PDF ডকুমেন্ট" else "")
    }

    // Device Images (up to 5)
    var deviceImages by remember {
        mutableStateOf(item?.images?.filter { it.isNotBlank() && !PdfUtils.isPdf(it) } ?: emptyList())
    }

    var isProcessingFile by remember { mutableStateOf(false) }

    // Launchers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isProcessingFile = true
                val newImgs = mutableListOf<String>()
                uris.take(5).forEach { uri ->
                    val b64 = ImageUtils.uriToBase64(context, uri)
                    if (b64 != null) newImgs.add(b64)
                }
                deviceImages = (deviceImages + newImgs).take(5)
                isProcessingFile = false
            }
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isProcessingFile = true
                val res = PdfUtils.readPdfUriToBase64(context, it)
                if (res != null) {
                    attachedPdfData = res.first
                    val kb = res.second / 1024
                    attachedPdfName = "ডিভাইস থেকে PDF ($kb KB)"
                }
                isProcessingFile = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (item == null) "নতুন মেনু / ফাইল যোগ করুন" else "মেনু / ফাইল এডিট করুন", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Grid Dropdown
                Text("টার্গেট গ্রিড নির্বাচন করুন (Grid):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { gridMenuOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            gridOptions.firstOrNull { it.first == selectedGrid }?.second ?: selectedGrid,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = gridMenuOpen,
                        onDismissRequest = { gridMenuOpen = false }
                    ) {
                        gridOptions.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label, fontSize = 12.sp, fontWeight = if (selectedGrid == key) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedGrid = key
                                    gridMenuOpen = false
                                }
                            )
                        }
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("ফাইলের নাম / শিরোনাম") },
                    placeholder = { Text("যেমন: মার্চ ২০২৬ ফ্ল্যাট বোনাস শিট") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedGrid == "grid1") {
                    OutlinedTextField(
                        value = icon,
                        onValueChange = { icon = it },
                        label = { Text("আইকন (যেমন: fa-cart-shopping, fa-check)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = externalUrl,
                        onValueChange = { externalUrl = it },
                        label = { Text("লিংক বা কমান্ড (যেমন: https://... বা go:dcc)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Month Dropdown
                    Text("মাস (Month):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { monthMenuOpen = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedMonth, fontSize = 12.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = monthMenuOpen,
                            onDismissRequest = { monthMenuOpen = false }
                        ) {
                            monthOptions.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m, fontSize = 12.sp) },
                                    onClick = {
                                        selectedMonth = m
                                        monthMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    // Date
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("তারিখ (যেমন: 2026-03-25)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // URL INPUT SECTION (PDF & Image URL - No device upload / base64 required)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("ফাইল / ইমেজ URL লিংক:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                            // PDF URL or Main Web Link
                            OutlinedTextField(
                                value = externalUrl,
                                onValueChange = { externalUrl = it },
                                label = { Text("PDF বা ফাইল URL লিংক (Direct Link)") },
                                placeholder = { Text("https://example.com/document.pdf") },
                                leadingIcon = {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MonicoRed, modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Image URLs (Multiple image URLs separated by comma or new line)
                            var imageUrlInput by remember { mutableStateOf("") }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = imageUrlInput,
                                    onValueChange = { imageUrlInput = it },
                                    label = { Text("ছবি URL যোগ করুন (Image URL)") },
                                    placeholder = { Text("https://example.com/photo.jpg") },
                                    leadingIcon = {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MonicoBlue, modifier = Modifier.size(18.dp))
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val trimmed = imageUrlInput.trim()
                                        if (trimmed.isNotBlank() && !deviceImages.contains(trimmed)) {
                                            deviceImages = (deviceImages + trimmed).take(10)
                                            imageUrlInput = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text("যোগ", fontSize = 11.sp)
                                }
                            }

                            if (deviceImages.isNotEmpty()) {
                                Text("সংযুক্ত ছবির লিংকসমূহ (${deviceImages.size}টি):", fontSize = 10.sp, color = MonicoGreen, fontWeight = FontWeight.Bold)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(deviceImages) { imgUrl ->
                                        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))) {
                                            AsyncImage(
                                                model = ImageUtils.resolveImageModel(imgUrl),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            IconButton(
                                                onClick = { deviceImages = deviceImages - imgUrl },
                                                modifier = Modifier.size(20.dp).align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.6f))
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Pinned checkbox
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPinned, onCheckedChange = { isPinned = it })
                        Text("এই ফাইলটি পিন করুন (Fast Access Pinned)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Role & User Group Configuration Section
                Text("Role & User Group Access Level:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Quick presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    selectedRoleSet = setOf("SUPER_ADMIN", "MPO", "AM", "TM", "RSM", "DA", "ADIC", "DIC")
                                },
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Everyone (সবাই)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = {
                                    selectedRoleSet = setOf("AM", "TM", "RSM")
                                },
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Supervisor", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = { selectedRoleSet = emptySet() },
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Clear", fontSize = 10.sp)
                            }
                        }

                        // Supervisor group with AM, TM, RSM
                        val isSupervisorAll = selectedRoleSet.containsAll(listOf("AM", "TM", "RSM"))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isSupervisorAll,
                                        onCheckedChange = { checked ->
                                            selectedRoleSet = if (checked) {
                                                selectedRoleSet + setOf("AM", "TM", "RSM")
                                            } else {
                                                selectedRoleSet - setOf("AM", "TM", "RSM")
                                            }
                                        }
                                    )
                                    Text("Supervisor (AM, TM, RSM)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MonicoBlue)
                                }
                                Row(
                                    modifier = Modifier.padding(start = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("AM", "TM", "RSM").forEach { sRole ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = selectedRoleSet.contains(sRole),
                                                onCheckedChange = { checked ->
                                                    selectedRoleSet = if (checked) selectedRoleSet + sRole else selectedRoleSet - sRole
                                                },
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(sRole, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Individual roles: MPO, DA, ADIC, DIC, Super Admin
                        listOf(
                            "MPO" to "MPO (Medical Promotion Officer)",
                            "DA" to "DA (Delivery Assistant)",
                            "ADIC" to "ADIC (Assistant Depot In-Charge)",
                            "DIC" to "DIC (Depot In-Charge)",
                            "SUPER_ADMIN" to "Super Admin (সম্পূর্ণ নিয়ন্ত্রণ)"
                        ).forEach { (rKey, rLabel) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedRoleSet = if (selectedRoleSet.contains(rKey)) {
                                            selectedRoleSet - rKey
                                        } else {
                                            selectedRoleSet + rKey
                                        }
                                    }
                            ) {
                                Checkbox(
                                    checked = selectedRoleSet.contains(rKey),
                                    onCheckedChange = { checked ->
                                        selectedRoleSet = if (checked) selectedRoleSet + rKey else selectedRoleSet - rKey
                                    }
                                )
                                Text(rLabel, fontSize = 11.sp, fontWeight = if (selectedRoleSet.contains(rKey)) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        // Compute final URL: if externalUrl is non-blank, use externalUrl; else if deviceImages has images, use first image; else fallback to existing attachedPdfData
                        val finalUrl = when {
                            externalUrl.isNotBlank() -> externalUrl.trim()
                            deviceImages.isNotEmpty() -> deviceImages[0].trim()
                            attachedPdfData.isNotBlank() -> attachedPdfData.trim()
                            else -> ""
                        }
                        val finalIcon = if (selectedGrid == "grid5" || PdfUtils.isPdf(finalUrl)) "fa-file-pdf" else if (deviceImages.isNotEmpty()) deviceImages[0].trim() else icon

                        val finalRole = when {
                            selectedRoleSet.isEmpty() -> "ALL"
                            selectedRoleSet.containsAll(listOf("MPO", "AM", "TM", "RSM", "DA", "ADIC", "DIC", "SUPER_ADMIN")) -> "ALL"
                            selectedRoleSet.containsAll(listOf("AM", "TM", "RSM")) && selectedRoleSet.size == 3 -> "SUPERVISOR"
                            else -> selectedRoleSet.joinToString(",")
                        }

                        onSave(
                            MenuItem(
                                id = item?.id ?: "",
                                title = title,
                                group = selectedGrid,
                                icon = finalIcon,
                                url = finalUrl,
                                images = if (selectedGrid != "grid1") deviceImages else emptyList(),
                                month = if (selectedGrid == "grid1") "" else selectedMonth,
                                date = if (selectedGrid == "grid1") "" else dateStr,
                                isPinned = isPinned,
                                allowedRole = finalRole,
                                status = item?.status ?: "active"
                            )
                        )
                    }
                }
            ) { Text(if (item == null) "সংরক্ষণ করুন" else "আপডেট করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun SidebarEditDialog(
    item: SidebarMenuItem?,
    onDismiss: () -> Unit,
    onSave: (SidebarMenuItem) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var url by remember { mutableStateOf(item?.url ?: "https://") }
    var icon by remember { mutableStateOf(item?.icon ?: "fa-globe") }
    var allowedRole by remember { mutableStateOf(item?.allowedRole ?: "SUPER_ADMIN,DIC") }
    var status by remember { mutableStateOf(item?.status ?: "active") }
    var orderText by remember { mutableStateOf((item?.order ?: 0).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "নতুন সাইডবার মেনু যোগ করুন" else "সাইডবার মেনু এডিট করুন", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("মেনুর নাম (যেমন: ERP Portal)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("লিংক URL (https://...)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("আইকন (যেমন: fa-globe, fa-chart-line)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = allowedRole,
                    onValueChange = { allowedRole = it },
                    label = { Text("অনুমোদিত রোল (SUPER_ADMIN,DIC)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = orderText,
                    onValueChange = { orderText = it },
                    label = { Text("ক্রমিক নম্বর / অর্ডার (0, 1, 2...)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && url.isNotBlank()) {
                        onSave(
                            SidebarMenuItem(
                                id = item?.id ?: "",
                                name = name,
                                url = url,
                                icon = icon,
                                allowedRole = allowedRole,
                                status = status,
                                order = orderText.toIntOrNull() ?: (item?.order ?: 0)
                            )
                        )
                    }
                }
            ) { Text(if (item == null) "যোগ করুন" else "আপডেট করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun SliderEditDialog(
    item: SliderItem?,
    onDismiss: () -> Unit,
    onSave: (SliderItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(item?.title ?: "") }
    var image by remember { mutableStateOf(item?.image ?: "") }
    var isProcessing by remember { mutableStateOf(false) }

    val sliderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isProcessing = true
                val base64 = ImageUtils.uriToBase64(context, it)
                if (base64 != null) image = base64
                isProcessing = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "নতুন স্লাইডার ব্যানার যোগ করুন" else "স্লাইডার ব্যানার এডিট করুন", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("স্লাইডার শিরোনাম") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = image,
                    onValueChange = { image = it },
                    label = { Text("ছবির সরাসরি লিংক (Image URL)") },
                    placeholder = { Text("https://example.com/banner.jpg") },
                    leadingIcon = {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MonicoBlue)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (image.isNotBlank()) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(10.dp))) {
                        AsyncImage(
                            model = ImageUtils.resolveImageModel(image),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && image.isNotBlank()) {
                        onSave(SliderItem(id = item?.id ?: "", title = title, image = image))
                    }
                }
            ) { Text(if (item == null) "যোগ করুন" else "আপডেট করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun NoticeEditDialog(
    item: NoticeItem?,
    onDismiss: () -> Unit,
    onSave: (text: String, id: String) -> Unit
) {
    var noticeText by remember { mutableStateOf(item?.text ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "নতুন নোটিশ যোগ করুন" else "নোটিশ এডিট করুন", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = noticeText,
                onValueChange = { noticeText = it },
                label = { Text("নোটিশের বিস্তারিত...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (noticeText.isNotBlank()) {
                        onSave(noticeText, item?.id ?: "")
                    }
                }
            ) { Text(if (item == null) "যোগ করুন" else "আপডেট করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun PopupEditDialog(
    item: PopupNotice?,
    onDismiss: () -> Unit,
    onSave: (PopupNotice) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(item?.title ?: "") }
    var body by remember { mutableStateOf(item?.body ?: "") }
    var type by remember { mutableStateOf(item?.type ?: "general") }
    var status by remember { mutableStateOf(item?.status ?: "active") }
    var image by remember { mutableStateOf(item?.image ?: "") }
    var isProcessing by remember { mutableStateOf(false) }

    val popupPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isProcessing = true
                val base64 = ImageUtils.uriToBase64(context, it)
                if (base64 != null) image = base64
                isProcessing = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "নতুন পপআপ নোটিশ যোগ করুন" else "পপআপ নোটিশ এডিট করুন", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("পপআপ শিরোনাম") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("পপআপ বিস্তারিত বার্তা") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Status Selection: Active vs Off
                Text("পপআপ স্ট্যাটাস (Status):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = status.equals("active", ignoreCase = true),
                        onClick = { status = "active" },
                        label = { Text("Active (চালু)") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (status.equals("active", ignoreCase = true)) MonicoGreen else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MonicoGreen.copy(alpha = 0.2f),
                            selectedLabelColor = MonicoGreen
                        )
                    )
                    FilterChip(
                        selected = !status.equals("active", ignoreCase = true),
                        onClick = { status = "off" },
                        label = { Text("Off (বন্ধ)") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (!status.equals("active", ignoreCase = true)) MonicoRed else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MonicoRed.copy(alpha = 0.2f),
                            selectedLabelColor = MonicoRed
                        )
                    )
                }
                Text(
                    text = if (status.equals("active", ignoreCase = true))
                        "স্ট্যাটাস: Active হলে সকল ব্যবহারকারী অ্যাপ ওপেন করলে পপআপটি দেখতে পাবে।"
                    else
                        "স্ট্যাটাস: Off হলে পপআপটি কারো অ্যাপে প্রদর্শিত হবে না।",
                    fontSize = 11.sp,
                    color = if (status.equals("active", ignoreCase = true)) MonicoGreen else MonicoRed,
                    fontWeight = FontWeight.Medium
                )

                OutlinedTextField(
                    value = image,
                    onValueChange = { image = it },
                    label = { Text("পপআপ ছবির সরাসরি লিংক (Image URL)") },
                    placeholder = { Text("https://example.com/popup.jpg") },
                    leadingIcon = {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MonicoBlue)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (image.isNotBlank()) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp))) {
                        AsyncImage(
                            model = ImageUtils.resolveImageModel(image),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { image = "" },
                            modifier = Modifier.size(24.dp).align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            PopupNotice(
                                id = item?.id ?: "",
                                title = title,
                                body = body,
                                status = status,
                                type = type,
                                image = image
                            )
                        )
                    }
                }
            ) { Text(if (item == null) "যোগ করুন" else "আপডেট করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun CardEditDialog(
    initialPath: String,
    item: ContentCard?,
    onDismiss: () -> Unit,
    onSave: (path: String, ContentCard) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var cardPath by remember { mutableStateOf(initialPath) }
    val cardPathOptions = listOf(
        "product_launch_cards" to "নতুন পণ্য (New Product Launch)",
        "product_training_cards" to "প্রোডাক্ট ট্রেনিং (Product Training)",
        "product_target_sheets" to "টার্গেট শিট (Target Sheet)"
    )
    var cardPathMenuOpen by remember { mutableStateOf(false) }

    var cardTitle by remember { mutableStateOf(item?.title ?: "") }
    var cardUrl by remember { mutableStateOf(item?.url ?: "") }
    var cardImages by remember {
        mutableStateOf(item?.images?.filter { it.isNotBlank() } ?: if (item?.image.isNullOrBlank()) emptyList() else listOf(item!!.image))
    }
    var isProcessing by remember { mutableStateOf(false) }

    val cardPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isProcessing = true
                val newImgs = mutableListOf<String>()
                uris.take(5).forEach { uri ->
                    val b64 = ImageUtils.uriToBase64(context, uri)
                    if (b64 != null) newImgs.add(b64)
                }
                cardImages = (cardImages + newImgs).take(5)
                isProcessing = false
            }
        }
    }

    val cardPdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isProcessing = true
                val res = PdfUtils.readPdfUriToBase64(context, it)
                if (res != null) {
                    cardImages = listOf(res.first)
                }
                isProcessing = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "নতুন কার্ড যোগ করুন" else "কার্ড এডিট করুন", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("কার্ডের ধরন নির্বাচন করুন:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { cardPathMenuOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(cardPathOptions.firstOrNull { it.first == cardPath }?.second ?: cardPath, fontSize = 12.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = cardPathMenuOpen,
                        onDismissRequest = { cardPathMenuOpen = false }
                    ) {
                        cardPathOptions.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label, fontSize = 12.sp) },
                                onClick = {
                                    cardPath = key
                                    cardPathMenuOpen = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = cardTitle,
                    onValueChange = { cardTitle = it },
                    label = { Text("কার্ডের নাম / প্রোডাক্ট নাম") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Direct PDF / Image URL inputs
                var urlInputText by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = urlInputText,
                        onValueChange = { urlInputText = it },
                        label = { Text("ছবি বা PDF লিংক যোগ করুন (URL)") },
                        placeholder = { Text("https://example.com/item.jpg বা .pdf") },
                        leadingIcon = {
                            Icon(Icons.Default.AddLink, contentDescription = null, tint = MonicoBlue)
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val trimmed = urlInputText.trim()
                            if (trimmed.isNotBlank() && !cardImages.contains(trimmed)) {
                                cardImages = cardImages + trimmed
                                urlInputText = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("যোগ", fontSize = 11.sp)
                    }
                }

                if (cardImages.isNotEmpty()) {
                    Text("সংযুক্ত পেজ (${cardImages.size}টি):", fontSize = 10.sp, color = MonicoGreen, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(cardImages) { itemData ->
                            Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))) {
                                if (PdfUtils.isPdf(itemData)) {
                                    Surface(
                                        color = MonicoRed,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
                                        }
                                    }
                                } else {
                                    AsyncImage(
                                        model = ImageUtils.resolveImageModel(itemData),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                IconButton(
                                    onClick = { cardImages = cardImages - itemData },
                                    modifier = Modifier.size(20.dp).align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.6f))
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = cardUrl,
                    onValueChange = { cardUrl = it },
                    label = { Text("ওয়েব লিংক (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (cardTitle.isNotBlank()) {
                        onSave(
                            cardPath,
                            ContentCard(
                                id = item?.id ?: "",
                                title = cardTitle,
                                url = cardUrl,
                                images = cardImages,
                                image = cardImages.firstOrNull() ?: ""
                            )
                        )
                    }
                }
            ) { Text(if (item == null) "যোগ করুন" else "আপডেট করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun UserEditDialog(
    item: User?,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var empId by remember { mutableStateOf(item?.employeeId ?: "") }
    var fullName by remember { mutableStateOf(item?.fullName ?: "") }
    var password by remember { mutableStateOf(item?.password ?: "") }
    var role by remember { mutableStateOf(item?.role ?: "MPO") }
    var profilePic by remember { mutableStateOf(item?.profilePic ?: "") }
    var isProcessing by remember { mutableStateOf(false) }

    val userPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isProcessing = true
                val base64 = ImageUtils.uriToBase64(context, it, maxWidth = 400, quality = 60)
                if (base64 != null) profilePic = base64
                isProcessing = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "নতুন ইউজার তৈরি করুন" else "ইউজার এডিট করুন", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = empId,
                    onValueChange = { empId = it.uppercase() },
                    label = { Text("এমপ্লয়ি আইডি (যেমন: MPO-02)") },
                    enabled = (item == null),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("পূর্ণ নাম") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("পাসওয়ার্ড") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it.uppercase() },
                    label = { Text("নির্বাচিত রোল (Selected Role)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("রোল নির্বাচন করুন (Role Selection):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                val allUserRoles = listOf(
                    "MPO" to "MPO",
                    "AM" to "AM",
                    "TM" to "TM",
                    "RSM" to "RSM",
                    "DA" to "DA",
                    "ADIC" to "ADIC",
                    "DIC" to "DIC",
                    "SUPER_ADMIN" to "Super Admin"
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    allUserRoles.chunked(4).forEach { rowList ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            rowList.forEach { (rCode, rName) ->
                                FilterChip(
                                    selected = role == rCode,
                                    onClick = { role = rCode },
                                    label = { Text(rName, fontSize = 10.sp, fontWeight = if (role == rCode) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Profile Picture Options for Super Admin
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "প্রোফাইল ছবি (URL অথবা ডিভাইস থেকে):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Option 1: URL input (Recommended for minimal DB storage)
                        OutlinedTextField(
                            value = profilePic,
                            onValueChange = { profilePic = it.trim() },
                            label = { Text("ছবির সরাসরি লিংক (URL - ডেটাবেজ সেভিং)") },
                            placeholder = { Text("https://example.com/photo.jpg") },
                            leadingIcon = {
                                Icon(Icons.Default.Link, contentDescription = null, tint = MonicoGreen)
                            },
                            trailingIcon = {
                                if (profilePic.isNotBlank()) {
                                    IconButton(onClick = { profilePic = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "অথবা ডিভাইস থেকে আপলোড করতে চাইলে:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Option 2: Device upload button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { userPhotoPicker.launch("image/*") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp), tint = MonicoGreen)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ডিভাইস থেকে নির্বাচন করুন", fontSize = 11.sp)
                            }
                            if (isProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }

                        // Preview indicator
                        if (profilePic.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Box(modifier = Modifier.size(48.dp).clip(CircleShape).border(2.dp, MonicoGreen, CircleShape)) {
                                    AsyncImage(
                                        model = ImageUtils.resolveImageModel(profilePic),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (profilePic.startsWith("data:image")) "ডিভাইস থেকে ছবি লোড হয়েছে (Base64)" else "ওয়েব লিংক যুক্ত হয়েছে (URL)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MonicoGreen
                                    )
                                    Text(
                                        text = if (profilePic.startsWith("data:image")) "ডাটাবেজে Base64 হিসেবে সেভ হবে" else "সরাসরি URL ব্যবহার হবে (কম ডেটা খরচ)",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (empId.isNotBlank() && fullName.isNotBlank() && password.isNotBlank()) {
                        onSave(
                            User(
                                employeeId = empId,
                                fullName = fullName,
                                password = password,
                                role = role,
                                profilePic = profilePic
                            )
                        )
                    }
                }
            ) { Text(if (item == null) "সেভ করুন" else "আপডেট করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

// -------------------------------------------------------------
// TABS CONTENT
// -------------------------------------------------------------

data class AdminGridCategory(
    val id: String,
    val title: String,
    val shortName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

private val adminGridCategories = listOf(
    AdminGridCategory("grid1", "Grid 1: Daily Report & Orders (দৈনিক কাজ)", "Grid 1 (Daily)", Icons.Default.Work, MonicoBlue),
    AdminGridCategory("grid2", "Grid 2: Flat Bonus Sheet (বোনাস শিট)", "Grid 2 (Bonus)", Icons.Default.CardGiftcard, MonicoGreen),
    AdminGridCategory("grid3", "Grid 3: Head Office Circular (সার্কুলার)", "Grid 3 (Circular)", Icons.Default.Campaign, MonicoAmber),
    AdminGridCategory("grid4", "Grid 4: Salary & TA Bill (বেতন ও বিল)", "Grid 4 (Salary TA)", Icons.Default.Payments, Color(0xFF9333EA)),
    AdminGridCategory("grid5", "Grid 5: Sales Report PDF (সেলস রিপোর্ট)", "Grid 5 (Sales PDF)", Icons.Default.PictureAsPdf, MonicoRed),
    AdminGridCategory("grid6", "Grid 6: Product & Target Sheet (টার্গেট শিট)", "Grid 6 (Target)", Icons.Default.School, Color(0xFF00897B))
)

@Composable
private fun AdminMenusTab(
    menus: List<MenuItem>,
    onAddNew: () -> Unit,
    onEdit: (MenuItem) -> Unit,
    onDelete: (String) -> Unit,
    onToggleStatus: (MenuItem) -> Unit
) {
    var selectedGridFilter by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(menus, selectedGridFilter, searchQuery) {
        menus.filter { m ->
            val matchesGrid = if (selectedGridFilter == "all") true else m.group == selectedGridFilter
            val matchesQuery = if (searchQuery.isBlank()) true else {
                m.title.contains(searchQuery, ignoreCase = true) ||
                m.month.contains(searchQuery, ignoreCase = true) ||
                m.date.contains(searchQuery, ignoreCase = true) ||
                m.allowedRole.contains(searchQuery, ignoreCase = true)
            }
            matchesGrid && matchesQuery
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Action & Search Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "গ্রিডভিত্তিক মেনু ফাইল (Grid Wise Files)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            "মোট ফাইল: ${menus.size}টি | ফিল্টার করা: ${filteredList.size}টি",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onAddNew,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("নতুন ফাইল", fontSize = 12.sp)
                    }
                }

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ফাইলের নাম, মাস বা তারিখ খুঁজুন...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                )

                // Grid Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedGridFilter == "all",
                            onClick = { selectedGridFilter = "all" },
                            label = { Text("সকল গ্রিড (${menus.size})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MonicoBlue.copy(alpha = 0.2f),
                                selectedLabelColor = MonicoBlue
                            )
                        )
                    }
                    items(adminGridCategories) { cat ->
                        val count = menus.count { it.group == cat.id }
                        FilterChip(
                            selected = selectedGridFilter == cat.id,
                            onClick = { selectedGridFilter = cat.id },
                            label = { Text("${cat.shortName} ($count)", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(cat.icon, contentDescription = null, tint = cat.color, modifier = Modifier.size(14.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cat.color.copy(alpha = 0.2f),
                                selectedLabelColor = cat.color
                            )
                        )
                    }
                }
            }
        }

        // Content List
        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Text("কোনো ফাইল পাওয়া যায়নি।", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onAddNew, shape = RoundedCornerShape(10.dp)) {
                        Text("নতুন ফাইল যোগ করুন")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedGridFilter == "all") {
                    // Grouped view by Grid
                    adminGridCategories.forEach { cat ->
                        val itemsInGrid = filteredList.filter { it.group == cat.id }
                        if (itemsInGrid.isNotEmpty()) {
                            item(key = "header_${cat.id}") {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = cat.color.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(cat.icon, contentDescription = null, tint = cat.color, modifier = Modifier.size(18.dp))
                                            Text(cat.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = cat.color)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = cat.color,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                "${itemsInGrid.size}টি",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            items(itemsInGrid, key = { it.id }) { m ->
                                MenuItemAdminCard(m, cat.color, onToggleStatus, onEdit, onDelete)
                            }
                        }
                    }

                    // Any orphan items not in known categories
                    val orphanItems = filteredList.filter { m -> adminGridCategories.none { it.id == m.group } }
                    if (orphanItems.isNotEmpty()) {
                        item(key = "header_others") {
                            Text("অন্যান্য ফাইল (${orphanItems.size}টি):", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                        }
                        items(orphanItems, key = { it.id }) { m ->
                            MenuItemAdminCard(m, Color.Gray, onToggleStatus, onEdit, onDelete)
                        }
                    }
                } else {
                    // Single Grid Selected View
                    val currentCat = adminGridCategories.firstOrNull { it.id == selectedGridFilter }
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = (currentCat?.color ?: MonicoBlue).copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(currentCat?.icon ?: Icons.Default.Folder, contentDescription = null, tint = currentCat?.color ?: MonicoBlue)
                                    Column {
                                        Text(currentCat?.title ?: selectedGridFilter, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("এই গ্রিডে মোট ${filteredList.size}টি ফাইল সংরক্ষিত আছে", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    items(filteredList, key = { it.id }) { m ->
                        MenuItemAdminCard(m, currentCat?.color ?: MonicoBlue, onToggleStatus, onEdit, onDelete)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItemAdminCard(
    m: MenuItem,
    gridColor: Color,
    onToggleStatus: (MenuItem) -> Unit,
    onEdit: (MenuItem) -> Unit,
    onDelete: (String) -> Unit
) {
    val isPdf = m.group == "grid5" || PdfUtils.isPdf(m.url)
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isPdf) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MonicoRed.copy(alpha = 0.15f),
                            contentColor = MonicoRed
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = MonicoRed, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("PDF", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MonicoBlue.copy(alpha = 0.15f),
                            contentColor = MonicoBlue
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Image, contentDescription = "IMG", tint = MonicoBlue, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("IMG", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(m.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = gridColor.copy(alpha = 0.12f)
                    ) {
                        Text(m.group.uppercase(), fontSize = 9.sp, color = gridColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                    }
                    if (m.month.isNotBlank()) {
                        Text("মাস: ${m.month}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    if (m.date.isNotBlank()) {
                        Text("তারিখ: ${m.date}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "রোল: ${m.allowedRole}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "স্ট্যাটাস: " + if (m.status == "active") "Active" else "Inactive",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (m.status == "active") MonicoGreen else MonicoRed
                    )
                    if (m.isPinned) {
                        Text("★ Pinned", fontSize = 9.sp, color = MonicoAmber, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                IconButton(onClick = { onToggleStatus(m) }) {
                    Icon(
                        imageVector = if (m.status == "active") Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = "Toggle status",
                        tint = if (m.status == "active") MonicoGreen else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = { onEdit(m) }) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MonicoBlue, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { onDelete(m.id) }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MonicoRed, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun AdminSidebarMenusTab(
    sidebarMenus: List<SidebarMenuItem>,
    onEdit: (SidebarMenuItem) -> Unit,
    onDelete: (String) -> Unit,
    onMoveUp: (Int) -> Unit = {},
    onMoveDown: (Int) -> Unit = {}
) {
    if (sidebarMenus.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("কোনো সাইডবার মেনু নেই।", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(sidebarMenus, key = { _, it -> it.id }) { index, sm ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MonicoBlue.copy(alpha = 0.15f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MonicoBlue
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sm.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(sm.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                            Text("Role: ${sm.allowedRole} | Status: ${sm.status} | Order: ${sm.order}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onMoveUp(index) },
                            enabled = index > 0,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "উপরে নিন",
                                tint = if (index > 0) MonicoBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                        IconButton(
                            onClick = { onMoveDown(index) },
                            enabled = index < sidebarMenus.size - 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "নিচে নিন",
                                tint = if (index < sidebarMenus.size - 1) MonicoBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                        IconButton(
                            onClick = { onEdit(sm) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MonicoBlue)
                        }
                        IconButton(
                            onClick = { onDelete(sm.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MonicoRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSlidersTab(
    sliders: List<SliderItem>,
    onEdit: (SliderItem) -> Unit,
    onDelete: (String) -> Unit
) {
    if (sliders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("কোনো স্লাইডার নেই।", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sliders, key = { it.id }) { s ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp))) {
                            AsyncImage(
                                model = ImageUtils.resolveImageModel(s.image),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Text(s.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onEdit(s) }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MonicoBlue)
                        }
                        IconButton(onClick = { onDelete(s.id) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MonicoRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminNoticesTab(
    notices: List<NoticeItem>,
    onEdit: (NoticeItem) -> Unit,
    onDelete: (String) -> Unit
) {
    if (notices.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("কোনো নোটিশ নেই।", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notices, key = { it.id }) { n ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(n.text, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 3)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onEdit(n) }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MonicoBlue)
                        }
                        IconButton(onClick = { onDelete(n.id) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MonicoRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminPopupsTab(
    popups: List<PopupNotice>,
    onAddNew: () -> Unit,
    onEdit: (PopupNotice) -> Unit,
    onDelete: (String) -> Unit,
    onToggleStatus: (PopupNotice) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Information Banner & Add button
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MonicoAmber.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = MonicoAmber, modifier = Modifier.size(20.dp))
                        Text("পপআপ নোটিশ ব্যবস্থাপনা", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Button(
                        onClick = onAddNew,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("নতুন পপআপ", fontSize = 11.sp)
                    }
                }
                Text(
                    "স্ট্যাটাস 'Active' হলে যেকোনো ব্যবহারকারী অ্যাপ ওপেন করলেই পপআপ দেখতে পাবেন। 'Off' থাকলে পপআপটি কারো অ্যাপে প্রদর্শিত হবে না।",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        if (popups.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("কোনো পপআপ নোটিশ নেই।", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onAddNew, shape = RoundedCornerShape(10.dp)) {
                        Text("প্রথম পপআপ নোটিশ যুক্ত করুন")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(popups, key = { it.id }) { p ->
                    val isActive = p.status.equals("active", ignoreCase = true)
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (p.image.isNotBlank()) {
                                    Box(modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp))) {
                                        AsyncImage(
                                            model = ImageUtils.resolveImageModel(p.image),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(p.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (p.body.isNotBlank()) {
                                        Text(p.body, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isActive) MonicoGreen.copy(alpha = 0.15f) else MonicoRed.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                if (isActive) "🟢 Active (সবার অ্যাপে দেখাবে)" else "⚪ Off (বন্ধ)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isActive) MonicoGreen else MonicoRed,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text("Type: ${p.type}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                                IconButton(onClick = { onToggleStatus(p) }) {
                                    Icon(
                                        imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = "Toggle status",
                                        tint = if (isActive) MonicoGreen else Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                IconButton(onClick = { onEdit(p) }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MonicoBlue)
                                }
                                IconButton(onClick = { onDelete(p.id) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MonicoRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminCardsTab(
    products: List<ContentCard>,
    trainings: List<ContentCard>,
    targets: List<ContentCard>,
    onEdit: (path: String, card: ContentCard) -> Unit,
    onDelete: (path: String, id: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("নতুন পণ্য (Product Launch)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        }
        items(products, key = { "pl_${it.id}" }) { c ->
            CardRowItem(c) {
                Row {
                    IconButton(onClick = { onEdit("product_launch_cards", c) }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MonicoBlue)
                    }
                    IconButton(onClick = { onDelete("product_launch_cards", c.id) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MonicoRed)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("প্রোডাক্ট ট্রেনিং (Product Training)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        }
        items(trainings, key = { "pt_${it.id}" }) { c ->
            CardRowItem(c) {
                Row {
                    IconButton(onClick = { onEdit("product_training_cards", c) }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MonicoBlue)
                    }
                    IconButton(onClick = { onDelete("product_training_cards", c.id) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MonicoRed)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("টার্গেট শিট (Target Sheets)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        }
        items(targets, key = { "ts_${it.id}" }) { c ->
            CardRowItem(c) {
                Row {
                    IconButton(onClick = { onEdit("product_target_sheets", c) }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MonicoBlue)
                    }
                    IconButton(onClick = { onDelete("product_target_sheets", c.id) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MonicoRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun CardRowItem(card: ContentCard, actions: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                val preview = card.images.firstOrNull() ?: card.image
                Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp))) {
                    if (PdfUtils.isPdf(preview)) {
                        Surface(color = MonicoRed, modifier = Modifier.fillMaxSize()) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
                            }
                        }
                    } else if (preview.isNotBlank()) {
                        AsyncImage(
                            model = ImageUtils.resolveImageModel(preview),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxSize()) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Photo, contentDescription = null)
                            }
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(card.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("পেজ সংখ্যা: ${card.images.size}টি", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            actions()
        }
    }
}

@Composable
private fun AdminUsersTab(
    users: List<User>,
    onEdit: (User) -> Unit,
    onDelete: (String) -> Unit
) {
    if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("কোনো ইউজার তালিকা নেই।", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(users, key = { it.employeeId }) { u ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                            if (u.profilePic.isNotBlank()) {
                                AsyncImage(
                                    model = ImageUtils.resolveImageModel(u.profilePic),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(24.dp).align(Alignment.Center))
                            }
                        }
                        Column {
                            Text(u.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("ID: ${u.employeeId} • Role: ${u.role}", fontSize = 11.sp, color = MonicoBlue)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onEdit(u) }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MonicoBlue)
                        }
                        IconButton(onClick = { onDelete(u.employeeId) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MonicoRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminNotificationsTab(
    notifications: List<NotificationItem>,
    onAddNew: () -> Unit,
    onDelete: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Broadcast Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MonicoBlue.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(14.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MonicoBlue.copy(alpha = 0.3f))
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MonicoBlue,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "সকল ইউজারকে নোটিফিকেশন ব্রডকাস্ট",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "মোট প্রেরিত: ${notifications.size} টি নোটিফিকেশন",
                                fontSize = 11.5.sp,
                                color = MonicoBlue
                            )
                        }
                    }

                    Button(
                        onClick = onAddNew,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue),
                        modifier = Modifier.testTag("admin_send_new_notif_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("নতুন পাঠান", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "সুপারঅ্যাডমিন হিসেবে যেকোনো নোটিশ, সার্কুলার বা জরুরি ঘোষণা সকল ইউজারের কাছে তাৎক্ষণিকভাবে পাঠাতে পারেন। ইউজারের হেডার নোটিফিকেশন আইকনে পেন্ডিং সংখ্যা প্রদর্শন হবে এবং পঠিত হলে স্বয়ংক্রিয়ভাবে মুছে যাবে।",
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "কোনো নোটিফিকেশন এখনো প্রেরণ করা হয়নি।",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    val (typeColor, typeIcon, typeLabel) = when (notif.type.lowercase()) {
                        "urgent" -> Triple(MaterialTheme.colorScheme.error, Icons.Default.Warning, "জরুরি")
                        "update" -> Triple(MonicoAmber, Icons.Default.Campaign, "আপডেট")
                        "offer" -> Triple(MonicoGreen, Icons.Default.CheckCircle, "অফার")
                        "notice" -> Triple(MonicoBlue, Icons.Default.Campaign, "নোটিশ")
                        else -> Triple(MonicoBlue, Icons.Default.Info, "তথ্য")
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = typeColor.copy(alpha = 0.15f),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(imageVector = typeIcon, contentDescription = null, tint = typeColor, modifier = Modifier.size(11.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(typeLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = typeColor)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    ) {
                                        Text(
                                            text = "প্রাপক: ${if (notif.targetRole == "ALL") "সকল ইউজার (All)" else notif.targetRole}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onDelete(notif.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MonicoRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = notif.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (notif.message.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = notif.message,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "সময়: ${if (notif.dateFormatted.isNotBlank()) notif.dateFormatted else "সম্প্রতি"}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "প্রেরক: ${notif.senderName}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

