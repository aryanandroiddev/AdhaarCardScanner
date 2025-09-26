package dev.arya.adhaarcardscanner.utils

object AadhaarUtils {

    private val aadhaarRegex = Regex("\\b\\d{12}\\b")

    fun findAadhaar(input: String?): String? {
        if (input.isNullOrEmpty()) return null
        val compact = input.replace(" ", "")
        val matchCompact = aadhaarRegex.find(compact)
        if (matchCompact != null) return matchCompact.value
        val match = aadhaarRegex.find(input)
        return match?.value
    }

    fun format(aadhaar: String): String {
        if (aadhaar.length != 12) return aadhaar
        return aadhaar.chunked(4).joinToString(" ")
    }
}