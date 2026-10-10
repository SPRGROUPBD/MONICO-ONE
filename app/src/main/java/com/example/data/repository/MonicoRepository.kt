package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.*
import com.example.data.remote.FirebaseRtdbService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MonicoRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("monico_one_prefs", Context.MODE_PRIVATE)
    val service = FirebaseRtdbService()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _menus = MutableStateFlow<List<MenuItem>>(emptyList())
    val menus: StateFlow<List<MenuItem>> = _menus.asStateFlow()

    private val _sliders = MutableStateFlow<List<SliderItem>>(emptyList())
    val sliders: StateFlow<List<SliderItem>> = _sliders.asStateFlow()

    private val _notices = MutableStateFlow<List<NoticeItem>>(emptyList())
    val notices: StateFlow<List<NoticeItem>> = _notices.asStateFlow()

    private val _popups = MutableStateFlow<List<PopupNotice>>(emptyList())
    val popups: StateFlow<List<PopupNotice>> = _popups.asStateFlow()

    private val _productCards = MutableStateFlow<List<ContentCard>>(emptyList())
    val productCards: StateFlow<List<ContentCard>> = _productCards.asStateFlow()

    private val _trainingCards = MutableStateFlow<List<ContentCard>>(emptyList())
    val trainingCards: StateFlow<List<ContentCard>> = _trainingCards.asStateFlow()

    private val _targetSheets = MutableStateFlow<List<ContentCard>>(emptyList())
    val targetSheets: StateFlow<List<ContentCard>> = _targetSheets.asStateFlow()

    private val _sidebarMenus = MutableStateFlow<List<SidebarMenuItem>>(emptyList())
    val sidebarMenus: StateFlow<List<SidebarMenuItem>> = _sidebarMenus.asStateFlow()

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        loadSessionFromLocal()
        loadCacheFromLocal()
        ensureDefaultItemsIfEmpty()
    }

    private fun loadSessionFromLocal() {
        val empId = prefs.getString("user_emp_id", null)
        if (!empId.isNullOrEmpty()) {
            _currentUser.value = User(
                employeeId = empId,
                fullName = prefs.getString("user_full_name", empId) ?: empId,
                password = prefs.getString("user_password", "") ?: "",
                role = prefs.getString("user_role", "MPO") ?: "MPO",
                status = "active",
                profilePic = prefs.getString("user_pic", "") ?: ""
            )
        }
        _lastSyncTime.value = prefs.getString("last_sync_time", "") ?: ""
    }

    private fun loadCacheFromLocal() {
        val menuJson = prefs.getString("cache_menus", null)
        if (!menuJson.isNullOrEmpty()) {
            try {
                val arr = JSONArray(menuJson)
                val list = mutableListOf<MenuItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val imgArr = obj.optJSONArray("images")
                    val imgList = mutableListOf<String>()
                    if (imgArr != null) {
                        for (j in 0 until imgArr.length()) imgList.add(imgArr.getString(j))
                    }
                    list.add(
                        MenuItem(
                            id = obj.optString("id"),
                            group = obj.optString("group"),
                            title = obj.optString("title"),
                            icon = obj.optString("icon"),
                            url = obj.optString("url"),
                            images = imgList,
                            allowedRole = obj.optString("allowedRole"),
                            month = obj.optString("month"),
                            date = obj.optString("date"),
                            color = obj.optString("color"),
                            isPinned = obj.optBoolean("isPinned"),
                            status = obj.optString("status")
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    _menus.value = list
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun ensureDefaultItemsIfEmpty() {
        if (_menus.value.isEmpty()) {
            _menus.value = getDefaultMenus()
        }
        if (_sliders.value.isEmpty()) {
            _sliders.value = listOf(
                SliderItem("s1", "Monico One - One Platform Complete Solution", "https://picsum.photos/800/400?random=1"),
                SliderItem("s2", "ফিল্ড সেলস ও অর্ডার এন্ট্রি ২০২৬", "https://picsum.photos/800/400?random=2"),
                SliderItem("s3", "নতুন পলিসি ও ইনসেন্টিভ প্রোগ্রাম", "https://picsum.photos/800/400?random=3")
            )
        }
        if (_notices.value.isEmpty()) {
            _notices.value = listOf(
                NoticeItem("n1", "জরুরি নোটিশ: ২০২৬ সালের সকল এমপ্লয়ি প্রতিদিন সকাল ৯টার মধ্যে হাজিরা নিশ্চিত করুন।"),
                NoticeItem("n2", "ফ্ল্যাট বোনাস ও টিএ বিল ফাইলসমূহ যথাসময়ে সাবমিট করুন।")
            )
        }
        if (_productCards.value.isEmpty()) {
            _productCards.value = listOf(
                ContentCard("pc1", "নতুন পণ্য: MoniCef 500mg", "", listOf("https://picsum.photos/600/800?random=11", "https://picsum.photos/600/800?random=12"), "https://picsum.photos/600/800?random=11"),
                ContentCard("pc2", "নতুন পণ্য: CardioPlus 20", "", listOf("https://picsum.photos/600/800?random=13"), "https://picsum.photos/600/800?random=13")
            )
        }
        if (_trainingCards.value.isEmpty()) {
            _trainingCards.value = listOf(
                ContentCard("tc1", "অ্যান্টিবায়োটিক সেলস ট্রেনিং", "", listOf("https://picsum.photos/600/800?random=21", "https://picsum.photos/600/800?random=22", "https://picsum.photos/600/800?random=23"), "https://picsum.photos/600/800?random=21"),
                ContentCard("tc2", "প্রোডাক্ট নলেজ ও প্রেজেন্টেশন", "", listOf("https://picsum.photos/600/800?random=24", "https://picsum.photos/600/800?random=25"), "https://picsum.photos/600/800?random=24")
            )
        }
        if (_targetSheets.value.isEmpty()) {
            _targetSheets.value = listOf(
                ContentCard("ts1", "মার্চ ২০২৬ প্রোডাক্ট টার্গেট শিট", "", listOf("https://picsum.photos/600/800?random=31", "https://picsum.photos/600/800?random=32"), "https://picsum.photos/600/800?random=31"),
                ContentCard("ts2", "এপ্রিল ২০২৬ ফিল্ড কোটা শিট", "", listOf("https://picsum.photos/600/800?random=33"), "https://picsum.photos/600/800?random=33")
            )
        }
        if (_sidebarMenus.value.isEmpty()) {
            _sidebarMenus.value = listOf(
                SidebarMenuItem("sm1", "Monico Web ERP", "https://example.com/erp", "fa-globe", "active", "SUPER_ADMIN,DIC"),
                SidebarMenuItem("sm2", "Order Monitoring Portal", "https://example.com/orders", "fa-chart-line", "active", "SUPER_ADMIN,DIC")
            )
        }
    }

    private fun getDefaultMenus(): List<MenuItem> {
        return listOf(
            // Grid 1: Primary Menus (Daily Work)
            MenuItem("g1_1", "grid1", "অর্ডার এন্ট্রি (Order Entry)", "fa-cart-shopping", "https://example.com/orders", emptyList(), "PALL+SALL", "", "", "", false, "active"),
            MenuItem("g1_2", "grid1", "ডি সি সি (DCC Entry)", "fa-check", "go:dcc", emptyList(), "PALL+SALL", "", "", "", false, "active"),
            MenuItem("g1_3", "grid1", "দৈনিক হাজিরা (Daily Attendance)", "fa-calendar", "https://example.com/attendance", emptyList(), "PALL+SALL", "", "", "", false, "active"),
            MenuItem("g1_4", "grid1", "ট্যুর প্ল্যান (Tour Plan)", "fa-route", "https://example.com/tour", emptyList(), "PALL+SALL", "", "", "", false, "active"),
            MenuItem("g1_5", "grid1", "ডেলিভারি ট্র্যাকিং (Delivery)", "fa-truck", "https://example.com/delivery", emptyList(), "PALL+SALL", "", "", "", false, "active"),
            MenuItem("g1_6", "grid1", "কালেকশন রিপোর্ট (Collection)", "fa-money-bill", "https://example.com/collection", emptyList(), "PALL+SALL", "", "", "", false, "active"),

            // Grid 2: Flat Bonus Sheet (Date, Month, Page count, unread dot)
            MenuItem("g2_1", "grid2", "মার্চ ২০২৬ ফ্ল্যাট বোনাস শিট", "https://picsum.photos/600/800?random=41", "", listOf("https://picsum.photos/600/800?random=41", "https://picsum.photos/600/800?random=42"), "PALL+SALL", "March", "2026-03-20", "", true, "active"),
            MenuItem("g2_2", "grid2", "ফেব্রুয়ারি ২০২৬ বোনাস শিট", "https://picsum.photos/600/800?random=43", "", listOf("https://picsum.photos/600/800?random=43", "https://picsum.photos/600/800?random=44", "https://picsum.photos/600/800?random=45"), "PALL+SALL", "February", "2026-02-15", "", false, "active"),
            MenuItem("g2_3", "grid2", "জানুয়ারি ২০২৬ স্পেশাল বোনাস", "https://picsum.photos/600/800?random=46", "", listOf("https://picsum.photos/600/800?random=46"), "PALL+SALL", "January", "2026-01-25", "", false, "active"),

            // Grid 3: Head Office Circular
            MenuItem("g3_1", "grid3", "সার্কুলার: নতুন পলিসি ২০২৬", "https://picsum.photos/600/800?random=51", "", listOf("https://picsum.photos/600/800?random=51", "https://picsum.photos/600/800?random=52"), "PALL+SALL", "March", "2026-03-01", "", true, "active"),
            MenuItem("g3_2", "grid3", "মার্কেট ডিরেক্টিভ ও গাইডলাইন", "https://picsum.photos/600/800?random=53", "", listOf("https://picsum.photos/600/800?random=53"), "PALL+SALL", "February", "2026-02-10", "", false, "active"),

            // Grid 4: Salary TA Bill
            MenuItem("g4_1", "grid4", "মার্চ ২০২৬ টিএ বিল শিট", "https://picsum.photos/600/800?random=61", "", listOf("https://picsum.photos/600/800?random=61", "https://picsum.photos/600/800?random=62"), "PALL+SALL", "March", "2026-03-25", "", false, "active"),
            MenuItem("g4_2", "grid4", "ফেব্রুয়ারি ২০২৬ বেতন শিট", "https://picsum.photos/600/800?random=63", "", listOf("https://picsum.photos/600/800?random=63"), "PALL+SALL", "February", "2026-02-28", "", false, "active"),

            // Grid 5: Sales Report (PDF)
            MenuItem("g5_1", "grid5", "মার্চ সেলস রিপোর্ট (PDF)", "fa-file-pdf", "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf", emptyList(), "PALL+SALL", "March", "2026-03-31", "", false, "active"),
            MenuItem("g5_2", "grid5", "কোয়ার্টার ১ ওভারভিউ (PDF)", "fa-file-pdf", "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf", emptyList(), "PALL+SALL", "February", "2026-02-25", "", false, "active")
        )
    }

    suspend fun login(empIdInput: String, passInput: String): Result<User> {
        val cleanId = empIdInput.trim().uppercase()
        val cleanPass = passInput.trim()

        try {
            val users = service.fetchUsers()
            val user = users[cleanId]
            if (user != null && (user.password == cleanPass || user.password.equals(cleanPass, ignoreCase = true))) {
                saveUserSession(user)
                service.updatePresence(user.employeeId, user.fullName, user.role)
                syncAll()
                return Result.success(user)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Offline / hardcoded admin fallback
        if (cleanId == "KS" && cleanPass == "Monico@Ku007") {
            val adminUser = User(
                employeeId = "KS",
                fullName = "Monico Super Admin",
                password = "Monico@Ku007",
                role = "SUPER_ADMIN",
                status = "active",
                profilePic = ""
            )
            saveUserSession(adminUser)
            return Result.success(adminUser)
        }

        // Offline check with cached user
        val cachedUser = _currentUser.value
        if (cachedUser != null && cachedUser.employeeId.equals(cleanId, ignoreCase = true) && cachedUser.password == cleanPass) {
            return Result.success(cachedUser)
        }

        return Result.failure(Exception("ভুল এমপ্লয়ি আইডি বা পাসওয়ার্ড!"))
    }

    private fun saveUserSession(user: User) {
        _currentUser.value = user
        prefs.edit()
            .putString("user_emp_id", user.employeeId)
            .putString("user_full_name", user.fullName)
            .putString("user_password", user.password)
            .putString("user_role", user.role)
            .putString("user_pic", user.profilePic)
            .apply()
    }

    fun logout() {
        _currentUser.value = null
        prefs.edit()
            .remove("user_emp_id")
            .remove("user_full_name")
            .remove("user_password")
            .remove("user_role")
            .remove("user_pic")
            .apply()
    }

    suspend fun syncAll(): Boolean {
        _isSyncing.value = true
        try {
            val m = service.fetchMenus()
            if (m.isNotEmpty()) {
                _menus.value = m
                saveMenusToCache(m)
            } else if (_menus.value.isEmpty()) {
                _menus.value = getDefaultMenus()
            }

            val s = service.fetchSliders()
            if (s.isNotEmpty()) _sliders.value = s

            val n = service.fetchNotices()
            if (n.isNotEmpty()) _notices.value = n

            val p = service.fetchPopups()
            if (p.isNotEmpty()) _popups.value = p

            val prod = service.fetchCards("product_launch_cards")
            if (prod.isNotEmpty()) _productCards.value = prod

            val train = service.fetchCards("product_training_cards")
            if (train.isNotEmpty()) _trainingCards.value = train

            val target = service.fetchCards("product_target_sheets")
            if (target.isNotEmpty()) _targetSheets.value = target

            val side = service.fetchSidebarMenus()
            if (side.isNotEmpty()) _sidebarMenus.value = side

            val u = service.fetchUsers()
            if (u.isNotEmpty()) {
                _allUsers.value = u.values.toList()
            }

            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            val formattedTime = sdf.format(Date())
            _lastSyncTime.value = formattedTime
            prefs.edit().putString("last_sync_time", formattedTime).apply()

            val cur = _currentUser.value
            if (cur != null) {
                service.updatePresence(cur.employeeId, cur.fullName, cur.role)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            _isSyncing.value = false
        }
    }

    private fun saveMenusToCache(list: List<MenuItem>) {
        try {
            val arr = JSONArray()
            list.forEach { m ->
                val obj = JSONObject()
                    .put("id", m.id)
                    .put("group", m.group)
                    .put("title", m.title)
                    .put("icon", m.icon)
                    .put("url", m.url)
                    .put("allowedRole", m.allowedRole)
                    .put("month", m.month)
                    .put("date", m.date)
                    .put("color", m.color)
                    .put("isPinned", m.isPinned)
                    .put("status", m.status)
                val imgArr = JSONArray()
                m.images.forEach { imgArr.put(it) }
                obj.put("images", imgArr)
                arr.put(obj)
            }
            prefs.edit().putString("cache_menus", arr.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markMenuSeen(menuId: String) {
        val user = _currentUser.value ?: return
        service.markMenuSeen(menuId, user.employeeId)
        _menus.value = _menus.value.map {
            if (it.id == menuId) {
                val newSeen = it.seenBy.toMutableMap()
                newSeen[user.employeeId] = true
                it.copy(seenBy = newSeen)
            } else it
        }
    }

    suspend fun updateProfile(fullName: String, newPassword: String): Boolean {
        val user = _currentUser.value ?: return false
        try {
            service.updateUser(user.employeeId, fullName, newPassword, null)
            val updated = user.copy(
                fullName = fullName,
                password = if (newPassword.isNotEmpty()) newPassword else user.password
            )
            saveUserSession(updated)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun saveSidebarMenu(item: SidebarMenuItem): Boolean {
        val ok = service.saveSidebarMenu(item)
        if (ok) syncAll()
        return ok
    }

    suspend fun deleteSidebarMenu(id: String): Boolean {
        val ok = service.deleteSidebarMenu(id)
        if (ok) syncAll()
        return ok
    }

    suspend fun saveCard(path: String, card: ContentCard): Boolean {
        val ok = service.saveContentCard(path, card)
        if (ok) syncAll()
        return ok
    }

    suspend fun deleteCard(path: String, id: String): Boolean {
        val ok = service.deleteContentCard(path, id)
        if (ok) syncAll()
        return ok
    }

    suspend fun saveMenuItem(menu: MenuItem): Boolean {
        val ok = service.saveMenuItem(menu)
        if (ok) syncAll()
        return ok
    }

    suspend fun deleteMenuItem(id: String): Boolean {
        val ok = service.deleteMenuItem(id)
        if (ok) syncAll()
        return ok
    }

    suspend fun saveSlider(slider: SliderItem): Boolean {
        val ok = service.saveSlider(slider)
        if (ok) syncAll()
        return ok
    }

    suspend fun deleteSlider(id: String): Boolean {
        val ok = service.deleteSlider(id)
        if (ok) syncAll()
        return ok
    }

    suspend fun saveNotice(text: String, id: String = ""): Boolean {
        val ok = service.saveNotice(text, id)
        if (ok) syncAll()
        return ok
    }

    suspend fun deleteNotice(id: String): Boolean {
        val ok = service.deleteNotice(id)
        if (ok) syncAll()
        return ok
    }

    suspend fun savePopup(popup: PopupNotice): Boolean {
        val ok = service.savePopup(popup)
        if (ok) syncAll()
        return ok
    }

    suspend fun deletePopup(id: String): Boolean {
        val ok = service.deletePopup(id)
        if (ok) syncAll()
        return ok
    }

    suspend fun saveUser(user: User): Boolean {
        val ok = service.saveUser(user)
        if (ok) syncAll()
        return ok
    }

    suspend fun deleteUser(employeeId: String): Boolean {
        val ok = service.deleteUser(employeeId)
        if (ok) syncAll()
        return ok
    }
}
