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
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://kushtia-depot-default-rtdb.firebaseio.com/MonicoOne"
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun fetchSyncMetaTimestamp(): Long? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$baseUrl/sync_meta/lastUpdated.json").get().build()
            val res = client.newCall(req).execute()
            val body = res.body?.string()?.trim()
            body?.toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchMenusShallowKeys(): Set<String> = withContext(Dispatchers.IO) {
        val set = mutableSetOf<String>()
        try {
            val req = Request.Builder().url("$baseUrl/menus.json?shallow=true").get().build()
            val res = client.newCall(req).execute()
            val body = res.body?.string() ?: return@withContext set
            if (body.startsWith("{")) {
                val json = JSONObject(body)
                val keys = json.keys()
                while (keys.hasNext()) {
                    set.add(keys.next())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        set
    }

    suspend fun notifySyncMeta(changeType: String = "general"): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
                .put("lastUpdated", System.currentTimeMillis())
                .put("changeType", changeType)
                .put("source", "android_app")
                .toString()
            val req = Request.Builder()
                .url("$baseUrl/sync_meta.json")
                .put(json.toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            false
        }
    }

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

            val json = JSONObject(body)
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

            val json = JSONObject(body)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = json.optJSONObject(key) ?: continue

                // 1. Robust image extraction: JSONArray, JSONObject (indexed 0, 1...), single string, or fallback fields
                val imgList = mutableListOf<String>()
                val imgArr = obj.optJSONArray("images")
                if (imgArr != null) {
                    for (i in 0 until imgArr.length()) {
                        val s = imgArr.optString(i)
                        if (!s.isNullOrBlank() && s != "null") imgList.add(s.trim())
                    }
                } else {
                    val imgObj = obj.optJSONObject("images")
                    if (imgObj != null) {
                        val sKeys = imgObj.keys().asSequence().toList().sortedWith(Comparator { a, b ->
                            val aNum = a.toIntOrNull()
                            val bNum = b.toIntOrNull()
                            if (aNum != null && bNum != null) aNum.compareTo(bNum) else a.compareTo(b)
                        })
                        for (k in sKeys) {
                            val s = imgObj.optString(k)
                            if (!s.isNullOrBlank() && s != "null") imgList.add(s.trim())
                        }
                    } else {
                        val singleImg = obj.optString("images", "")
                        if (singleImg.isNotBlank() && singleImg != "null") {
                            if (singleImg.contains(",") && !singleImg.startsWith("data:") && !singleImg.contains(";base64,")) {
                                singleImg.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { imgList.add(it) }
                            } else {
                                imgList.add(singleImg.trim())
                            }
                        }
                    }
                }

                // Additional fallback for singular image fields used by website admin
                val fallbackImg = listOf(
                    obj.optString("image"),
                    obj.optString("imageUrl"),
                    obj.optString("image_url"),
                    obj.optString("photo"),
                    obj.optString("picture")
                ).firstOrNull { !it.isNullOrBlank() && it != "null" }
                if (!fallbackImg.isNullOrBlank() && !imgList.contains(fallbackImg.trim())) {
                    imgList.add(fallbackImg.trim())
                }

                // 2. Robust URL extraction: checks url, fileUrl, pdfUrl, downloadUrl, link, targetUrl, etc.
                val extractedUrl = listOf(
                    obj.optString("url"),
                    obj.optString("fileUrl"),
                    obj.optString("file_url"),
                    obj.optString("fileURL"),
                    obj.optString("pdfUrl"),
                    obj.optString("pdf_url"),
                    obj.optString("pdfURL"),
                    obj.optString("pdf"),
                    obj.optString("file"),
                    obj.optString("downloadUrl"),
                    obj.optString("download_url"),
                    obj.optString("link"),
                    obj.optString("targetUrl"),
                    obj.optString("docUrl"),
                    obj.optString("attachment"),
                    obj.optString("filePath")
                ).firstOrNull { !it.isNullOrBlank() && it != "null" }?.trim() ?: ""

                val seenMap = mutableMapOf<String, Boolean>()
                val seenObj = obj.optJSONObject("seenBy")
                if (seenObj != null) {
                    val sKeys = seenObj.keys()
                    while (sKeys.hasNext()) {
                        val sk = sKeys.next()
                        seenMap[sk] = seenObj.optBoolean(sk, true)
                    }
                }

                list.add(
                    MenuItem(
                        id = key,
                        group = obj.optString("group", "grid2"),
                        title = obj.optString("title", ""),
                        icon = obj.optString("icon", "fa-folder"),
                        url = extractedUrl,
                        images = imgList,
                        allowedRole = obj.optString("allowedRole", "ALL"),
                        month = obj.optString("month", ""),
                        date = obj.optString("date", ""),
                        color = obj.optString("color", ""),
                        isPinned = obj.optBoolean("isPinned", false),
                        status = obj.optString("status", "active"),
                        seenBy = seenMap
                    )
                )
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

            val json = JSONObject(body)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = json.optJSONObject(key) ?: continue
                list.add(
                    SliderItem(
                        id = key,
                        title = obj.optString("title", ""),
                        image = obj.optString("image", "")
                    )
                )
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

            val json = JSONObject(body)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = json.optJSONObject(key) ?: continue
                list.add(
                    NoticeItem(
                        id = key,
                        text = obj.optString("text", "")
                    )
                )
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

            val json = JSONObject(body)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = json.optJSONObject(key) ?: continue
                list.add(
                    PopupNotice(
                        id = key,
                        title = obj.optString("title", ""),
                        body = obj.optString("body", ""),
                        status = obj.optString("status", "active"),
                        type = obj.optString("type", "general"),
                        image = obj.optString("image", "")
                    )
                )
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
            var body = res.body?.string()

            // If product_target_sheets is empty or 404, check alternate path target_sheets
            if ((body == null || body == "null" || body.isBlank() || body.trim() == "{}") && path == "product_target_sheets") {
                try {
                    val altReq = Request.Builder().url("$baseUrl/target_sheets.json").get().build()
                    val altRes = client.newCall(altReq).execute()
                    val altBody = altRes.body?.string()
                    if (!altBody.isNullOrBlank() && altBody != "null" && altBody.trim() != "{}") {
                        body = altBody
                    }
                } catch (_: Exception) {}
            }

            if (body == null || body == "null" || body.isBlank()) return@withContext list

            val rawItems = mutableListOf<Pair<String, JSONObject>>()
            val trimmedBody = body.trim()
            if (trimmedBody.startsWith("[")) {
                val arr = JSONArray(trimmedBody)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    if (obj != null) {
                        val itemId = obj.optString("id").ifBlank { "-C$i" }
                        rawItems.add(itemId to obj)
                    }
                }
            } else {
                val json = JSONObject(trimmedBody)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = json.optJSONObject(key) ?: continue
                    rawItems.add(key to obj)
                }
            }

            for ((key, obj) in rawItems) {
                val imgList = mutableListOf<String>()
                val imgArr = obj.optJSONArray("images")
                if (imgArr != null) {
                    for (i in 0 until imgArr.length()) {
                        val s = imgArr.optString(i)
                        if (!s.isNullOrBlank() && s != "null") imgList.add(s.trim())
                    }
                } else {
                    val imgObj = obj.optJSONObject("images")
                    if (imgObj != null) {
                        val sKeys = imgObj.keys().asSequence().toList().sortedWith(Comparator { a, b ->
                            val aNum = a.toIntOrNull()
                            val bNum = b.toIntOrNull()
                            if (aNum != null && bNum != null) aNum.compareTo(bNum) else a.compareTo(b)
                        })
                        for (k in sKeys) {
                            val s = imgObj.optString(k)
                            if (!s.isNullOrBlank() && s != "null") imgList.add(s.trim())
                        }
                    } else {
                        val singleImg = obj.optString("images", "")
                        if (singleImg.isNotBlank() && singleImg != "null") {
                            if (singleImg.contains(",") && !singleImg.startsWith("data:") && !singleImg.contains(";base64,")) {
                                singleImg.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { imgList.add(it) }
                            } else {
                                imgList.add(singleImg.trim())
                            }
                        }
                    }
                }

                val fallbackImg = listOf(
                    obj.optString("image"),
                    obj.optString("imageUrl"),
                    obj.optString("image_url")
                ).firstOrNull { !it.isNullOrBlank() && it != "null" }?.trim() ?: ""
                if (fallbackImg.isNotBlank() && !imgList.contains(fallbackImg)) {
                    imgList.add(fallbackImg)
                }

                val extractedUrl = listOf(
                    obj.optString("url"),
                    obj.optString("fileUrl"),
                    obj.optString("file_url"),
                    obj.optString("pdfUrl"),
                    obj.optString("pdf"),
                    obj.optString("file"),
                    obj.optString("downloadUrl"),
                    obj.optString("link")
                ).firstOrNull { !it.isNullOrBlank() && it != "null" }?.trim() ?: ""

                val cardId = obj.optString("id").ifBlank { key }
                list.add(
                    ContentCard(
                        id = cardId,
                        title = obj.optString("title", ""),
                        url = extractedUrl,
                        images = imgList,
                        image = fallbackImg.ifBlank { imgList.firstOrNull() ?: "" }
                    )
                )
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

            val json = JSONObject(body)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = json.optJSONObject(key) ?: continue
                list.add(
                    SidebarMenuItem(
                        id = key,
                        name = obj.optString("name", ""),
                        url = obj.optString("url", ""),
                        icon = obj.optString("icon", "fa-link"),
                        status = obj.optString("status", "active"),
                        allowedRole = obj.optString("allowedRole", "SUPER_ADMIN,DIC")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
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
                .put("group", menu.group)
                .put("title", menu.title)
                .put("icon", menu.icon)
                .put("url", menu.url)
                .put("fileUrl", menu.url) // Website admin compatibility
                .put("allowedRole", menu.allowedRole)
                .put("month", menu.month)
                .put("date", menu.date)
                .put("color", menu.color)
                .put("isPinned", menu.isPinned)
                .put("status", menu.status)

            val arr = JSONArray()
            menu.images.forEach { arr.put(it) }
            json.put("images", arr)
            if (menu.images.isNotEmpty()) {
                json.put("image", menu.images[0]) // Website admin compatibility
            }

            val targetUrl = if (menu.id.isNotEmpty()) "$baseUrl/menus/${menu.id}.json" else "$baseUrl/menus.json"
            val req = if (menu.id.isNotEmpty()) {
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

    /**
     * Opens an SSE (Server-Sent Events) live streaming call to /menus.json in Firebase RTDB.
     * Fires events in real time whenever web admin adds, edits, or deletes any menu item or file.
     */
    fun openMenusEventStream(): okhttp3.Call {
        val streamClient = client.newBuilder()
            .readTimeout(0, TimeUnit.MILLISECONDS) // infinite read timeout for SSE stream
            .build()
        val req = Request.Builder()
            .url("$baseUrl/menus.json")
            .header("Accept", "text/event-stream")
            .build()
        return streamClient.newCall(req)
    }

    suspend fun deleteMenuItem(menuId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$baseUrl/menus/$menuId.json").delete().build()
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveUser(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
                .put("employeeId", user.employeeId.uppercase())
                .put("fullName", user.fullName)
                .put("password", user.password)
                .put("role", user.role)
                .put("status", user.status)
                .put("profilePic", user.profilePic)
            val req = Request.Builder()
                .url("$baseUrl/users/${user.employeeId.uppercase()}.json")
                .put(json.toString().toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteUser(employeeId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$baseUrl/users/$employeeId.json").delete().build()
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveSlider(slider: SliderItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
                .put("title", slider.title)
                .put("image", slider.image)
            val targetUrl = if (slider.id.isNotEmpty()) "$baseUrl/sliders/${slider.id}.json" else "$baseUrl/sliders.json"
            val req = if (slider.id.isNotEmpty()) {
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
        try {
            val req = Request.Builder().url("$baseUrl/sliders/$id.json").delete().build()
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveNotice(text: String, id: String = ""): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().put("text", text)
            val targetUrl = if (id.isNotEmpty()) "$baseUrl/notices/$id.json" else "$baseUrl/notices.json"
            val req = if (id.isNotEmpty()) {
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
        try {
            val req = Request.Builder().url("$baseUrl/notices/$id.json").delete().build()
            client.newCall(req).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun savePopup(popup: PopupNotice): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
                .put("title", popup.title)
                .put("body", popup.body)
                .put("status", popup.status)
                .put("type", popup.type)
                .put("image", popup.image)
            val targetUrl = if (popup.id.isNotEmpty()) "$baseUrl/popup_notices/${popup.id}.json" else "$baseUrl/popup_notices.json"
            val req = if (popup.id.isNotEmpty()) {
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
        try {
            val req = Request.Builder().url("$baseUrl/popup_notices/$id.json").delete().build()
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
                .put("fileUrl", card.url) // Website admin compatibility
                .put("file_url", card.url)
                .put("pdfUrl", card.url)
                .put("image", card.image.ifBlank { card.images.firstOrNull() ?: "" })
                .put("imageUrl", card.image.ifBlank { card.images.firstOrNull() ?: "" })

            val arr = JSONArray()
            card.images.forEach { arr.put(it) }
            json.put("images", arr)

            val targetUrl = if (card.id.isNotEmpty()) "$baseUrl/$path/${card.id}.json" else "$baseUrl/$path.json"
            val req = if (card.id.isNotEmpty()) {
                Request.Builder().url(targetUrl).put(json.toString().toRequestBody(jsonMedia)).build()
            } else {
                Request.Builder().url(targetUrl).post(json.toString().toRequestBody(jsonMedia)).build()
            }
            val isSuccess = client.newCall(req).execute().isSuccessful

            // If saving target sheets, mirror to target_sheets node as well for website admin compatibility
            if (path == "product_target_sheets" && card.id.isNotEmpty()) {
                try {
                    val altUrl = "$baseUrl/target_sheets/${card.id}.json"
                    val altReq = Request.Builder().url(altUrl).put(json.toString().toRequestBody(jsonMedia)).build()
                    client.newCall(altReq).execute()
                } catch (_: Exception) {}
            }

            isSuccess
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteContentCard(path: String, id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$baseUrl/$path/$id.json").delete().build()
            val success = client.newCall(req).execute().isSuccessful
            if (path == "product_target_sheets") {
                try {
                    val altReq = Request.Builder().url("$baseUrl/target_sheets/$id.json").delete().build()
                    client.newCall(altReq).execute()
                } catch (_: Exception) {}
            }
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveSidebarMenu(menu: SidebarMenuItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
                .put("name", menu.name)
                .put("url", menu.url)
                .put("icon", menu.icon)
                .put("status", menu.status)
                .put("allowedRole", menu.allowedRole)
            val targetUrl = if (menu.id.isNotEmpty()) "$baseUrl/sidebar_menus/${menu.id}.json" else "$baseUrl/sidebar_menus.json"
            val req = if (menu.id.isNotEmpty()) {
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

    suspend fun deleteSidebarMenu(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$baseUrl/sidebar_menus/$id.json").delete().build()
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

            val json = JSONObject(body)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = json.optJSONObject(key) ?: continue

                val readMap = mutableMapOf<String, Boolean>()
                val readObj = obj.optJSONObject("readBy")
                if (readObj != null) {
                    val rKeys = readObj.keys()
                    while (rKeys.hasNext()) {
                        val rk = rKeys.next()
                        readMap[rk] = readObj.optBoolean(rk, true)
                    }
                }

                list.add(
                    NotificationItem(
                        id = key,
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
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list.sortedByDescending { it.timestamp }
    }

    suspend fun saveNotification(item: NotificationItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
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

            val targetUrl = if (item.id.isNotEmpty()) "$baseUrl/notifications/${item.id}.json" else "$baseUrl/notifications.json"
            val req = if (item.id.isNotEmpty()) {
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
        try {
            val req = Request.Builder().url("$baseUrl/notifications/$id.json").delete().build()
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

