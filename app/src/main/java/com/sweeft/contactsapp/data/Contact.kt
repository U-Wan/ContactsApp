package com.sweeft.contactsapp.data

import android.graphics.Bitmap

data class Contact(
    val name: String,
    val phoneNumber: String,
    val photo: Bitmap?,
    var isExpandable: Boolean = false
)
