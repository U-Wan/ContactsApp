package com.sweeft.contactsapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sweeft.contactsapp.databinding.ContactBinding


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

        // Set the data to views in the ViewHolder
        holder.bind(contact)
    }

    override fun getItemCount(): Int {
        return contactList.size
    }


    fun updateData(newContactList: List<Contact>) {
        contactList = newContactList
        notifyDataSetChanged()
    }

}

