package com.sweeft.contactsapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView


class ContactsAdapter(private var contactList: List<Contact>) :
    RecyclerView.Adapter<ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.contact,
            parent,
            false
        )
        return ContactViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contactList[position]

        holder.bind(contact)
    }

    override fun getItemCount(): Int {
        return contactList.size
    }


    // Update the data and notife adapter
    fun updateData(newContactList: List<Contact>) {
        contactList = newContactList
        notifyDataSetChanged()
    }

}


