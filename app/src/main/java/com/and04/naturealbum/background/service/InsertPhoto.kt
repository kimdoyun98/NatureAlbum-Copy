package com.and04.naturealbum.background.service

import android.os.Parcelable
import com.and04.naturealbum.data.localdata.room.Label
import kotlinx.parcelize.Parcelize

@Parcelize
data class InsertPhoto(
    val uri: String,
    val fileName: String,
    val dateTime: String,
    val label: Label,
    val latitude: Double,
    val longitude: Double,
    val description: String
): Parcelable
