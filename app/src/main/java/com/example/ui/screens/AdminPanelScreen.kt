package com.example.ui.screens

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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    repository: MonicoRepository,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("মেনু ফাইল (Grids)", "সাইডবার মেনু", "স্লাইডার", "নোটিশ", "পপআপ", "প্রোডাক্ট/ট্রেনিং", "ইউজার")

    val menus by repository.menus.collectAsState()
    val sidebarMenus by repository.sidebarMenus.collectAsState()
    val sliders by repository.sliders.collectAsState()
    val notices by repository.notices.collectAsState()
    val popups by repository.popups.collectAsState()
    val productCards by repository.productCards.collectAsState()
    val trainingCards by repository.trainingCards.collectAsState()
    val targetSheets by repository.targetSheets.collectAsState()
    val allUsers by repository.allUsers.collectAsState()

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

    // Delete confirmation state
    var itemToDelete by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Monico One অ্যাডমিন কনসোল", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("All File Add • Edit • Delete • Device Upload", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { repository.syncAll() } }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
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
                        0 -> {
                            editingMenuItem = null
                            showMenuDialog = true
                        }
                        1 -> {
                            editingSidebarItem = null
                            showSidebarDialog = true
                        }
                        2 -> {
                            editingSliderItem = null
                            showSliderDialog = true
                        }
                        3 -> {
                            editingNoticeItem = null
                            showNoticeDialog = true
                        }
                        4 -> {
                            editingPopupItem = null
                            showPopupDialog = true
                        }
                        5 -> {
                            editingCardItem = null
                            showCardDialog = true
                        }
                        6 -> {
                            editingUserItem = null
                            showUserDialog = true
                        }
                    }
                },
                containerColor = MonicoBlue,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item")
            }
        },
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
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> AdminMenusTab(
                        menus = menus,
                        onEdit = { m ->
                            editingMenuItem = m
                            showMenuDialog = true
                        },
                        onDelete = { id ->
                            itemToDelete = "এই মেনু/ফাইলটি মুছে ফেলতে চান?" to {
                                scope.launch { repository.deleteMenuItem(id) }
                            }
                        },
                        onToggleStatus = { m ->
                            val newStatus = if (m.status == "active") "inactive" else "active"
                            scope.launch { repository.saveMenuItem(m.copy(status = newStatus)) }
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
                                scope.launch { repository.deleteSidebarMenu(id) }
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
                                scope.launch { repository.deleteSlider(id) }
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
                                scope.launch { repository.deleteNotice(id) }
                            }
                        }
                    )
                    4 -> AdminPopupsTab(
                        popups = popups,
                        onEdit = { p ->
                            editingPopupItem = p
                            showPopupDialog = true
                        },
                        onDelete = { id ->
                            itemToDelete = "পপআপ নোটিশটি মুছে ফেলতে চান?" to {
                                scope.launch { repository.deletePopup(id) }
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
                                scope.launch { repository.deleteCard(path, id) }
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
                                scope.launch { repository.deleteUser(id) }
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

    // 1. Menu Dialog (Add / Edit) with Device Multi-Image Picker
    if (showMenuDialog) {
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
            "PALL+SALL" to "PALL+SALL (All Field Roles)",
            "ALL" to "ALL (Everyone)",
            "PALL" to "PALL (Primary: MPO, AM, RSM, TM)",
            "SALL" to "SALL (Special: DIC, ADIC, DA)",
            "MPO" to "MPO Only",
            "DIC" to "DIC Only",
            "SUPER_ADMIN" to "Super Admin Only"
        )

        var selectedGrid by remember { mutableStateOf(editingMenuItem?.group ?: "grid2") }
        var gridDropdownExpanded by remember { mutableStateOf(false) }

        var selectedMonth by remember { mutableStateOf(editingMenuItem?.month?.ifBlank { "March" } ?: "March") }
        var monthDropdownExpanded by remember { mutableStateOf(false) }

        var selectedRole by remember { mutableStateOf(editingMenuItem?.allowedRole?.ifBlank { "PALL+SALL" } ?: "PALL+SALL") }
        var roleDropdownExpanded by remember { mutableStateOf(false) }

        var title by remember { mutableStateOf(editingMenuItem?.title ?: "") }
        var dateStr by remember { mutableStateOf(editingMenuItem?.date?.ifBlank { "2026-03-25" } ?: "2026-03-25") }
        var icon by remember { mutableStateOf(editingMenuItem?.icon ?: "fa-folder") }
        var url by remember { mutableStateOf(editingMenuItem?.url ?: "") }
        var isPinned by remember { mutableStateOf(editingMenuItem?.isPinned ?: false) }

        // Multiple device images list
        var deviceImages by remember {
            mutableStateOf<List<String>>(editingMenuItem?.images ?: emptyList())
        }

        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents()
        ) { uris: List<Uri> ->
            val converted = uris.take(5).mapNotNull { uri ->
                ImageUtils.uriToBase64(context, uri)
            }
            if (converted.isNotEmpty()) {
                deviceImages = (deviceImages + converted).take(5)
            }
        }

        AlertDialog(
            onDismissRequest = { showMenuDialog = false },
            title = {
                Text(
                    if (editingMenuItem == null) "নতুন মেনু / ফাইল যোগ করুন" else "মেনু / ফাইল এডিট করুন",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Grid Selection Dropdown
                    Text("টার্গেট গ্রিড নির্বাচন করুন (Grid):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    ExposedDropdownMenuBox(
                        expanded = gridDropdownExpanded,
                        onExpandedChange = { gridDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = gridOptions.firstOrNull { it.first == selectedGrid }?.second ?: selectedGrid,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gridDropdownExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = gridDropdownExpanded,
                            onDismissRequest = { gridDropdownExpanded = false }
                        ) {
                            gridOptions.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontSize = 12.sp, fontWeight = if (selectedGrid == key) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedGrid = key
                                        gridDropdownExpanded = false
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
                            value = url,
                            onValueChange = { url = it },
                            label = { Text("লিংক বা কমান্ড (যেমন: https://... বা go:dcc)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Month Dropdown
                        Text("মাস (Month):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        ExposedDropdownMenuBox(
                            expanded = monthDropdownExpanded,
                            onExpandedChange = { monthDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedMonth,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthDropdownExpanded) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = monthDropdownExpanded,
                                onDismissRequest = { monthDropdownExpanded = false }
                            ) {
                                monthOptions.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m, fontSize = 12.sp) },
                                        onClick = {
                                            selectedMonth = m
                                            monthDropdownExpanded = false
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

                        if (selectedGrid == "grid5") {
                            // PDF Link
                            OutlinedTextField(
                                value = url,
                                onValueChange = { url = it },
                                label = { Text("PDF ফাইলের লিংক (URL)") },
                                placeholder = { Text("https://example.com/report.pdf") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            // Device Image Upload Section
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("ছবি আপলোড (Device থেকে সর্বোচ্চ ৫টি পেজ):", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                    Button(
                                        onClick = { photoPickerLauncher.launch("image/*") },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue)
                                    ) {
                                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("ডিভাইস থেকে ছবি বেছে নিন", fontSize = 12.sp)
                                    }

                                    // Preview thumbnails
                                    if (deviceImages.isNotEmpty()) {
                                        Text("নির্বাচিত পেজসমূহ (${deviceImages.size}টি):", fontSize = 10.sp, color = MonicoGreen, fontWeight = FontWeight.Bold)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(deviceImages) { imgBase64 ->
                                                Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))) {
                                                    AsyncImage(
                                                        model = imgBase64,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    IconButton(
                                                        onClick = { deviceImages = deviceImages - imgBase64 },
                                                        modifier = Modifier.size(20.dp).align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.6f))
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = url,
                                        onValueChange = { url = it },
                                        label = { Text("বা ইমেজ URL দিন (ঐচ্ছিক)") },
                                        placeholder = { Text("https://example.com/image.jpg") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Pinned checkbox
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isPinned, onCheckedChange = { isPinned = it })
                            Text("এই ফাইলটি পিন করুন (Fast Access Pinned)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Allowed Role Dropdown
                    Text("অনুমোদিত রোল (Allowed Role):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    ExposedDropdownMenuBox(
                        expanded = roleDropdownExpanded,
                        onExpandedChange = { roleDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = roleOptions.firstOrNull { it.first == selectedRole }?.second ?: selectedRole,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleDropdownExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = roleDropdownExpanded,
                            onDismissRequest = { roleDropdownExpanded = false }
                        ) {
                            roleOptions.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontSize = 12.sp) },
                                    onClick = {
                                        selectedRole = key
                                        roleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val finalImages = if (deviceImages.isNotEmpty()) deviceImages else if (url.isNotBlank() && !url.endsWith(".pdf", ignoreCase = true)) listOf(url) else emptyList()
                            val finalUrl = if (selectedGrid == "grid5") url else if (finalImages.isNotEmpty()) finalImages[0] else url

                            scope.launch {
                                repository.saveMenuItem(
                                    MenuItem(
                                        id = editingMenuItem?.id ?: "",
                                        title = title,
                                        group = selectedGrid,
                                        icon = if (selectedGrid == "grid5") "fa-file-pdf" else if (finalImages.isNotEmpty()) finalImages[0] else icon,
                                        url = finalUrl,
                                        images = if (selectedGrid != "grid1" && selectedGrid != "grid5") finalImages else emptyList(),
                                        month = if (selectedGrid == "grid1") "" else selectedMonth,
                                        date = if (selectedGrid == "grid1") "" else dateStr,
                                        isPinned = isPinned,
                                        allowedRole = selectedRole,
                                        status = editingMenuItem?.status ?: "active"
                                    )
                                )
                                showMenuDialog = false
                            }
                        }
                    }
                ) { Text(if (editingMenuItem == null) "সংরক্ষণ করুন" else "আপডেট করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showMenuDialog = false }) { Text("বাতিল") }
            }
        )
    }

    // 2. Sidebar Menu Dialog (Add / Edit)
    if (showSidebarDialog) {
        var name by remember { mutableStateOf(editingSidebarItem?.name ?: "") }
        var url by remember { mutableStateOf(editingSidebarItem?.url ?: "https://") }
        var icon by remember { mutableStateOf(editingSidebarItem?.icon ?: "fa-globe") }
        var allowedRole by remember { mutableStateOf(editingSidebarItem?.allowedRole ?: "SUPER_ADMIN,DIC") }
        var status by remember { mutableStateOf(editingSidebarItem?.status ?: "active") }

        AlertDialog(
            onDismissRequest = { showSidebarDialog = false },
            title = { Text(if (editingSidebarItem == null) "নতুন সাইডবার মেনু যোগ করুন" else "সাইডবার মেনু এডিট করুন", fontWeight = FontWeight.Bold) },
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && url.isNotBlank()) {
                            scope.launch {
                                repository.saveSidebarMenu(
                                    SidebarMenuItem(
                                        id = editingSidebarItem?.id ?: "",
                                        name = name,
                                        url = url,
                                        icon = icon,
                                        allowedRole = allowedRole,
                                        status = status
                                    )
                                )
                                showSidebarDialog = false
                            }
                        }
                    }
                ) { Text(if (editingSidebarItem == null) "যোগ করুন" else "আপডেট করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showSidebarDialog = false }) { Text("বাতিল") }
            }
        )
    }

    // 3. Slider Dialog (Add / Edit) with Device Image Picker
    if (showSliderDialog) {
        var title by remember { mutableStateOf(editingSliderItem?.title ?: "") }
        var image by remember { mutableStateOf(editingSliderItem?.image ?: "") }

        val sliderPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                val base64 = ImageUtils.uriToBase64(context, it)
                if (base64 != null) {
                    image = base64
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showSliderDialog = false },
            title = { Text(if (editingSliderItem == null) "নতুন স্লাইডার ব্যানার যোগ করুন" else "স্লাইডার ব্যানার এডিট করুন", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("স্লাইডার শিরোনাম") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { sliderPickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ডিভাইস থেকে স্লাইডার ছবি নিন")
                    }

                    if (image.isNotBlank()) {
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(10.dp))) {
                            AsyncImage(
                                model = image,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    OutlinedTextField(
                        value = image,
                        onValueChange = { image = it },
                        label = { Text("বা ছবির লিংক (URL)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && image.isNotBlank()) {
                            scope.launch {
                                repository.saveSlider(
                                    SliderItem(
                                        id = editingSliderItem?.id ?: "",
                                        title = title,
                                        image = image
                                    )
                                )
                                showSliderDialog = false
                            }
                        }
                    }
                ) { Text(if (editingSliderItem == null) "যোগ করুন" else "আপডেট করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showSliderDialog = false }) { Text("বাতিল") }
            }
        )
    }

    // 4. Notice Dialog (Add / Edit)
    if (showNoticeDialog) {
        var noticeText by remember { mutableStateOf(editingNoticeItem?.text ?: "") }

        AlertDialog(
            onDismissRequest = { showNoticeDialog = false },
            title = { Text(if (editingNoticeItem == null) "নতুন নোটিশ যোগ করুন" else "নোটিশ এডিট করুন", fontWeight = FontWeight.Bold) },
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
                            scope.launch {
                                repository.saveNotice(noticeText, editingNoticeItem?.id ?: "")
                                showNoticeDialog = false
                            }
                        }
                    }
                ) { Text(if (editingNoticeItem == null) "যোগ করুন" else "আপডেট করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showNoticeDialog = false }) { Text("বাতিল") }
            }
        )
    }

    // 5. Popup Dialog (Add / Edit) with Device Image Picker
    if (showPopupDialog) {
        var title by remember { mutableStateOf(editingPopupItem?.title ?: "") }
        var body by remember { mutableStateOf(editingPopupItem?.body ?: "") }
        var type by remember { mutableStateOf(editingPopupItem?.type ?: "general") }
        var status by remember { mutableStateOf(editingPopupItem?.status ?: "active") }
        var image by remember { mutableStateOf(editingPopupItem?.image ?: "") }

        val popupPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                val base64 = ImageUtils.uriToBase64(context, it)
                if (base64 != null) {
                    image = base64
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showPopupDialog = false },
            title = { Text(if (editingPopupItem == null) "নতুন পপআপ নোটিশ যোগ করুন" else "পপআপ নোটিশ এডিট করুন", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    Button(
                        onClick = { popupPickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ডিভাইস থেকে পপআপ ছবি নিন")
                    }

                    if (image.isNotBlank()) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp))) {
                            AsyncImage(
                                model = image,
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
                        if (title.isNotBlank()) {
                            scope.launch {
                                repository.savePopup(
                                    PopupNotice(
                                        id = editingPopupItem?.id ?: "",
                                        title = title,
                                        body = body,
                                        status = status,
                                        type = type,
                                        image = image
                                    )
                                )
                                showPopupDialog = false
                            }
                        }
                    }
                ) { Text(if (editingPopupItem == null) "যোগ করুন" else "আপডেট করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showPopupDialog = false }) { Text("বাতিল") }
            }
        )
    }

    // 6. Card Dialog (Product Launch, Training, Target) with Device Multi-Image Picker
    if (showCardDialog) {
        var cardPath by remember { mutableStateOf(editingCardPath) }
        val cardPathOptions = listOf(
            "product_launch_cards" to "নতুন পণ্য (New Product Launch)",
            "product_training_cards" to "প্রোডাক্ট ট্রেনিং (Product Training)",
            "product_target_sheets" to "টার্গেট শিট (Target Sheet)"
        )
        var cardPathExpanded by remember { mutableStateOf(false) }

        var cardTitle by remember { mutableStateOf(editingCardItem?.title ?: "") }
        var cardUrl by remember { mutableStateOf(editingCardItem?.url ?: "") }
        var cardImages by remember {
            mutableStateOf<List<String>>(editingCardItem?.images ?: if (editingCardItem?.image.isNullOrBlank()) emptyList() else listOf(editingCardItem!!.image))
        }

        val cardPhotoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents()
        ) { uris: List<Uri> ->
            val converted = uris.take(5).mapNotNull { uri ->
                ImageUtils.uriToBase64(context, uri)
            }
            if (converted.isNotEmpty()) {
                cardImages = (cardImages + converted).take(5)
            }
        }

        AlertDialog(
            onDismissRequest = { showCardDialog = false },
            title = { Text(if (editingCardItem == null) "নতুন কার্ড যোগ করুন" else "কার্ড এডিট করুন", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("কার্ডের ধরন নির্বাচন করুন:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    ExposedDropdownMenuBox(
                        expanded = cardPathExpanded,
                        onExpandedChange = { cardPathExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = cardPathOptions.firstOrNull { it.first == cardPath }?.second ?: cardPath,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardPathExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = cardPathExpanded,
                            onDismissRequest = { cardPathExpanded = false }
                        ) {
                            cardPathOptions.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontSize = 12.sp) },
                                    onClick = {
                                        cardPath = key
                                        cardPathExpanded = false
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

                    Button(
                        onClick = { cardPhotoPicker.launch("image/*") },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MonicoBlue)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ডিভাইস থেকে কার্ডের ছবি নিন")
                    }

                    if (cardImages.isNotEmpty()) {
                        Text("নির্বাচিত পেজসমূহ (${cardImages.size}টি):", fontSize = 10.sp, color = MonicoGreen, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(cardImages) { imgBase64 ->
                                Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))) {
                                    AsyncImage(
                                        model = imgBase64,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    IconButton(
                                        onClick = { cardImages = cardImages - imgBase64 },
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
                        label = { Text("লিংক (ঐচ্ছিক)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cardTitle.isNotBlank()) {
                            scope.launch {
                                repository.saveCard(
                                    cardPath,
                                    ContentCard(
                                        id = editingCardItem?.id ?: "",
                                        title = cardTitle,
                                        url = cardUrl,
                                        images = cardImages,
                                        image = cardImages.firstOrNull() ?: ""
                                    )
                                )
                                showCardDialog = false
                            }
                        }
                    }
                ) { Text(if (editingCardItem == null) "যোগ করুন" else "আপডেট করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showCardDialog = false }) { Text("বাতিল") }
            }
        )
    }

    // 7. User Dialog (Add / Edit) with Device Profile Pic Picker
    if (showUserDialog) {
        var empId by remember { mutableStateOf(editingUserItem?.employeeId ?: "") }
        var fullName by remember { mutableStateOf(editingUserItem?.fullName ?: "") }
        var password by remember { mutableStateOf(editingUserItem?.password ?: "") }
        var role by remember { mutableStateOf(editingUserItem?.role ?: "MPO") }
        var profilePic by remember { mutableStateOf(editingUserItem?.profilePic ?: "") }

        val userPhotoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                val base64 = ImageUtils.uriToBase64(context, it)
                if (base64 != null) {
                    profilePic = base64
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showUserDialog = false },
            title = { Text(if (editingUserItem == null) "নতুন ইউজার তৈরি করুন" else "ইউজার এডিট করুন", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = empId,
                        onValueChange = { empId = it.uppercase() },
                        label = { Text("এমপ্লয়ি আইডি (যেমন: MPO-02)") },
                        enabled = (editingUserItem == null),
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
                        label = { Text("রোল (MPO / AM / RSM / TM / DIC / SUPER_ADMIN)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { userPhotoPicker.launch("image/*") },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MonicoGreen)
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ডিভাইস থেকে প্রোফাইল ছবি নিন")
                    }

                    if (profilePic.isNotBlank()) {
                        Box(modifier = Modifier.size(60.dp).clip(CircleShape).border(2.dp, MonicoGreen, CircleShape)) {
                            AsyncImage(
                                model = profilePic,
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
                        if (empId.isNotBlank() && fullName.isNotBlank() && password.isNotBlank()) {
                            scope.launch {
                                repository.saveUser(
                                    User(
                                        employeeId = empId,
                                        fullName = fullName,
                                        password = password,
                                        role = role,
                                        profilePic = profilePic
                                    )
                                )
                                showUserDialog = false
                            }
                        }
                    }
                ) { Text(if (editingUserItem == null) "সেভ করুন" else "আপডেট করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showUserDialog = false }) { Text("বাতিল") }
            }
        )
    }
}

@Composable
private fun AdminMenusTab(
    menus: List<MenuItem>,
    onEdit: (MenuItem) -> Unit,
    onDelete: (String) -> Unit,
    onToggleStatus: (MenuItem) -> Unit
) {
    if (menus.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("কোনো মেনু ফাইল নেই।", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(menus) { m ->
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(m.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Group: ${m.group} | Month: ${m.month.ifEmpty { "N/A" }} | Date: ${m.date.ifEmpty { "N/A" }}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Role: ${m.allowedRole} | Status: ${m.status}",
                            fontSize = 10.sp,
                            color = if (m.status == "active") MonicoGreen else MonicoRed
                        )
                        if (m.isPinned) {
                            Text("Pinned to Fast Access", fontSize = 10.sp, color = MonicoAmber, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Toggle Status
                        IconButton(onClick = { onToggleStatus(m) }) {
                            Icon(
                                imageVector = if (m.status == "active") Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = "Toggle status",
                                tint = if (m.status == "active") MonicoGreen else Color.Gray
                            )
                        }
                        // Edit
                        IconButton(onClick = { onEdit(m) }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MonicoBlue)
                        }
                        // Delete
                        IconButton(onClick = { onDelete(m.id) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MonicoRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSidebarMenusTab(
    sidebarMenus: List<SidebarMenuItem>,
    onEdit: (SidebarMenuItem) -> Unit,
    onDelete: (String) -> Unit
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
        items(sidebarMenus) { sm ->
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(sm.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(sm.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                        Text("Role: ${sm.allowedRole} | Status: ${sm.status}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onEdit(sm) }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MonicoBlue)
                        }
                        IconButton(onClick = { onDelete(sm.id) }) {
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
        items(sliders) { s ->
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
                    if (s.image.isNotBlank()) {
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))) {
                            AsyncImage(
                                model = s.image,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.title, fontWeight = FontWeight.Bold)
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
        items(notices) { n ->
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
                    Text(n.text, modifier = Modifier.weight(1f), fontSize = 13.sp)
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
    onEdit: (PopupNotice) -> Unit,
    onDelete: (String) -> Unit
) {
    if (popups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("কোনো পপআপ নোটিশ নেই।", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(popups) { p ->
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.title, fontWeight = FontWeight.Bold)
                        Text(p.body, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Status: ${p.status} | Type: ${p.type}", fontSize = 10.sp, color = MonicoGreen)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
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

@Composable
private fun AdminCardsTab(
    products: List<ContentCard>,
    trainings: List<ContentCard>,
    targets: List<ContentCard>,
    onEdit: (String, ContentCard) -> Unit,
    onDelete: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("নতুন পণ্য লঞ্চ কার্ড (Product Launch):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        items(products) { c ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(c.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onEdit("product_launch_cards", c) }) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MonicoBlue)
                        }
                        IconButton(onClick = { onDelete("product_launch_cards", c.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MonicoRed)
                        }
                    }
                }
            }
        }

        item {
            Text("প্রোডাক্ট ট্রেনিং কার্ড (Product Training):", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        items(trainings) { c ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(c.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onEdit("product_training_cards", c) }) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MonicoBlue)
                        }
                        IconButton(onClick = { onDelete("product_training_cards", c.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MonicoRed)
                        }
                    }
                }
            }
        }

        item {
            Text("টার্গেট শিট কার্ড (Target Sheets):", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        items(targets) { c ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(c.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onEdit("product_target_sheets", c) }) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MonicoBlue)
                        }
                        IconButton(onClick = { onDelete("product_target_sheets", c.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MonicoRed)
                        }
                    }
                }
            }
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
            Text("কোনো ইউজার পাওয়া যায়নি।", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(users) { u ->
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(u.fullName.ifEmpty { u.employeeId }, fontWeight = FontWeight.Bold)
                        Text("ID: ${u.employeeId} | Role: ${u.role}", fontSize = 12.sp, color = MonicoBlue)
                        Text("Pass: ${u.password}", fontSize = 11.sp, color = MonicoRed)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onEdit(u) }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MonicoBlue)
                        }
                        if (u.employeeId != "KS") {
                            IconButton(onClick = { onDelete(u.employeeId) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MonicoRed)
                            }
                        }
                    }
                }
            }
        }
    }
}
