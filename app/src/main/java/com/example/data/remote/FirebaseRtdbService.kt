package com.example.data.remote

import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class FirebaseRtdbService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://kushtia-depot-default-rtdb.firebaseio.com/MonicoOne"
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    fun fetchStringProperty(url: String): String? {
        return try {
            val req = Request.Builder().url(url).get().build()
            val res = client.newCall(req).execute()
            val body = res.body?.string()?.trim()
            if (body == null || body == "null" || body.isBlank()) null
            else if (body.startsWith("\"") && body.endsWith("\"") && body.length >= 2) {
                body.substring(1, body.length - 1).replace("\\\"", "\"").replace("\\\\", "\\")
            } else {
                body
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Ultra-fast single user authentication and profile fetch (<300ms, only ~150 bytes).
     * Bypasses the 12.5MB payload by directly querying node properties in RTDB.
     */
    suspend fun fetchSingleUser(userId: String): User? = withContext(Dispatchers.IO) {
        val clean = userId.trim()
        if (clean.isBlank()) return@withContext null

        var matchedKey: String? = null
        val candidates = listOf(clean.uppercase(), clean, clean.lowercase()).distinct()
        for (c in candidates) {
            val testPass = fetchStringProperty("$baseUrl/users/$c/password.json")
            if (testPass != null) {
                matchedKey = c
                break
            }
        }

        // If direct candidate lookups didn't find the node, query shallow keys (only 200 bytes)
        if (matchedKey == null) {
            try {
                val shallowReq = Request.Builder().url("$baseUrl/users.json?shallow=true").get().build()
                val shallowRes = client.newCall(shallowReq).execute()
                val shallowBody = shallowRes.body?.string()
                if (shallowBody != null && shallowBody != "null" && shallowBody.startsWith("{")) {
                    val sJson = JSONObject(shallowBody)
                    val keys = sJson.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        if (k.equals(clean, ignoreCase = true)) {
                            matchedKey = k
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (matchedKey == null) return@withContext null

        try {
            coroutineScope {
                val passDeferred = async { fetchStringProperty("$baseUrl/users/$matchedKey/password.json") ?: "" }
                val nameDeferred = async { fetchStringProperty("$baseUrl/users/$matchedKey/fullName.json") ?: matchedKey }
                val roleDeferred = async { fetchStringProperty("$baseUrl/users/$matchedKey/role.json") ?: "MPO" }
                val statusDeferred = async { fetchStringProperty("$baseUrl/users/$matchedKey/status.json") ?: "active" }
                val empIdDeferred = async { fetchStringProperty("$baseUrl/users/$matchedKey/employeeId.json") ?: matchedKey }

                val pass = passDeferred.await()
                val name = nameDeferred.await()
                val role = roleDeferred.await()
                val status = statusDeferred.await()
                val empId = empIdDeferred.await().ifBlank { matchedKey }

                User(
                    employeeId = empId,
                    fullName = name,
                    password = pass,
                    role = role,
                    status = status,
                    profilePic = ""
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchUserProfilePic(userId: String): String? = withContext(Dispatchers.IO) {
        val clean = userId.trim()
        if (clean.isBlank()) return@withContext null
        fetchStringProperty("$baseUrl/users/$clean/profilePic.json")
    }

    suspend fun fetchUsers(): Map<String, User> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, User>()
        try {
            val req = Request.Builder().url("$baseUrl/users.json").get().build()
            val res = client.newCall(req).execute()
            val body = res.body?.string() ?: return@withContext result
            if (body == "null" || body.isBlank()) return@withContext result

            val trimmed = body.trim()
            if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    val uObj = arr.optJSONObject(i) ?: continue
                    val empId = uObj.optString("employeeId", i.toString()).uppercase()
                    result[empId] = User(
                        employeeId = empId,
                        fullName = uObj.optString("fullName", empId),
                        password = uObj.optString("password", ""),
                        role = uObj.optString("role", "MPO"),
                        status = uObj.optString("status", "active"),
                        profilePic = uObj.optString("profilePic", "")
                    )
                }
            } else {
                val json = JSONObject(trimmed)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val uObj = json.optJSONObject(key)
                    if (uObj != null) {
                        result[key.uppercase()] = User(
                            employeeId = uObj.optString("employeeId", key).uppercase(),
                            fullName = uObj.optString("fullName", key),
                            password = uObj.optString("password", ""),
                            role = uObj.optString("role", "MPO"),
                            status = uObj.optString("status", "active"),
                            profilePic = uObj.optString("profilePic", "")
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    suspend fun fetchMenus(): List<MenuItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MenuItem>()
        try {
            val req = Request.Builder().url("$baseUrl/menus.json").get().build()
            val res = client.newCall(req).execute()
            val body = res.body?.string() ?: return@withContext list
            if (body == "null" || body.isBlank()) return@withContext list

            fun parseMenu(key: String, obj: JSONObject): MenuItem {
                val imgList = mutableListOf<String>()
                val imgArr = obj.optJSONArray("images")
                if (imgArr != null) {
                    for (i in 0 until imgArr.length()) {
                        val s = imgArr.optString(i)
                        if (!s.isNullOrBlank()) imgList.add(s)
                    }
                }
                val seenMap = mutableMapOf<String, Boolean>()
                val seenObj = obj.optJSONObject("seenBy")
                if (seenObj != null) {
                    val sKeys = seenObj.keys()
                    while (sKeys.hasNext()) {
                        val sk = sKeys.next()
                        seenMap[sk] = seenObj.optBoolean(sk, true)
                    }
                }

                val rawId = obj.optString("id").trim()
                val itemId = if (rawId.isNotBlank()) rawId else key.trim().ifBlank { java.util.UUID.randomUUID().toString() }
                return MenuItem(
                    id = itemId,
                    group = obj.optString("group", "grid1"),
                    title = obj.optString("title", ""),
                    icon = obj.optString("icon", "fa-folder"),
                    url = obj.optString("url", ""),
                    images = imgList,
                    allowedRole = obj.optString("allowedRole", "PALL"),
                    month = obj.optString("month", ""),
                    date = obj.optString("date", ""),
                    color = obj.optString("color", ""),
                    isPinned = obj.optBoolean("isPinned", false),
                    status = obj.optString("status", "active"),
                    seenBy = seenMap
                )
            }

            val trimmed = body.trim()
            if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    list.add(parseMenu(i.toString(), obj))
                }
            } else {
                val json = JSONObject(trimmed)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = json.optJSONObject(key) ?: continue
                    list.add(parseMenu(key, obj))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun fetchSliders(): List<SliderItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SliderItem>()
        try {
            val req = Request.Builder().url("$baseUrl/sliders.json").get().build()
            val res = client.newCall(req).execute()
            val body = res.body?.string() ?: return@withContext list
            if (body == "null" || body.isBlank()) return@withContext list

            val trimmed = body.trim()
            if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id", i.toString()).ifBlank { i.toString() }
                    list.add(SliderItem(id = id, title = obj.optString("title", ""), image = obj.optString("image", "")))
                }
            } else {
                val json = JSONObject(trimmed)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = json.optJSONObject(key) ?: continue
                    val id = obj.optString("id", key).ifBlank { key }
                    list.add(SliderItem(id = id, title = obj.optString("title", ""), image = obj.optString("image", "")))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun fetchNotices(): List<NoticeItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<NoticeItem>()
        try {
            val req = Request.Builder().url("$baseUrl/notices.json").get().build()
            val res = client.newCall(req).execute()
            val body = res.body?.string() ?: return@withContext list
            if (body == "null" || body.isBlank()) return@withContext list

            val trimmed = body.trim()
            if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id", i.toString()).ifBlank { i.toString() }
                    list.add(NoticeItem(id = id, text = obj.optString("text", "")))
                }
            } else {
                val json = JSONObject(trimmed)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = json.optJSONObject(key) ?: continue
                    val id = obj.optString("id", key).ifBlank { key }
                    list.add(NoticeItem(id = id, text = obj.optString("text", "")))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun fetchPopups(): List<PopupNotice> = withContext(Dispatchers.IO) {
        val list = mutableListOf<PopupNotice>()
        try {
            val req = Request.Builder().url("$baseUrl/popup_notices.json").get().build()
            val res = client.newCall(req).execute()
            val body = res.body?.string() ?: return@withContext list
            if (body == "null" || body.isBlank()) return@withContext list

            val trimmed = body.trim()
            fun parsePopup(key: String, obj: JSONObject): PopupNotice {
                val id = obj.optString("id", key).ifBlank { key }
                return PopupNotice(
                    id = id,
                    title = obj.optString("title", ""),
                    body = obj.optString("body", ""),
                    status = obj.optString("status", "active"),
                    type = obj.optString("type", "general"),
                    image = obj.optString("image", "")
                )
            }

            if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    list.add(parsePopup(i.toString(), obj))
                }
            } else {
                val json = JSONObject(trimmed)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = json.optJSONObject(key) ?: continue
                    list.add(parsePopup(key, obj))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun fetchCards(path: String): List<ContentCard> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ContentCard>()
        try {
            val req = Request.Builder().url("$baseUrl/$path.json").get().build()
            val res = client.newCall(req).execute()
            val body = res.body?.string() ?: return@withContext list
            if (body == "null" || body.isBlank()) return@withContext list

            val trimmed = body.trim()
            if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val imgList = mutableListOf<String>()
                    val imgArr = obj.optJSONArray("images")
                    if (imgArr != null) {
                        for (j in 0 until imgArr.length()) {
                            val s = imgArr.optString(j)
                            if (!s.isNullOrBlank()) imgList.add(s.trim())
                        }
                    } else {
                        val imgStr = obj.optString("images", "")
                        if (imgStr.isNotBlank()) {
                            imgStr.split("\n", ",").map { it.trim() }.filter { it.isNotBlank() }.forEach { imgList.add(it) }
                        }
                    }
                    val singleImg = obj.optString("image", "").trim()
                    if (singleImg.isNotBlank() && !imgList.contains(singleImg)) {
                        imgList.add(0, singleImg)
                    }
                    val cardId = obj.optString("id", i.toString())
                    list.add(
                        ContentCard(
                            id = cardId,
                            title = obj.optString("title", ""),
                            url = obj.optString("url", ""),
                            images = imgList,
                            image = singleImg
                        )
                    )
                }
            } else {
                val json = JSONObject(trimmed)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = json.optJSONObject(key) ?: continue
                    val imgList = mutableListOf<String>()
                    val imgArr = obj.optJSONArray("images")
                    if (imgArr != null) {
                        for (i in 0 until imgArr.length()) {
                            val s = imgArr.optString(i)
                            if (!s.isNullOrBlank()) imgList.add(s.trim())
                        }
                    } else {
                        val imgStr = obj.optString("images", "")
                        if (imgStr.isNotBlank()) {
                            imgStr.split("\n", ",").map { it.trim() }.filter { it.isNotBlank() }.forEach { imgList.add(it) }
                        }
                    }
                    val singleImg = obj.optString("image", "").trim()
                    if (singleImg.isNotBlank() && !imgList.contains(singleImg)) {
                        imgList.add(0, singleImg)
                    }
                    list.add(
                        ContentCard(
                            id = key,
                            title = obj.optString("title", ""),
                            url = obj.optString("url", ""),
                            images = imgList,
                            image = singleImg
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun fetchSidebarMenus(): List<SidebarMenuItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SidebarMenuItem>()
        try {
            val req = Request.Builder().url("$baseUrl/sidebar_menus.json").get().build()
            val res = client.newCall(req).execute()
            val body = res.body?.string() ?: return@withContext list
            if (body == "null" || body.isBlank()) return@withContext list

            val trimmed = body.trim()
            if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id", i.toString()).ifBlank { i.toString() }
                    list.add(
                        SidebarMenuItem(
                            id = id,
                            name = obj.optString("name", ""),
                            url = obj.optString("url", ""),
                            icon = obj.optString("icon", "fa-link"),
                            status = obj.optString("status", "active"),
                            allowedRole = obj.optString("allowedRole", "SUPER_ADMIN,DIC"),
                            order = obj.optInt("order", i)
                        )
                    )
                }
            } else {
                val json = JSONObject(trimmed)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = json.optJSONObject(key) ?: continue
                    val id = obj.optString("id", key).ifBlank { key }
                    list.add(
                        SidebarMenuItem(
                            id = id,
                            name = obj.optString("name", ""),
                            url = obj.optString("url", ""),
                            icon = obj.optString("icon", "fa-link"),
                            status = obj.optString("status", "active"),
                            allowedRole = obj.optString("allowedRole", "SUPER_ADMIN,DIC"),
                            order = obj.optInt("order", 0)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list.sortedWith(compareBy<SidebarMenuItem> { it.order }.thenBy { it.name })
    }

    suspend fun markMenuSeen(menuId: String, employeeId: String) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().put(employeeId, true).toString()
            val req = Request.Builder()
                .url("$baseUrl/menus/$menuId/seenBy.json")
                .patch(json.toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateUser(employeeId: String, fullName: String, password: String, profilePic: String?) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            json.put("fullName", fullName)
            if (password.isNotEmpty()) {
                json.put("password", password)
            }
            if (profilePic != null) {
                json.put("profilePic", profilePic)
            }
            val req = Request.Builder()
                .url("$baseUrl/users/$employeeId.json")
                .patch(json.toString().toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updatePresence(employeeId: String, fullName: String, role: String) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
                .put("employeeId", employeeId)
                .put("fullName", fullName)
                .put("role", role)
                .put("timestamp", System.currentTimeMillis() / 1000)
                .toString()
            val req = Request.Builder()
                .url("$baseUrl/online_users/$employeeId.json")
                .put(json.toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Admin operations
    suspend fun saveMenuItem(menu: MenuItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
                .put("id", menu.id)
                .put("group", menu.group)
                .put("title", menu.title)
                .put("icon", menu.icon)
                .put("url", menu.url)
                .put("allowedRole", menu.allowedRole)
                .put("month", menu.month)
                .put("date", menu.date)
                .put("color", menu.color)
                .put("isPinned", menu.isPinned)
                .put("status", menu.status)

            val arr = JSONArray()
            menu.images.forEach { arr.put(it) }
            json.put("images", arr)

            val cleanId = if (menu.id.isNotBlank()) java.net.URLEncoder.encode(menu.id.trim(), "UTF-8") else ""
            val targetUrl = if (cleanId.isNotEmpty()) "$baseUrl/menus/$cleanId.json" else "$baseUrl/menus.json"
            val req = if (cleanId.isNotEmpty()) {
                Request.Builder().url(targetUrl).put(json.toString().toRequestBody(jsonMedia)).build()
            } else {
                Request.Builder().url(targetUrl).post(json.toString().toRequestBody(jsonMedia)).build()
            }
            val res = client.newCall(req).execute()
            res.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteMenuItem(menuId: String): Boolean = withContext(Dispatchers.IO) {
        if (menuId.isBlank()) return@withContext false
        try {
            val cleanId = java.net.URLEncoder.encode(menuId.trim(), "UTF-8")
            val req = Request.Builder().url("$baseUrl/menus/$cleanId.json").delete().build()
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveUser(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanEmpId = user.employeeId.trim().uppercase()
            if (cleanEmpId.isBlank()) return@withContext false
            val encodedEmpId = java.net.URLEncoder.encode(cleanEmpId, "UTF-8")
            val json = JSONObject()
                .put("employeeId", cleanEmpId)
                .put("fullName", user.fullName)
                .put("password", user.password)
                .put("role", user.role)
                .put("status", user.status)
                .put("profilePic", user.profilePic)
            val req = Request.Builder()
                .url("$baseUrl/users/$encodedEmpId.json")
                .put(json.toString().toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteUser(employeeId: String): Boolean = withContext(Dispatchers.IO) {
        if (employeeId.isBlank()) return@withContext false
        try {
            val cleanId = java.net.URLEncoder.encode(employeeId.trim().uppercase(), "UTF-8")
            val req = Request.Builder().url("$baseUrl/users/$cleanId.json").delete().build()
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveSlider(slider: SliderItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
                .put("id", slider.id)
                .put("title", slider.title)
                .put("image", slider.image)
            val cleanId = if (slider.id.isNotBlank()) java.net.URLEncoder.encode(slider.id.trim(), "UTF-8") else ""
            val targetUrl = if (cleanId.isNotEmpty()) "$baseUrl/sliders/$cleanId.json" else "$baseUrl/sliders.json"
            val req = if (cleanId.isNotEmpty()) {
                Request.Builder().url(targetUrl).put(json.toString().toRequestBody(jsonMedia)).build()
            } else {
                Request.Builder().url(targetUrl).post(json.toString().toRequestBody(jsonMedia)).build()
            }
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteSlider(id: String): Boolean = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext false
        try {
            val cleanId = java.net.URLEncoder.encode(id.trim(), "UTF-8")
            val req = Request.Builder().url("$baseUrl/sliders/$cleanId.json").delete().build()
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveNotice(text: String, id: String = ""): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().put("id", id).put("text", text)
            val cleanId = if (id.isNotBlank()) java.net.URLEncoder.encode(id.trim(), "UTF-8") else ""
            val targetUrl = if (cleanId.isNotEmpty()) "$baseUrl/notices/$cleanId.json" else "$baseUrl/notices.json"
            val req = if (cleanId.isNotEmpty()) {
                Request.Builder().url(targetUrl).put(json.toString().toRequestBody(jsonMedia)).build()
            } else {
                Request.Builder().url(targetUrl).post(json.toString().toRequestBody(jsonMedia)).build()
            }
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteNotice(id: String): Boolean = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext false
        try {
            val cleanId = java.net.URLEncoder.encode(id.trim(), "UTF-8")
            val req = Request.Builder().url("$baseUrl/notices/$cleanId.json").delete().build()
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun savePopup(popup: PopupNotice): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
                .put("id", popup.id)
                .put("title", popup.title)
                .put("body", popup.body)
                .put("status", popup.status)
                .put("type", popup.type)
                .put("image", popup.image)
            val cleanId = if (popup.id.isNotBlank()) java.net.URLEncoder.encode(popup.id.trim(), "UTF-8") else ""
            val targetUrl = if (cleanId.isNotEmpty()) "$baseUrl/popup_notices/$cleanId.json" else "$baseUrl/popup_notices.json"
            val req = if (cleanId.isNotEmpty()) {
                Request.Builder().url(targetUrl).put(json.toString().toRequestBody(jsonMedia)).build()
            } else {
                Request.Builder().url(targetUrl).post(json.toString().toRequestBody(jsonMedia)).build()
            }
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deletePopup(id: String): Boolean = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext false
        try {
            val cleanId = java.net.URLEncoder.encode(id.trim(), "UTF-8")
            val req = Request.Builder().url("$baseUrl/popup_notices/$cleanId.json").delete().build()
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveContentCard(path: String, card: ContentCard): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
                .put("id", card.id)
                .put("title", card.title)
                .put("url", card.url)
                .put("image", card.image)
            val arr = JSONArray()
            card.images.forEach { arr.put(it) }
            json.put("images", arr)
            val cleanId = if (card.id.isNotBlank()) java.net.URLEncoder.encode(card.id.trim(), "UTF-8") else ""
            val targetUrl = if (cleanId.isNotEmpty()) "$baseUrl/$path/$cleanId.json" else "$baseUrl/$path.json"
            val req = if (cleanId.isNotEmpty()) {
                Request.Builder().url(targetUrl).put(json.toString().toRequestBody(jsonMedia)).build()
            } else {
                Request.Builder().url(targetUrl).post(json.toString().toRequestBody(jsonMedia)).build()
            }
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteContentCard(path: String, id: String): Boolean = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext false
        try {
            val cleanId = java.net.URLEncoder.encode(id.trim(), "UTF-8")
            val req = Request.Builder().url("$baseUrl/$path/$cleanId.json").delete().build()
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveSidebarMenu(menu: SidebarMenuItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
                .put("id", menu.id)
                .put("name", menu.name)
                .put("url", menu.url)
                .put("icon", menu.icon)
                .put("status", menu.status)
                .put("allowedRole", menu.allowedRole)
                .put("order", menu.order)
            val cleanId = if (menu.id.isNotBlank()) java.net.URLEncoder.encode(menu.id.trim(), "UTF-8") else ""
            val targetUrl = if (cleanId.isNotEmpty()) "$baseUrl/sidebar_menus/$cleanId.json" else "$baseUrl/sidebar_menus.json"
            val req = if (cleanId.isNotEmpty()) {
                Request.Builder().url(targetUrl).put(json.toString().toRequestBody(jsonMedia)).build()
            } else {
                Request.Builder().url(targetUrl).post(json.toString().toRequestBody(jsonMedia)).build()
            }
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun reorderSidebarMenus(menus: List<SidebarMenuItem>): Boolean = withContext(Dispatchers.IO) {
        try {
            coroutineScope {
                menus.mapIndexed { idx, item ->
                    async {
                        val cleanId = java.net.URLEncoder.encode(item.id.trim(), "UTF-8")
                        val patchJson = JSONObject().put("order", idx).toString()
                        val req = Request.Builder()
                            .url("$baseUrl/sidebar_menus/$cleanId.json")
                            .patch(patchJson.toRequestBody(jsonMedia))
                            .build()
                        client.newCall(req).execute().isSuccessful
                    }
                }.all { it.await() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteSidebarMenu(id: String): Boolean = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext false
        try {
            val cleanId = java.net.URLEncoder.encode(id.trim(), "UTF-8")
            val req = Request.Builder().url("$baseUrl/sidebar_menus/$cleanId.json").delete().build()
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchNotifications(): List<NotificationItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<NotificationItem>()
        try {
            val req = Request.Builder().url("$baseUrl/notifications.json").get().build()
            val res = client.newCall(req).execute()
            val body = res.body?.string() ?: return@withContext list
            if (body == "null" || body.isBlank()) return@withContext list

            fun parseNotif(key: String, obj: JSONObject): NotificationItem {
                val readMap = mutableMapOf<String, Boolean>()
                val readObj = obj.optJSONObject("readBy")
                if (readObj != null) {
                    val rKeys = readObj.keys()
                    while (rKeys.hasNext()) {
                        val rk = rKeys.next()
                        readMap[rk] = readObj.optBoolean(rk, true)
                    }
                }

                val notifId = obj.optString("id", key).ifBlank { key }
                return NotificationItem(
                    id = notifId,
                    title = obj.optString("title", ""),
                    message = obj.optString("message", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    dateFormatted = obj.optString("dateFormatted", ""),
                    targetRole = obj.optString("targetRole", "ALL"),
                    type = obj.optString("type", "info"),
                    senderName = obj.optString("senderName", "Super Admin"),
                    targetUrl = obj.optString("targetUrl", ""),
                    readBy = readMap
                )
            }

            val trimmed = body.trim()
            if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    list.add(parseNotif(i.toString(), obj))
                }
            } else {
                val json = JSONObject(trimmed)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = json.optJSONObject(key) ?: continue
                    list.add(parseNotif(key, obj))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list.sortedByDescending { it.timestamp }
    }

    suspend fun saveNotification(item: NotificationItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
                .put("id", item.id)
                .put("title", item.title)
                .put("message", item.message)
                .put("timestamp", item.timestamp)
                .put("dateFormatted", item.dateFormatted)
                .put("targetRole", item.targetRole)
                .put("type", item.type)
                .put("senderName", item.senderName)
                .put("targetUrl", item.targetUrl)

            val readObj = JSONObject()
            item.readBy.forEach { (k, v) -> readObj.put(k, v) }
            json.put("readBy", readObj)

            val cleanId = if (item.id.isNotBlank()) java.net.URLEncoder.encode(item.id.trim(), "UTF-8") else ""
            val targetUrl = if (cleanId.isNotEmpty()) "$baseUrl/notifications/$cleanId.json" else "$baseUrl/notifications.json"
            val req = if (cleanId.isNotEmpty()) {
                Request.Builder().url(targetUrl).put(json.toString().toRequestBody(jsonMedia)).build()
            } else {
                Request.Builder().url(targetUrl).post(json.toString().toRequestBody(jsonMedia)).build()
            }
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteNotification(id: String): Boolean = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext false
        try {
            val cleanId = java.net.URLEncoder.encode(id.trim(), "UTF-8")
            val req = Request.Builder().url("$baseUrl/notifications/$cleanId.json").delete().build()
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun markNotificationRead(notificationId: String, empId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$baseUrl/notifications/$notificationId/readBy/$empId.json")
                .put("true".toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

