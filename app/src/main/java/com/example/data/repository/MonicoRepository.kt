package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.*
import com.example.data.remote.FirebaseRtdbService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
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

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        loadSessionFromLocal()
        loadCacheFromLocal()
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

    // High performance parallel sync
    suspend fun syncAll(): Boolean = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        try {
            coroutineScope {
                val menusDeferred = async { service.fetchMenus() }
                val slidersDeferred = async { service.fetchSliders() }
                val noticesDeferred = async { service.fetchNotices() }
                val popupsDeferred = async { service.fetchPopups() }
                val prodDeferred = async { service.fetchCards("product_launch_cards") }
                val trainDeferred = async { service.fetchCards("product_training_cards") }
                val targetDeferred = async { service.fetchCards("product_target_sheets") }
                val sideDeferred = async { service.fetchSidebarMenus() }
                val usersDeferred = async { service.fetchUsers() }
                val notifDeferred = async { service.fetchNotifications() }

                val m = menusDeferred.await()
                if (m.isNotEmpty()) {
                    _menus.value = m
                    saveMenusToCache(m)
                }

                val s = slidersDeferred.await()
                if (s.isNotEmpty()) _sliders.value = s

                val n = noticesDeferred.await()
                if (n.isNotEmpty()) _notices.value = n

                val p = popupsDeferred.await()
                if (p.isNotEmpty()) _popups.value = p

                val prod = prodDeferred.await()
                if (prod.isNotEmpty()) _productCards.value = prod

                val train = trainDeferred.await()
                if (train.isNotEmpty()) _trainingCards.value = train

                val target = targetDeferred.await()
                if (target.isNotEmpty()) _targetSheets.value = target

                val side = sideDeferred.await()
                if (side.isNotEmpty()) _sidebarMenus.value = side

                val notifs = notifDeferred.await()
                if (notifs.isNotEmpty()) {
                    _notifications.value = notifs
                }

                val u = usersDeferred.await()
                if (u.isNotEmpty()) {
                    _allUsers.value = u.values.toList()
                    val cur = _currentUser.value
                    if (cur != null) {
                        val freshUser = u[cur.employeeId]
                        if (freshUser != null) {
                            saveUserSession(freshUser)
                        }
                    }
                }
            }

            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            val formattedTime = sdf.format(Date())
            _lastSyncTime.value = formattedTime
            prefs.edit().putString("last_sync_time", formattedTime).apply()

            val cur = _currentUser.value
            if (cur != null) {
                service.updatePresence(cur.employeeId, cur.fullName, cur.role)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
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

    suspend fun updateProfile(fullName: String, newPassword: String, newProfilePic: String? = null): Boolean {
        val user = _currentUser.value ?: return false
        try {
            val finalPic = newProfilePic ?: user.profilePic
            val finalPass = if (newPassword.isNotBlank()) newPassword else user.password
            service.updateUser(user.employeeId, fullName, finalPass, finalPic)
            val updated = user.copy(
                fullName = fullName,
                password = finalPass,
                profilePic = finalPic
            )
            saveUserSession(updated)

            // Also update in all users list
            val currentAll = _allUsers.value.toMutableList()
            val idx = currentAll.indexOfFirst { it.employeeId.equals(user.employeeId, ignoreCase = true) }
            if (idx != -1) currentAll[idx] = updated else currentAll.add(updated)
            _allUsers.value = currentAll
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // Instant optimistic CRUD operations
    suspend fun saveMenuItem(menu: MenuItem): Boolean {
        val targetId = if (menu.id.isNotBlank()) menu.id else "-M" + System.currentTimeMillis()
        val itemToSave = menu.copy(id = targetId)
        val currentList = _menus.value.toMutableList()
        val idx = currentList.indexOfFirst { it.id == targetId }
        if (idx != -1) {
            currentList[idx] = itemToSave
        } else {
            currentList.add(0, itemToSave)
        }
        _menus.value = currentList
        saveMenusToCache(currentList)

        return service.saveMenuItem(itemToSave)
    }

    suspend fun deleteMenuItem(id: String): Boolean {
        val currentList = _menus.value.filter { it.id != id }
        _menus.value = currentList
        saveMenusToCache(currentList)

        return service.deleteMenuItem(id)
    }

    suspend fun saveSidebarMenu(item: SidebarMenuItem): Boolean {
        val targetId = if (item.id.isNotBlank()) item.id else "-SM" + System.currentTimeMillis()
        val itemToSave = item.copy(id = targetId)
        val list = _sidebarMenus.value.toMutableList()
        val idx = list.indexOfFirst { it.id == targetId }
        if (idx != -1) list[idx] = itemToSave else list.add(itemToSave)
        _sidebarMenus.value = list

        return service.saveSidebarMenu(itemToSave)
    }

    suspend fun deleteSidebarMenu(id: String): Boolean {
        _sidebarMenus.value = _sidebarMenus.value.filter { it.id != id }
        return service.deleteSidebarMenu(id)
    }

    suspend fun saveSlider(slider: SliderItem): Boolean {
        val targetId = if (slider.id.isNotBlank()) slider.id else "-S" + System.currentTimeMillis()
        val itemToSave = slider.copy(id = targetId)
        val list = _sliders.value.toMutableList()
        val idx = list.indexOfFirst { it.id == targetId }
        if (idx != -1) list[idx] = itemToSave else list.add(itemToSave)
        _sliders.value = list

        return service.saveSlider(itemToSave)
    }

    suspend fun deleteSlider(id: String): Boolean {
        _sliders.value = _sliders.value.filter { it.id != id }
        return service.deleteSlider(id)
    }

    suspend fun saveNotice(text: String, id: String = ""): Boolean {
        val targetId = if (id.isNotBlank()) id else "-N" + System.currentTimeMillis()
        val itemToSave = NoticeItem(targetId, text)
        val list = _notices.value.toMutableList()
        val idx = list.indexOfFirst { it.id == targetId }
        if (idx != -1) list[idx] = itemToSave else list.add(0, itemToSave)
        _notices.value = list

        return service.saveNotice(text, targetId)
    }

    suspend fun deleteNotice(id: String): Boolean {
        _notices.value = _notices.value.filter { it.id != id }
        return service.deleteNotice(id)
    }

    suspend fun savePopup(popup: PopupNotice): Boolean {
        val targetId = if (popup.id.isNotBlank()) popup.id else "-P" + System.currentTimeMillis()
        val itemToSave = popup.copy(id = targetId)
        val list = _popups.value.toMutableList()
        val idx = list.indexOfFirst { it.id == targetId }
        if (idx != -1) list[idx] = itemToSave else list.add(0, itemToSave)
        _popups.value = list

        return service.savePopup(itemToSave)
    }

    suspend fun deletePopup(id: String): Boolean {
        _popups.value = _popups.value.filter { it.id != id }
        return service.deletePopup(id)
    }

    suspend fun saveCard(path: String, card: ContentCard): Boolean {
        val targetId = if (card.id.isNotBlank()) card.id else "-C" + System.currentTimeMillis()
        val itemToSave = card.copy(id = targetId)
        when (path) {
            "product_launch_cards" -> {
                val list = _productCards.value.toMutableList()
                val idx = list.indexOfFirst { it.id == targetId }
                if (idx != -1) list[idx] = itemToSave else list.add(itemToSave)
                _productCards.value = list
            }
            "product_training_cards" -> {
                val list = _trainingCards.value.toMutableList()
                val idx = list.indexOfFirst { it.id == targetId }
                if (idx != -1) list[idx] = itemToSave else list.add(itemToSave)
                _trainingCards.value = list
            }
            "product_target_sheets" -> {
                val list = _targetSheets.value.toMutableList()
                val idx = list.indexOfFirst { it.id == targetId }
                if (idx != -1) list[idx] = itemToSave else list.add(itemToSave)
                _targetSheets.value = list
            }
        }
        return service.saveContentCard(path, itemToSave)
    }

    suspend fun deleteCard(path: String, id: String): Boolean {
        when (path) {
            "product_launch_cards" -> _productCards.value = _productCards.value.filter { it.id != id }
            "product_training_cards" -> _trainingCards.value = _trainingCards.value.filter { it.id != id }
            "product_target_sheets" -> _targetSheets.value = _targetSheets.value.filter { it.id != id }
        }
        return service.deleteContentCard(path, id)
    }

    suspend fun saveUser(user: User): Boolean {
        val list = _allUsers.value.toMutableList()
        val idx = list.indexOfFirst { it.employeeId.equals(user.employeeId, ignoreCase = true) }
        if (idx != -1) list[idx] = user else list.add(user)
        _allUsers.value = list

        return service.saveUser(user)
    }

    suspend fun deleteUser(employeeId: String): Boolean {
        _allUsers.value = _allUsers.value.filterNot { it.employeeId.equals(employeeId, ignoreCase = true) }
        return service.deleteUser(employeeId)
    }

    fun getReadNotificationIds(empId: String): Set<String> {
        if (empId.isBlank()) return emptySet()
        return prefs.getStringSet("read_notifs_${empId.lowercase()}", emptySet()) ?: emptySet()
    }

    suspend fun markNotificationAsRead(id: String, empId: String) {
        if (empId.isBlank() || id.isBlank()) return
        val currentSet = getReadNotificationIds(empId).toMutableSet()
        currentSet.add(id)
        prefs.edit().putStringSet("read_notifs_${empId.lowercase()}", currentSet).apply()

        // Also update in memory
        val list = _notifications.value.toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx != -1) {
            val updatedRead = list[idx].readBy.toMutableMap()
            updatedRead[empId.uppercase()] = true
            list[idx] = list[idx].copy(readBy = updatedRead)
            _notifications.value = list
        }

        withContext(Dispatchers.IO) {
            service.markNotificationRead(id, empId.uppercase())
        }
    }

    suspend fun markAllNotificationsAsRead(empId: String) {
        if (empId.isBlank()) return
        val currentSet = getReadNotificationIds(empId).toMutableSet()
        val list = _notifications.value.map { notif ->
            currentSet.add(notif.id)
            val updatedRead = notif.readBy.toMutableMap()
            updatedRead[empId.uppercase()] = true
            notif.copy(readBy = updatedRead)
        }
        prefs.edit().putStringSet("read_notifs_${empId.lowercase()}", currentSet).apply()
        _notifications.value = list

        withContext(Dispatchers.IO) {
            _notifications.value.forEach { notif ->
                service.markNotificationRead(notif.id, empId.uppercase())
            }
        }
    }

    fun getUnreadNotificationCount(empId: String, role: String): Int {
        if (empId.isBlank()) return 0
        val readIds = getReadNotificationIds(empId)
        val upperEmp = empId.uppercase()
        val upperRole = role.uppercase()

        return _notifications.value.count { notif ->
            val matchesRole = notif.targetRole.equals("ALL", ignoreCase = true) ||
                    notif.targetRole.contains(upperRole, ignoreCase = true) ||
                    notif.targetRole.contains("PALL", ignoreCase = true)

            val isRead = readIds.contains(notif.id) || (notif.readBy[upperEmp] == true)
            matchesRole && !isRead
        }
    }

    suspend fun sendNotification(
        title: String,
        message: String,
        targetRole: String = "ALL",
        type: String = "info",
        senderName: String = "Super Admin"
    ): Boolean {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        val formattedDate = sdf.format(Date())
        val newId = "-N" + System.currentTimeMillis()

        val newItem = NotificationItem(
            id = newId,
            title = title.trim(),
            message = message.trim(),
            timestamp = System.currentTimeMillis(),
            dateFormatted = formattedDate,
            targetRole = targetRole,
            type = type,
            senderName = senderName,
            readBy = emptyMap()
        )

        val updated = mutableListOf(newItem)
        updated.addAll(_notifications.value)
        _notifications.value = updated

        return service.saveNotification(newItem)
    }

    suspend fun deleteNotification(id: String): Boolean {
        _notifications.value = _notifications.value.filter { it.id != id }
        return service.deleteNotification(id)
    }
}
