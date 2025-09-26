package dev.arya.adhaarcardscanner.ui

sealed class ScanStatus {
    object Scanning : ScanStatus()
    object Success : ScanStatus()
    object Error : ScanStatus()
}
