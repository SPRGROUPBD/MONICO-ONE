package com.example.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

data class IconSuggestion(
    val code: String,
    val label: String,
    val icon: ImageVector
)

object IconUtils {

    val COMMON_SUGGESTIONS = listOf(
        IconSuggestion("fa-cart-shopping", "Order Entry", Icons.Default.ShoppingCart),
        IconSuggestion("fa-briefcase", "Work / Office", Icons.Default.BusinessCenter),
        IconSuggestion("fa-file-invoice", "Bill / Invoice", Icons.Default.ReceiptLong),
        IconSuggestion("fa-check", "DCC / Check", Icons.Default.CheckCircle),
        IconSuggestion("fa-route", "Tour / Route", Icons.Default.NearMe),
        IconSuggestion("fa-truck", "Depot / Delivery", Icons.Default.LocalShipping),
        IconSuggestion("fa-calendar", "Attendance", Icons.Default.CalendarMonth),
        IconSuggestion("fa-money-bill", "Salary / Payment", Icons.Default.AttachMoney),
        IconSuggestion("fa-chart-line", "Sales Report", Icons.Default.BarChart),
        IconSuggestion("fa-hospital", "Medical / Doctor", Icons.Default.MedicalServices),
        IconSuggestion("fa-box", "Sample / Stock", Icons.Default.Inventory2),
        IconSuggestion("fa-globe", "Web Portal", Icons.Default.Language)
    )

    fun getIconForName(iconName: String): ImageVector {
        if (iconName.isBlank()) return Icons.Default.Apps

        val clean = iconName.trim().lowercase()
            .replace("fa-solid", "")
            .replace("fa-regular", "")
            .replace("fa-brands", "")
            .replace("fas ", "")
            .replace("far ", "")
            .replace("fab ", "")
            .replace("fa-", "")
            .replace("-", "")
            .replace("_", "")
            .trim()

        return when {
            // Cart & Orders
            clean.contains("cart") || clean.contains("order") || clean.contains("shop") ||
                clean.contains("basket") || clean.contains("market") || clean.contains("buy") -> Icons.Default.ShoppingCart

            // Briefcase & Work
            clean.contains("briefcase") || clean.contains("work") || clean.contains("job") ||
                clean.contains("business") || clean.contains("office") || clean.contains("case") ||
                clean.contains("portfolio") || clean.contains("duty") -> Icons.Default.BusinessCenter

            // Invoice, Bill & Receipts
            clean.contains("invoice") || clean.contains("receipt") || clean.contains("bill") ||
                clean.contains("voucher") || clean.contains("statement") || clean.contains("quote") -> Icons.Default.ReceiptLong

            // Verification & Checks (DCC)
            clean.contains("dcc") || clean.contains("check") || clean.contains("done") ||
                clean.contains("verify") || clean.contains("success") || clean.contains("tick") ||
                clean.contains("ok") -> Icons.Default.CheckCircle

            // Calendar & Attendance
            clean.contains("calendar") || clean.contains("date") || clean.contains("month") ||
                clean.contains("schedule") || clean.contains("day") || clean.contains("att") ||
                clean.contains("event") -> Icons.Default.CalendarMonth

            // Money & Payments
            clean.contains("wallet") -> Icons.Default.AccountBalanceWallet
            clean.contains("money") || clean.contains("dollar") || clean.contains("cash") ||
                clean.contains("currency") || clean.contains("fund") || clean.contains("pay") ||
                clean.contains("coin") || clean.contains("taka") -> Icons.Default.AttachMoney

            // User & Profile
            clean.contains("users") || clean.contains("group") || clean.contains("team") -> Icons.Default.Group
            clean.contains("user") || clean.contains("person") || clean.contains("profile") ||
                clean.contains("member") || clean.contains("account") || clean.contains("emp") ||
                clean.contains("client") || clean.contains("admin") -> Icons.Default.Person

            // Reports & Charts
            clean.contains("chart") || clean.contains("report") || clean.contains("analytic") ||
                clean.contains("stat") || clean.contains("graph") || clean.contains("trend") ||
                clean.contains("sale") -> Icons.Default.BarChart

            // PDF & Docs
            clean.contains("pdf") || clean.contains("document") || clean.contains("file") ||
                clean.contains("doc") || clean.contains("sheet") -> Icons.Default.PictureAsPdf

            // Announce, Bell & Notices
            clean.contains("bullhorn") || clean.contains("bell") || clean.contains("notice") ||
                clean.contains("alert") || clean.contains("announce") || clean.contains("promo") ||
                clean.contains("alarm") -> Icons.Default.Campaign

            // Transport, Delivery & Trucks
            clean.contains("truck") || clean.contains("delivery") || clean.contains("shipping") ||
                clean.contains("transport") || clean.contains("courier") || clean.contains("fleet") ||
                clean.contains("vehicle") -> Icons.Default.LocalShipping

            // Tour, Route & Maps
            clean.contains("route") || clean.contains("tour") || clean.contains("navigate") ||
                clean.contains("map") || clean.contains("location") || clean.contains("gps") ||
                clean.contains("pin") || clean.contains("place") || clean.contains("trip") -> Icons.Default.NearMe

            // Healthcare, Pharma & Medical
            clean.contains("hospital") || clean.contains("clinic") || clean.contains("medical") ||
                clean.contains("pharma") || clean.contains("medicine") || clean.contains("pill") ||
                clean.contains("doctor") || clean.contains("health") || clean.contains("drug") ||
                clean.contains("steth") || clean.contains("capsule") -> Icons.Default.MedicalServices

            // Inventory, Warehouse & Stock
            clean.contains("box") || clean.contains("package") || clean.contains("warehouse") ||
                clean.contains("stock") || clean.contains("inventory") || clean.contains("parcel") ||
                clean.contains("depot") -> Icons.Default.Inventory2

            // Settings, Config & Tools
            clean.contains("gear") || clean.contains("setting") || clean.contains("tool") ||
                clean.contains("config") || clean.contains("wrench") -> Icons.Default.Settings

            // Web, Link & Globe
            clean.contains("globe") || clean.contains("link") || clean.contains("web") ||
                clean.contains("site") || clean.contains("url") || clean.contains("net") ||
                clean.contains("browser") || clean.contains("online") -> Icons.Default.Language

            // Communication & Phone
            clean.contains("phone") || clean.contains("call") || clean.contains("contact") ||
                clean.contains("support") || clean.contains("headset") || clean.contains("tel") -> Icons.Default.Phone
            clean.contains("email") || clean.contains("mail") || clean.contains("inbox") ||
                clean.contains("msg") || clean.contains("letter") -> Icons.Default.Email

            // Security & Passwords
            clean.contains("lock") || clean.contains("security") || clean.contains("shield") ||
                clean.contains("safe") || clean.contains("protect") || clean.contains("auth") ||
                clean.contains("key") -> Icons.Default.Security

            // Printing
            clean.contains("print") -> Icons.Default.Print

            // Tasks, DCC Checklist & Assignment
            clean.contains("task") || clean.contains("todo") || clean.contains("list") ||
                clean.contains("plan") || clean.contains("assign") -> Icons.Default.Assignment

            // Building & Company HQ
            clean.contains("building") || clean.contains("company") || clean.contains("hq") ||
                clean.contains("branch") || clean.contains("center") -> Icons.Default.Apartment

            // Star & Favorites
            clean.contains("star") || clean.contains("award") || clean.contains("badge") ||
                clean.contains("medal") || clean.contains("fav") -> Icons.Default.Star
            clean.contains("heart") || clean.contains("love") -> Icons.Default.Favorite

            // Folders
            clean.contains("folder") || clean.contains("dir") -> Icons.Default.Folder

            // Modern standard fallback
            else -> Icons.Default.Apps
        }
    }
}
