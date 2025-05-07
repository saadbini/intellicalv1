package com.saza.intellical.adapters

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.saza.intellical.databinding.CheckableColorButtonBinding
import com.saza.commons.extensions.applyColorFilter
import com.saza.intellical.R

class CheckableColorAdapter(private val activity: Activity, private val colors: IntArray, var currentColor: Int, val callback: (color: Int) -> Unit) :
    RecyclerView.Adapter<CheckableColorAdapter.CheckableColorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckableColorViewHolder {
        return CheckableColorViewHolder(
            binding = CheckableColorButtonBinding.inflate(activity.layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: CheckableColorViewHolder, position: Int) {
        val color = colors[position]
        holder.bindView(color = color, checked = color == currentColor)
    }

    override fun getItemCount() = colors.size

    private fun updateSelection(color: Int) {
        currentColor = color
        callback(color)
    }

    inner class CheckableColorViewHolder(val binding: CheckableColorButtonBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindView(color: Int, checked: Boolean) {
            binding.checkableColorButton.apply {
                backgroundTintList = ColorStateList.valueOf(color)
                setOnClickListener {
                    updateSelection(color)
                }

                if (checked) {
                    setImageResource(R.drawable.ic_check_vector)
                    applyColorFilter(Color.WHITE)
                } else {
                    setImageDrawable(null)
                }
            }
        }
    }
}
