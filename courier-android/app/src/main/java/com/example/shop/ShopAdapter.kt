package com.example.shop

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.shop.databinding.ItemShopBinding

class ShopAdapter(private val onShopCheckedChangeListener: OnShopCheckedChangeListener) :
    RecyclerView.Adapter<ShopAdapter.ShopViewHolder>() {
    inner class ShopViewHolder(val binding: ItemShopBinding) : RecyclerView.ViewHolder(binding.root)

    private val diffCallback = object : DiffUtil.ItemCallback<Shop>() {
        override fun areContentsTheSame(oldItem: Shop, newItem: Shop): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areItemsTheSame(oldItem: Shop, newItem: Shop): Boolean {
            return oldItem.id == newItem.id
        }
    }

    private val differ = AsyncListDiffer(this, diffCallback)

    var shops: List<Shop>
        get() = differ.currentList
        set(value) {
            differ.submitList(value)
        }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        return ShopViewHolder(
            ItemShopBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        )
    }

    override fun getItemCount(): Int {
        return shops.count()
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        holder.binding.apply {
            val shop = shops[position]
            tvId.text = "ID: " + shop.id.toString()
            tvName.text = "Shop name: " + shop.name
            tvPhone.text = "Phone: " + shop.phone
            tvContact.text = "Contact person: " + shop.contact_person
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                onShopCheckedChangeListener.onShopChecked(shop, isChecked)
            }
        }
    }

    interface OnShopCheckedChangeListener {
        fun onShopChecked(shop: Shop, isChecked: Boolean)
    }
}