package com.example.notifsimplifier.service

object MarketingDetector {

    private val marketingKeywords = listOf(
        "% off",
        "discount",
        "offer",
        "deal",
        "save ",
        "coupon",
        "promo",
        "voucher",
        "cashback",
        "hurry",
        "limited time",
        "expires soon",
        "last chance",
        "today only",
        "craving",
        "hungry?",
        "order now",
        "try now",
        "exclusive",
        "unlock",
        "earn ",
        "reward",
        "miss you",
        "we want you back",
        "come back",
        "it's been a while",
        "don't miss",
        "just launched",
        "trending",
        "flat rs",
        "flat inr",
        "free delivery on",
        "use code",
        "avail ",
        "get free",
        "win ",
        "surprise",
    )

    // If any of these are present the notification is transactional — never treat it as marketing.
    private val transactionalVeto = listOf(
        "arriving",
        "arrived",
        "delivered",
        "out for delivery",
        "picked up",
        "on the way",
        "driver",
        "rider",
        "order confirmed",
        "order placed",
        "order accepted",
        "preparing your",
        "cancelled",
        "refund",
        "payment",
        "tracking",
        "eta",
        "minutes away",
        "your order",
        "your delivery",
        "your driver",
        "your rider",
        "assigned",
        "otp",
        "verification",
        "code",
    )

    fun isMarketing(title: String, text: String): Boolean {
        val combined = "$title $text".lowercase()
        val hasTransactional = transactionalVeto.any { it in combined }
        if (hasTransactional) return false
        return marketingKeywords.any { it in combined }
    }
}
