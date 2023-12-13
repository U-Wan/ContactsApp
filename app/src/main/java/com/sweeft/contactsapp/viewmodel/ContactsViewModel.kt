package com.sweeft.contactsapp.viewmodel

import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sweeft.contactsapp.R
import com.sweeft.contactsapp.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactsViewModel : ViewModel() {

    private val _originalContactListLiveData = MutableLiveData<List<Contact>>()
    val originalContactListLiveData: LiveData<List<Contact>> get() = _originalContactListLiveData

    private val _searchQueryLiveData = MutableLiveData<String?>()
    val searchQueryLiveData: MutableLiveData<String?> get() = _searchQueryLiveData
    private val contactList = mutableListOf<Contact>()


    suspend fun fetchContacts(contentResolver: ContentResolver, context: Context, searchQuery: String? = null) {
        withContext(Dispatchers.IO) {
            val contactList = mutableListOf<Contact>()

            // Use the search query to filter contacts
            val cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                getSearchSelection(searchQuery),
                getSearchSelectionArgs(searchQuery),
                null
            )

            cursor?.use { contactsCursor ->
                val idIndex = contactsCursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex =
                    contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)

                while (contactsCursor.moveToNext()) {
                    val contactId = contactsCursor.getString(idIndex)
                    val name = contactsCursor.getString(nameIndex)

                    val phoneNumber = getPhoneNumber(contentResolver, contactId)
                    val contactPhotoBitmap = getContactPhotoBitmap(contentResolver, contactId, context)

                    contactList.add(Contact(name, phoneNumber, contactPhotoBitmap))
                }
            }

            cursor?.close()

            _originalContactListLiveData.postValue(contactList.toList())
        }
    }
    fun setSearchQuery(query: String?) {
        _searchQueryLiveData.value = query
    }

    private fun getSearchSelection(searchQuery: String?): String? {
        return if (!searchQuery.isNullOrBlank()) {
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?"
        } else {
            null
        }
    }

    private fun getSearchSelectionArgs(searchQuery: String?): Array<String>? {
        return if (!searchQuery.isNullOrBlank()) {
            arrayOf("%$searchQuery%")
        } else {
            null
        }
    }
    suspend fun getContactPhotoBitmap(
        contentResolver: ContentResolver,
        contactId: String,
        context: Context
    ): Bitmap? = withContext(Dispatchers.IO) {
        val photoCursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Photo.PHOTO),
            ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE),
            null
        )
        val photoBitmap = photoCursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO)
                if (columnIndex != -1) {
                    val photoBlob = cursor.getBlob(columnIndex)
                    if (photoBlob != null && photoBlob.isNotEmpty()) {
                        BitmapFactory.decodeByteArray(photoBlob, 0, photoBlob.size)
                    } else {
                        getDefaultPhotoBitmap(context) ?: error("Default photo is null")
                    }
                } else {
                    getDefaultPhotoBitmap(context) ?: error("Default photo is null")
                }
            } else {
                getDefaultPhotoBitmap(context) ?: error("Default photo is null")
            }
        }

        return@withContext photoBitmap
    }

    fun getDefaultPhotoBitmap(context: Context): Bitmap? {
        return BitmapFactory.decodeResource(context.resources, R.drawable.harold)
    }


    fun checkContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestContactsPermissionLauncher(fragment: Fragment) =
        fragment.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                viewModelScope.launch {
                    fetchContacts(fragment.requireActivity().contentResolver, fragment.requireContext())
                }
            }
        }

    fun showPermissionDeniedDialog(context: Context, fragment: Fragment) {
        val dialog = AlertDialog.Builder(context)
            .setTitle("Permission Required")
            .setMessage(context.getString(R.string.permission_message))
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings(fragment)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun openAppSettings(fragment: Fragment) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", fragment.requireContext().packageName, null)
        intent.data = uri
        fragment.startActivity(intent)
    }

    fun handleNoContactsPermission(context: Context, fragment: Fragment) {
        if (!hasRequestedPermissionBefore(context)) {
            requestContactsPermissionLauncher(fragment).launch(android.Manifest.permission.READ_CONTACTS)
            markPermissionAsRequested(context)
        } else {
            showPermissionDeniedDialog(context, fragment)
        }
    }

    private fun hasRequestedPermissionBefore(context: Context): Boolean {
        val preferences = context.getSharedPreferences("ContactsAppPrefs", Context.MODE_PRIVATE)
        return preferences.getBoolean("hasRequestedPermission", false)
    }

    private fun markPermissionAsRequested(context: Context) {
        val preferences = context.getSharedPreferences("ContactsAppPrefs", Context.MODE_PRIVATE)
        preferences.edit().putBoolean("hasRequestedPermission", true).apply()
    }

    private suspend fun getPhoneNumber(
        contentResolver: ContentResolver,
        contactId: String
    ): String = withContext(Dispatchers.IO) {
        val phoneCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
            arrayOf(contactId),
            null
        )

        val phoneNumber = phoneCursor?.use { phoneCursorInner ->
            if (phoneCursorInner.moveToFirst()) {
                val phoneNumberColumnIndex =
                    phoneCursorInner.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                phoneCursorInner.getString(phoneNumberColumnIndex)
            } else {
                ""
            }
        } ?: ""

        phoneCursor?.close()

        return@withContext phoneNumber
    }
}
