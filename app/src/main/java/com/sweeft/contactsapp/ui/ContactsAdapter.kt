package com.sweeft.contactsapp.ui

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.sweeft.contactsapp.data.Contact
import com.sweeft.contactsapp.databinding.ContactBinding
import java.util.ArrayList
import java.util.Locale
import android.content.Context
import android.widget.Toast

class ContactsAdapter(private var originalContactList: List<Contact>,
                      private val context: Context
) :
    RecyclerView.Adapter<ContactViewHolder>()
//, Filterable
{

    private var contactList: List<Contact> = originalContactList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ContactBinding.inflate(inflater, parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contactList[position]
        holder.bind(contact)

        val isExpandable: Boolean = contact.isExpandable
        if (isExpandable) {
            holder.expandView()
        } else {
            holder.collapseExpandedView()
        }

        holder.setOnItemClickListener { clickedPosition ->
            isAnyItemExpanded(clickedPosition)
            contact.isExpandable = !contact.isExpandable
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
            it.isExpandable
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

    /*
    //we don not use this methods it is just for educational purposes :)
    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val pattern = constraint.toString().lowercase(Locale.getDefault())
            contactList = if (pattern.isEmpty()) {
                originalContactList
            } else {
                val filteredList = ArrayList<Contact>()
                for (contact in originalContactList) {
                    if (contact.phoneNumber.contains(pattern,ignoreCase = true) ||
                        contact.name.contains(pattern,ignoreCase = true)
                    ) {
                        filteredList.add(contact)
                    }
                }
                filteredList
            }
            val filterResults = FilterResults()
            filterResults.values = contactList
            return filterResults
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            contactList = results?.values as? List<Contact> ?: emptyList()
            notifyDataSetChanged()
        }
    }
    fun filterList(query: String?) {
        if (query != null) {
            val filteredList = ArrayList<Contact>()
            for (contact in originalContactList) {
                if (contact.phoneNumber.lowercase(Locale.ROOT).contains(query) ||
                    contact.name.lowercase(Locale.ROOT).contains(query)
                ) {
                    filteredList.add(contact)
                }
            }

            if (filteredList.isEmpty()) {
                // You can notify the UI or perform any action here if no data is found
            }
            setFilteredList(filteredList)
        }
    }
    */
}

