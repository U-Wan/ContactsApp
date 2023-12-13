package com.sweeft.contactsapp.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.sweeft.contactsapp.model.Contact
import com.sweeft.contactsapp.databinding.ContactBinding
import android.widget.Toast

class ContactsAdapter(private val context: Context) :
    ListAdapter<Contact, ContactViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ContactBinding.inflate(inflater, parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = getItem(position)
        holder.bind(contact)

        val isExpandable: Boolean = contact.isExpandable == true
        if (isExpandable) {
            holder.expandView()
        } else {
            holder.collapseExpandedView()
        }

        holder.setOnItemClickListener { clickedPosition ->
            isAnyItemExpanded(clickedPosition)
            contact.isExpandable = !contact.isExpandable!!
            notifyItemChanged(clickedPosition, Unit)
        }

        holder.setOnButtonCallClickListener {
            makeVoiceCall(contact.phoneNumber)
        }

        holder.setOnButtonMessageClickListener {
            openMessagingApp(contact.phoneNumber)
        }
    }

    private fun makeVoiceCall(phoneNumber: String?) {
        if (!phoneNumber.isNullOrBlank()) {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            context.startActivity(dialIntent)
        } else {
            showToast("Invalid phone number")
        }
    }

    private fun openMessagingApp(phoneNumber: String?) {
        if (!phoneNumber.isNullOrBlank()) {
            val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber"))
            context.startActivity(smsIntent)
        } else {
            showToast("Invalid phone number")
        }
    }

    private fun isAnyItemExpanded(clickedPosition: Int) {
        val temp = currentList.indexOfFirst {
            it.isExpandable == true
        }
        if (temp >= 0 && temp != clickedPosition) {
            val updatedList = currentList.toMutableList()
            updatedList[temp].isExpandable = false
            submitList(updatedList)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}







