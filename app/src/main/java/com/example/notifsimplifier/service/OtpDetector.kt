package com.example.notifsimplifier.service

object OtpDetector {

    // Keep keywords in one place so extending the list later is easy.
    private val keywords = listOf(
        "otp",
        "one-time",
        "one time",
        "verification code",
        "verify code",
        "security code",
        "passcode",
        "auth code",
        "authentication code",
        "login code",
        "access code",
        "confirmation code",
        "your code",
        "use code",
        "enter code",
    )

    // Standalone 4–8 digit number (e.g. "123456" but not "123456789").
    private val codePattern = Regex("""\b\d{4,8}\b""")

    fun isOtp(title: String, text: String): Boolean {
        val combined = "$title $text".lowercase()
        return keywords.any { it in combined } && codePattern.containsMatchIn(combined)
    }
}
