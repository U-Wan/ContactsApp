package com.sweeft.contactsapp.ui

import androidx.recyclerview.widget.RecyclerView
import com.sweeft.contactsapp.data.Contact
import com.sweeft.contactsapp.databinding.ContactBinding

class ContactViewHolder(private val binding: ContactBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(contact: Contact) {
        binding.tvName.text = contact.name
        binding.tvPhone.text = contact.phoneNumber
        binding.personPhoto.setImageBitmap(contact.photo)
    }
}
