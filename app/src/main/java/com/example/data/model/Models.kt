package com.example.data.model

data class User(
    val employeeId: String = "",
    val fullName: String = "",
    val password: String = "",
    val role: String = "MPO",
    val status: String = "active",
    val profilePic: String = ""
)

data class MenuItem(
    val id: String = "",
    val group: String = "grid1", // grid1, grid2, grid3, grid4, grid5
    val title: String = "",
    val icon: String = "fa-folder",
    val url: String = "",
    val images: List<String> = emptyList(),
    val allowedRole: String = "PALL",
    val month: String = "",
    val date: String = "",
    val color: String = "",
    val isPinned: Boolean = false,
    val status: String = "active",
    val seenBy: Map<String, Boolean> = emptyMap()
)

data class SliderItem(
    val id: String = "",
    val title: String = "",
    val image: String = ""
)

data class NoticeItem(
    val id: String = "",
    val text: String = ""
)

data class PopupNotice(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val status: String = "active",
    val type: String = "general", // general, product
    val image: String = ""
)

data class ContentCard(
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val images: List<String> = emptyList(),
    val image: String = ""
)

data class SidebarMenuItem(
    val id: String = "",
    val name: String = "",
    val url: String = "",
    val icon: String = "fa-link",
    val status: String = "active",
    val allowedRole: String = "SUPER_ADMIN,DIC"
)

data class AdvertisementItem(
    val id: String = "",
    val title: String = "",
    val placement: String = "grid1",
    val code: String = "",
    val status: String = "active"
)

data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val dateFormatted: String = "",
    val targetRole: String = "ALL", // "ALL", "MPO", "AM", "RSM", "PMD", "DIC", etc.
    val type: String = "info", // "info", "urgent", "update", "notice", "offer"
    val senderName: String = "Super Admin",
    val readBy: Map<String, Boolean> = emptyMap()
)

