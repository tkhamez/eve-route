package net.tkhamez.everoute

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class Token {

    fun getAccessToken(eveAuth: Map<String, Any?>): String {
        val df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        df.timeZone = TimeZone.getTimeZone("UTC");
        val expiresOn = df.parse(eveAuth["expiresOn"].toString())
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        return if (expiresOn.time - now.timeInMillis <= 60 * 1000) { // TODO
            refreshToken()
        } else {
            eveAuth["accessToken"].toString()
        }
    }

    private fun refreshToken(): String {

        // TODO implement

        return ""
    }
}
