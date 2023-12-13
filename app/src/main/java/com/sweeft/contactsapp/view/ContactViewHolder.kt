package com.sweeft.contactsapp.view

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.sweeft.contactsapp.model.Contact
import com.sweeft.contactsapp.databinding.ContactBinding

class ContactViewHolder(private val binding: ContactBinding) :
    RecyclerView.ViewHolder(binding.root) {

    private val bottomMenuView = binding.bottomMenu
    private val layout = binding.bottomconstarintlayout
    private val btnCall = binding.btnCall
    private val btnMessage = binding.btnMessage

    fun bind(contact: Contact) {
        binding.tvName.text = contact.name
        binding.tvPhone.text = contact.phoneNumber
        binding.personPhoto.setImageBitmap(contact.photo)
    }

    fun setOnItemClickListener(onItemClickListener: (Int) -> Unit) {
        layout.setOnClickListener {
            onItemClickListener(adapterPosition)
        }
    }

    fun expandView() {
        bottomMenuView.visibility = View.VISIBLE
    }

    fun setOnButtonCallClickListener(onButtonCallClickListener: (Int) -> Unit) {
        btnCall.setOnClickListener {
            onButtonCallClickListener(adapterPosition)
        }
    }

    fun setOnButtonMessageClickListener(onButtonMessageClickListener: (Int) -> Unit) {
        btnMessage.setOnClickListener {
            onButtonMessageClickListener(adapterPosition)
        }
    }

    fun collapseExpandedView(){
        binding.bottomMenu.visibility = View.GONE
    }

}