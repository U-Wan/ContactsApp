package com.sweeft.contactsapp.ui

import android.Manifest
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sweeft.contactsapp.R
import com.sweeft.contactsapp.data.Contact
import java.util.ArrayList
import java.util.Locale

class ContactsFragment : Fragment() {

    private lateinit var adapter: ContactsAdapter
    private val contactList = mutableListOf<Contact>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView

    private val DEBOUNCE_DELAY = 400L
    private val debounceHandler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_contacts, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        searchView = view.findViewById(R.id.et_entered_number)
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager


        if (!checkContactsPermission()) {
            if (!hasRequestedPermissionBefore()) {
                requestContactsPermission()
                markPermissionAsRequested()
            } else {
                showPermissionDeniedDialog()
            }
        }
        else {
            fetchContacts()
            adapter = ContactsAdapter(contactList)
            recyclerView.adapter = adapter
        }

        setupSearchView()

        return view
    }
    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                debounceHandler.removeCallbacksAndMessages(null)
                debounceHandler.postDelayed(
                    { filterList(newText) },
                    DEBOUNCE_DELAY
                )
                return true
            }
        })
    }

    private fun filterList(query: String?) {

        if (query != null) {
            val filteredList = ArrayList<Contact>()
            for (i in contactList) {
                if (i.phoneNumber.lowercase(Locale.ROOT).contains(query)||i.name.lowercase(Locale.ROOT).contains(query)) {
                    filteredList.add(i)
                }
            }

            if (filteredList.isEmpty()) {
                Toast.makeText(activity, "No Data found", Toast.LENGTH_SHORT).show()
                adapter.setFilteredList(emptyList())
            } else {
                adapter.setFilteredList(filteredList)
            }
        }
    }

    private fun hasRequestedPermissionBefore(): Boolean {
        val preferences = requireActivity().getPreferences(Context.MODE_PRIVATE)
        return preferences.getBoolean("hasRequestedPermission", false)
    }

    private fun markPermissionAsRequested() {
        val preferences = requireActivity().getPreferences(Context.MODE_PRIVATE)
        preferences.edit().putBoolean("hasRequestedPermission", true).apply()
    }
    private fun checkContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private val requestContactsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                fetchContacts()
                adapter = ContactsAdapter(contactList)
                recyclerView.adapter = adapter
            } else {
                Toast.makeText(activity, "You cannot use app without permission my friend", Toast.LENGTH_LONG).show()
                requireActivity().finish()
            }
        }

    private fun requestContactsPermission() {
        requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    //dialog
    private fun showPermissionDeniedDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Permission Required")
            .setMessage("This app needs the Read Contacts permission to display contacts. Please grant the permission in the app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()

                Toast.makeText(activity, "You cannot use app without permission my friend", Toast.LENGTH_LONG).show()
                requireActivity().finish()
            }
            .create()

        dialog.show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", requireContext().packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun fetchContacts() {
        val contentResolver: ContentResolver = requireActivity().contentResolver
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
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
                val contactPhotoBitmap = getContactPhotoBitmap(contentResolver, contactId)

                contactList.add(Contact(name, phoneNumber, contactPhotoBitmap))
            }
        }
    }

    private fun getContactPhotoBitmap(contentResolver: ContentResolver, contactId: String): Bitmap? {
        val photoCursor: Cursor? = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Photo.PHOTO),
            ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE),
            null
        )

        return photoCursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO)

                if (columnIndex != -1) {
                    val photoBlob = cursor.getBlob(columnIndex)
                    if (photoBlob != null && photoBlob.isNotEmpty()) {
                        return BitmapFactory.decodeByteArray(photoBlob, 0, photoBlob.size)
                    }
                }
            }

            // If there's no valid photo data, return the default photo
            return getDefaultPhotoBitmap()
        }
    }

    private fun getDefaultPhotoBitmap(): Bitmap? {
        return BitmapFactory.decodeResource(resources, R.drawable.harold)
    }


    private fun getPhoneNumber(contentResolver: ContentResolver, contactId: String): String {
        val phoneCursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
            arrayOf(contactId),
            null
        )

        return phoneCursor?.use { phoneCursorInner ->
            if (phoneCursorInner.moveToFirst()) {
                val phoneNumberColumnIndex =
                    phoneCursorInner.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                phoneCursorInner.getString(phoneNumberColumnIndex)
            } else {
                ""
            }
        } ?: ""
    }
}
