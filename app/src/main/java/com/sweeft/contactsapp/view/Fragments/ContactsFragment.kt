package com.sweeft.contactsapp.view.Fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.sweeft.contactsapp.R
import com.sweeft.contactsapp.databinding.FragmentContactsBinding
import com.sweeft.contactsapp.model.Contact
import com.sweeft.contactsapp.view.ContactsAdapter
import com.sweeft.contactsapp.viewmodel.ContactsViewModel
import kotlinx.coroutines.launch

class ContactsFragment : Fragment() {

    private lateinit var binding: FragmentContactsBinding
    private val viewModel: ContactsViewModel by viewModels()
    private lateinit var adapter: ContactsAdapter
    private val debounceDelay = 400L // milliseconds



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentContactsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        lifecycleScope.launch {
            viewModel.originalContactListLiveData.observe(viewLifecycleOwner, Observer { contacts ->
                updateContacts(contacts)
            })
            viewModel.searchQueryLiveData.observe(viewLifecycleOwner, Observer { searchQuery ->
                fetchContacts(searchQuery)
            })

            if (!checkContactsPermission()) {
                handleNoContactsPermission()
            } else {
                fetchContacts(null)
            }
        }


        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_contactsFragment_to_fragmentAdd)
        }

        binding.etEnteredNumber.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.setSearchQuery(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText)
                return true
            }
        })

    }
    private fun setupRecyclerView() {
        adapter = ContactsAdapter(requireContext())
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun fetchContacts(searchQuery: String?) {
        lifecycleScope.launch {
            viewModel.fetchContacts(
                requireActivity().contentResolver,
                requireContext(),
                searchQuery
            )
        }
    }

    private fun updateContacts(contacts: List<Contact>) {
        adapter.submitList(contacts)
    }

    //permission handling

    private fun handleNoContactsPermission() {
        if (!hasRequestedPermissionBefore()) {
            requestContactsPermission()
            markPermissionAsRequested()
        } else {
            showPermissionDeniedDialog()
        }
    }
    private fun requestContactsPermission() {
        requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }
    private val requestContactsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                fetchContacts(null)
            }
        }

    private fun checkContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasRequestedPermissionBefore(): Boolean {
        val preferences = requireActivity().getPreferences(Context.MODE_PRIVATE)
        return preferences.getBoolean("hasRequestedPermission", false)
    }

    private fun markPermissionAsRequested() {
        val preferences = requireActivity().getPreferences(Context.MODE_PRIVATE)
        preferences.edit().putBoolean("hasRequestedPermission", true).apply()
    }

    private fun showPermissionDeniedDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Permission Required")
            .setMessage(getString(R.string.permission_message))
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



}