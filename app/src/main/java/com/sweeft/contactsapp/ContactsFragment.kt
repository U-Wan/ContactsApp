package com.sweeft.contactsapp

import android.Manifest
import android.app.AlertDialog
import android.content.ContentResolver
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
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class ContactsFragment : Fragment() {

    private lateinit var adapter: ContactsAdapter
    private val contactList = mutableListOf<Contact>()
    private lateinit var recyclerView: RecyclerView

    //
    private val DEBOUNCE_DELAY = 300L

    private val debounceHandler = Handler(Looper.getMainLooper())
    private val debounceRunnable = Runnable { applySearchFilter() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_contacts, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)



        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager

        if (checkContactsPermission()) {
            fetchContacts()
            adapter = ContactsAdapter(contactList)
            recyclerView.adapter = adapter
        } else {
            requestContactsPermission()
        }

        setupSearchFunctionality() // Add this line to set up the search functionality

        return view
    }

    private fun checkContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestContactsPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.READ_CONTACTS),
            REQUEST_READ_CONTACTS
        )
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
            val nameIndex = contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)

            while (contactsCursor.moveToNext()) {
                val contactId = contactsCursor.getString(idIndex)
                val name = contactsCursor.getString(nameIndex)

                val phoneNumber = getPhoneNumber(contentResolver, contactId)
                val photoBitmap = getContactPhoto(contentResolver, contactId)

                contactList.add(Contact(name, phoneNumber, photoBitmap))
            }
        }
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

    private fun getContactPhoto(contentResolver: ContentResolver, contactId: String): Bitmap? {
        val photoUri = getPhotoUri(contentResolver, contactId)

        return photoUri?.let {
            BitmapFactory.decodeStream(contentResolver.openInputStream(it))
        }
    }

    private fun getPhotoUri(contentResolver: ContentResolver, contactId: String): Uri? {
        val photoCursor: Cursor? = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Photo.PHOTO_URI),
            ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE),
            null
        )

        return photoCursor?.use { photoCursorInner ->
            if (photoCursorInner.moveToFirst()) {
                Uri.parse(photoCursorInner.getString(0))
            } else {
                null
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Permission Required")
            .setMessage("This app needs the Read Contacts permission to display contacts. Please grant the permission in the app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_READ_CONTACTS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, fetch and display contacts
                    fetchContacts()
                    adapter.updateData(contactList)
                } else {
                    // Permission denied, show a dialog
                    showPermissionDeniedDialog()
                }
            }
        }
    }



    private fun setupSearchFunctionality() {
        val etSearch = view?.findViewById<EditText>(R.id.et_entered_number)
        etSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // Not needed for this example
            }

            override fun onTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // Not needed for this example
            }

            override fun afterTextChanged(editable: Editable?) {
                debounceHandler.removeCallbacks(debounceRunnable)
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY)
            }
        })
    }
    private fun applySearchFilter() {
        val etSearch = view?.findViewById<EditText>(R.id.et_entered_number)
        val query = etSearch?.text.toString().toLowerCase(Locale.getDefault())

        val filteredList = contactList.filter {
            it.name.toLowerCase(Locale.getDefault()).contains(query) ||
                    it.phoneNumber.toLowerCase(Locale.getDefault()).contains(query)
        }

        adapter.updateData(filteredList)
    }



    companion object {
        private const val REQUEST_READ_CONTACTS = 123
    }
}
