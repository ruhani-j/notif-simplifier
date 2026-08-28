package com.example.notifsimplifier.service

object ImportantDetector {

    private val importantKeywords = listOf(
        // Delivery & ride-sharing
        "arriving",
        "arrived",
        "on the way",
        "out for delivery",
        "delivered",
        "driver",
        "rider",
        "your delivery",
        "order confirmed",
        "order placed",
        "order accepted",
        "order ready",
        "preparing your",
        "picked up",
        "en route",
        "minutes away",
        " eta ",
        "estimated arrival",
        "at your location",
        "nearby",

        // Financial / transactional
        "payment",
        "transaction",
        "charged",
        "debited",
        "credited",
        "refund",
        "transfer",
        "invoice",
        "receipt",
        "amount due",
        "payment due",
        "payment failed",
        "payment successful",
        "payment declined",

        // Security & account
        "login attempt",
        "sign-in attempt",
        "new sign-in",
        "new login",
        "suspicious",
        "unrecognized",
        "unauthorized",
        "security alert",
        "account alert",
        "password changed",
        "account locked",
        "verify your account",

        // Time-critical / appointments
        "appointment",
        "booking confirmed",
        "reservation confirmed",
        "check-in",
        "boarding",
        "gate",
        "cancelled",

        // Safety
        "emergency",
        "sos",
    )

    fun isImportant(title: String, text: String): Boolean {
        val combined = "$title $text".lowercase()
        return importantKeywords.any { it in combined }
    }
}
