package com.sweeft.contactsapp.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.ContactsContract
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sweeft.contactsapp.R
import com.sweeft.contactsapp.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsViewModel : ViewModel() {
    private var isContactsFetched = false

    private val _searchQueryLiveData = MutableLiveData<String?>()
    val searchQueryLiveData: MutableLiveData<String?> get() = _searchQueryLiveData

    private val _originalContactListLiveData = MutableLiveData<List<Contact>>()
    val originalContactListLiveData: LiveData<List<Contact>> get() = _originalContactListLiveData

    private val contactList = mutableListOf<Contact>()



    suspend fun fetchContacts(contentResolver: ContentResolver, context: Context, searchQuery: String? = null) {
        withContext(Dispatchers.IO) {
            contactList.clear()

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

            // Update the originalContactListLiveData with the filtered list
            val filteredContacts = filterContacts(contactList)
            _originalContactListLiveData.postValue(filteredContacts.toList())
        }
    }

    fun setSearchQuery(query: String?) {
        _searchQueryLiveData.value = query
    }


    private fun filterContacts(contacts: List<Contact>): List<Contact> {
        val searchQuery = _searchQueryLiveData.value
        return if (!searchQuery.isNullOrBlank()) {
            contacts.filter { contact ->
                contact.name?.contains(searchQuery, ignoreCase = true) == true ||
                        contact.phoneNumber?.contains(searchQuery, ignoreCase = true) == true
            }
        } else {
            contacts
        }
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

    fun getDefaultPhotoBitmap(context: Context): Bitmap? {
        return BitmapFactory.decodeResource(context.resources, R.drawable.harold)
    }




}
