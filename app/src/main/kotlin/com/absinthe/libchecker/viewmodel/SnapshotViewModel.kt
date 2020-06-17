package com.absinthe.libchecker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.bean.ARMV7
import com.absinthe.libchecker.bean.ARMV8
import com.absinthe.libchecker.bean.NO_LIBS
import com.absinthe.libchecker.bean.SnapshotDiffItem
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.LCDatabase
import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.database.SnapshotItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SnapshotViewModel(application: Application) : AndroidViewModel(application) {

    val timestamp: MutableLiveData<Long> = MutableLiveData(GlobalValues.snapshotTimestamp)
    val snapshotItems: LiveData<List<SnapshotItem>>
    val snapshotDiffItems: MutableLiveData<List<SnapshotDiffItem>> = MutableLiveData()

    private val repository: LCRepository

    init {
        val lcDao = LCDatabase.getDatabase(application).lcDao()
        repository = LCRepository(lcDao)
        snapshotItems = lcDao.getSnapshots()
    }

    fun computeSnapshots() = viewModelScope.launch(Dispatchers.IO) {

        deleteAllSnapshots()
        val dbList = mutableListOf<SnapshotItem>()

        dbList.add(
            SnapshotItem(
                BuildConfig.APPLICATION_ID,
                "LibChecker",
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE.toLong(),
                0L, 0L,
                false,
                0,
                "", "", "", "", ""
            )
        )

        insertSnapshots(dbList)
        withContext(Dispatchers.Main) {
            val ts = System.currentTimeMillis()
            GlobalValues.snapshotTimestamp = ts
            timestamp.value = ts
        }
    }

    fun computeDiff() = viewModelScope.launch(Dispatchers.IO) {
        val diffList = mutableListOf<SnapshotDiffItem>()

        diffList.add(
            SnapshotDiffItem(
                BuildConfig.APPLICATION_ID,
                SnapshotDiffItem.DiffNode("LibChecker", "New Name"),
                SnapshotDiffItem.DiffNode(BuildConfig.VERSION_NAME),
                SnapshotDiffItem.DiffNode(BuildConfig.VERSION_CODE.toLong()),
                SnapshotDiffItem.DiffNode(NO_LIBS.toShort()),
                SnapshotDiffItem.DiffNode(""),
                SnapshotDiffItem.DiffNode(""),
                SnapshotDiffItem.DiffNode(""),
                SnapshotDiffItem.DiffNode(""),
                SnapshotDiffItem.DiffNode(""),
                added = true
            )
        )
        diffList.add(
            SnapshotDiffItem(
                "com.absinthe.anywhere_",
                SnapshotDiffItem.DiffNode("Anywhere-"),
                SnapshotDiffItem.DiffNode("2.0.0-beta5", "2.0.0-beta6"),
                SnapshotDiffItem.DiffNode(20006, 20007),
                SnapshotDiffItem.DiffNode(ARMV7.toShort(), ARMV8.toShort()),
                SnapshotDiffItem.DiffNode(""),
                SnapshotDiffItem.DiffNode(""),
                SnapshotDiffItem.DiffNode(""),
                SnapshotDiffItem.DiffNode(""),
                SnapshotDiffItem.DiffNode(""),
                true, removed = true
            )
        )

        withContext(Dispatchers.Main) {
            snapshotDiffItems.value = diffList
        }
    }

    fun insertSnapshots(items: List<SnapshotItem>) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertSnapshots(items)
    }

    fun deleteAllSnapshots() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAllSnapshots()
    }
}