package com.sweeft.contactsapp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.sweeft.contactsapp.data.Contact
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

    fun submitFilteredList(filteredContacts: List<Contact>) {
        submitList(filteredContacts)
    }

    private fun isAnyItemExpanded(position: Int) {
        val temp = currentList.indexOfFirst {
            it.isExpandable == true
        }
        if (temp>=0&&temp != position) {
            val updatedList = currentList.toMutableList()
            updatedList[temp].isExpandable = false
            submitList(updatedList)
        }
    }

    private fun makeVoiceCall(phoneNumber: String?) {
        if (!phoneNumber.isNullOrBlank()) {
            val uri = Uri.parse("tel:$phoneNumber")
            val intent = Intent(Intent.ACTION_DIAL, uri)
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Invalid phone number", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openMessagingApp(phoneNumber: String?) {
        if (!phoneNumber.isNullOrBlank()) {
            val uri = Uri.parse("smsto:$phoneNumber")
            val intent = Intent(Intent.ACTION_SENDTO, uri)
            context.startActivity(intent)
        } else {

            Toast.makeText(context, "Invalid phone number", Toast.LENGTH_SHORT).show()
        }
    }


}

class ContactDiffCallback : DiffUtil.ItemCallback<Contact>() {
    override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
        return oldItem.phoneNumber == newItem.phoneNumber
    }

    override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
        return oldItem == newItem
    }

}




