package com.sweeft.contactsapp

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val tvName: TextView = itemView.findViewById(R.id.tv_name)
    private val tvPhone: TextView = itemView.findViewById(R.id.tv_phone)
    private val personPhoto: ImageView = itemView.findViewById(R.id.person_photo)

    fun bind(contact: Contact) {
        tvName.text = contact.name
        tvPhone.text = contact.phoneNumber
        personPhoto.setImageBitmap(contact.photo)
    }
}

