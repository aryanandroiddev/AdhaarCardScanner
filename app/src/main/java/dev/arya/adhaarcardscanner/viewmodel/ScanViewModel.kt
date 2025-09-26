package dev.arya.adhaarcardscanner.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ScanViewModel : ViewModel() {

    private val _scanning = MutableLiveData<Boolean>(false)
    val scanning: LiveData<Boolean> = _scanning

    private val _aadhaarResult = MutableLiveData<String?>()
    val aadhaarResult: LiveData<String?> = _aadhaarResult

    private val _errorMsg = MutableLiveData<String?>()
    val errorMsg: LiveData<String?> = _errorMsg

    var isProcessing: Boolean = false
        private set

    fun startScanning() {
        _scanning.postValue(true)
        _errorMsg.postValue(null)
        _aadhaarResult.postValue(null)
        isProcessing = false
    }

    fun setProcessing(processing: Boolean) {
        isProcessing = processing
    }

    fun setAadhaar(aadhaar: String) {
        _aadhaarResult.postValue(aadhaar)
        _errorMsg.postValue(null)
        _scanning.postValue(false)
    }

    fun setError(msg: String) {
        _errorMsg.postValue(msg)
        _aadhaarResult.postValue(null)
        _scanning.postValue(false)
    }
}