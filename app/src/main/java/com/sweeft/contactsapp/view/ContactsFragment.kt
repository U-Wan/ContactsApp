package com.sweeft.contactsapp.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.sweeft.contactsapp.R
import com.sweeft.contactsapp.databinding.FragmentContactsBinding
import com.sweeft.contactsapp.model.Contact
import com.sweeft.contactsapp.viewmodel.ContactsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactsFragment : Fragment() {

    private lateinit var binding: FragmentContactsBinding
    private val viewModel: ContactsViewModel by viewModels()
    private lateinit var adapter: ContactsAdapter
    private val _defaultPhotoBitmapLiveData = MutableLiveData<Bitmap?>()
    val defaultPhotoBitmapLiveData: LiveData<Bitmap?> get() = _defaultPhotoBitmapLiveData


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
            // Observe changes in the contact list
            viewModel.originalContactListLiveData.observe(viewLifecycleOwner, Observer { contacts ->
                updateContacts(contacts)
            })
            viewModel.searchQueryLiveData.observe(viewLifecycleOwner, Observer { searchQuery ->
                // Fetch contacts with the updated search query
                fetchContacts(searchQuery)
            })

            // Check and request contacts permission if necessary
            if (!viewModel.checkContactsPermission(requireContext())) {
                requestContactsPermission()
            } else {
                // Permission already granted, fetch contacts
                fetchContacts(null)
            }

        }

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_contactsFragment_to_fragmentAdd)
        }

        // Set up SearchView listener
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

    private fun updateContacts(contacts: List<Contact>) {
        adapter.submitList(contacts)
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
    private fun requestContactsPermission() {
        val requestPermissionLauncher = viewModel.requestContactsPermissionLauncher(this)
        requestPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
    }
}