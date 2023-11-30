package com.sweeft.contactsapp.ui

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sweeft.contactsapp.data.Contact
import com.sweeft.contactsapp.databinding.ContactBinding
import android.content.Context
import android.widget.Toast

class ContactsAdapter(private var originalContactList: List<Contact>, private val context: Context) :
    RecyclerView.Adapter<ContactViewHolder>() {
    private var contactList: List<Contact> = originalContactList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ContactBinding.inflate(inflater, parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contactList[position]
        holder.bind(contact)

        val isExpandable: Boolean = contact.isExpandable == true
        if (isExpandable) {
            holder.expandView()
        } else {
            holder.collapseExpandedView()
        }

        holder.setOnItemClickListener { clickedPosition ->
            isAnyItemExpanded(clickedPosition)
            contact.isExpandable = contact.isExpandable
            notifyItemChanged(clickedPosition, Unit)
        }

        holder.setOnButtonCallClickListener { clickedPosition ->
            makeVoiceCall(contact.phoneNumber)
        }

        holder.setOnButtonMessageClickListener { clickedPosition ->
            openMessagingApp(contact.phoneNumber)
        }
    }

    private fun isAnyItemExpanded(position: Int){
        val temp = originalContactList.indexOfFirst {
            it.isExpandable == true
        }
        if (temp >= 0 && temp != position){
            originalContactList[temp].isExpandable = false
            notifyItemChanged(temp , 0)
        }
    }

    override fun getItemCount(): Int {
        return contactList.size
    }

    fun setFilteredList(filteredContactList: List<Contact>) {
        contactList = filteredContactList
        notifyDataSetChanged()
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

