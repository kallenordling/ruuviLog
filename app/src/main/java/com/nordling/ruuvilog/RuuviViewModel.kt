package com.nordling.ruuvilog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RuuviViewModel : ViewModel() {

    private val tagsMap = mutableMapOf<String, RuuviTag>()
    private val _tags = MutableLiveData<List<RuuviTag>>(emptyList())
    val tags: LiveData<List<RuuviTag>> = _tags

    private val _scanning = MutableLiveData(false)
    val scanning: LiveData<Boolean> = _scanning

    fun updateTag(tag: RuuviTag) {
        tagsMap[tag.mac] = tag
        _tags.postValue(tagsMap.values.sortedByDescending { it.rssi })
    }

    fun setScanning(active: Boolean) {
        _scanning.postValue(active)
    }

    fun clearTags() {
        tagsMap.clear()
        _tags.postValue(emptyList())
    }
}
