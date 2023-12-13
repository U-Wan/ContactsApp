/*
package com.sweeft.contactsapp.view

package com.sweeft.contactsapp.view

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
import android.provider.ContactsContract
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sweeft.contactsapp.R
import com.sweeft.contactsapp.model.Contact
import com.sweeft.contactsapp.databinding.FragmentContactsBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ContactsFragment : Fragment() {
    private lateinit var binding: FragmentContactsBinding
    private lateinit var adapter: ContactsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var noDataImageView: ImageView
    private lateinit var actionButton:FloatingActionButton

    private val contactList = mutableListOf<Contact>()
    private lateinit var originalContactList: List<Contact>
    private var filteredContactList: List<Contact> = mutableListOf()

    private val debounceDelay = 400L // milliseconds

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentContactsBinding.inflate(inflater, container, false)
        val view = binding.root

        recyclerView = binding.recyclerView
        searchView = binding.etEnteredNumber
        noDataImageView = binding.noDataImageView

        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager


        setupSearchView()


        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_contactsFragment_to_fragmentAdd)

        }
    }

    private fun handleNoContactsPermission() {
        if (!hasRequestedPermissionBefore()) {
            requestContactsPermission()
            markPermissionAsRequested()
        } else {
            showPermissionDeniedDialog()
        }
    }
    private fun fetchAndDisplayContacts() {
        fetchContacts()
        originalContactList = contactList.toList()
        initializeAdapter()
    }
    private fun fetchContacts() {
        contactList.clear()

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
        cursor?.close()
    }


    private fun getDefaultPhotoBitmap(): Bitmap? {
        return BitmapFactory.decodeResource(resources, R.drawable.harold)
    }

    private fun initializeAdapter() {
        adapter = ContactsAdapter(requireContext())
        recyclerView.adapter = adapter
        adapter.submitList(originalContactList)
    }
    override fun onStart() {
        super.onStart()
        if (!checkContactsPermission()) {
            handleNoContactsPermission()
        } else {
            if (contactList.isEmpty() && checkContactsPermission()) {
                fetchAndDisplayContacts()
            }
        }
    }

    private fun showPlaceholder() {
        recyclerView.visibility = View.GONE
        noDataImageView.visibility = View.VISIBLE
    }

    private fun hidePlaceholder() {
        recyclerView.visibility = View.VISIBLE
        noDataImageView.visibility = View.GONE
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
                originalContactList = contactList.toList() // Update the original list
                if (::adapter.isInitialized) {
                    adapter.submitList(originalContactList)
                } else {
                    initializeAdapter()
                }
            } else {
                requireView().showToast("You should accept permissions my friend")
            }
        }


    private fun requestContactsPermission() {
        requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    // Dialog
    private fun showPermissionDeniedDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Permission Required")
            .setMessage(getString(R.string.permission_message))
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                requireView().showToast("You cannot use app without permission my friend")
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


    private fun updateFilteredContacts(query: String?) {
        filteredContactList = originalContactList.filter { contact ->
            val sanitizedQuery = query.orEmpty().replace("\\s".toRegex(), "")
            val sanitizedPhoneNumber = contact.phoneNumber?.replace("\\s".toRegex(), "")
            val sanitizedName = contact.name?.replace("\\s".toRegex(), "")

            sanitizedPhoneNumber?.contains(sanitizedQuery, ignoreCase = true) == true ||
                    sanitizedName?.contains(sanitizedQuery, ignoreCase = true) == true
        }

        if (::adapter.isInitialized) {
            adapter.submitFilteredList(filteredContactList)
        }

        if (filteredContactList.isEmpty()) {
            showPlaceholder()
        } else {
            hidePlaceholder()
        }
    }

    private fun getContactPhotoBitmap(
        contentResolver: ContentResolver,
        contactId: String
    ): Bitmap? {
        val photoCursor: Cursor? = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Photo.PHOTO),
            ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE),
            null
        )

        return photoCursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO)

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


    private fun setupSearchView() {
        var job: Job? = null

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                job?.cancel()

                job = lifecycleScope.launch {
                    delay(debounceDelay)
                    updateFilteredContacts(newText)
                }

                return true
            }
        })
    }

}
fun View.showToast(
    message: CharSequence) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
*/
