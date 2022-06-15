package com.arduinoworld.smartremote

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arduinoworld.smartremote.databinding.LightRecyclerViewItemBinding

@Suppress("DEPRECATION")
@SuppressLint("SetTextI18n")
class LightRecyclerAdapter(
        private var lightRecyclerAdapterList: List<Light>
):
    RecyclerView.Adapter<LightRecyclerAdapter.ViewHolder>() {

    private lateinit var clickListener : OnItemClickListener

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val binding = LightRecyclerViewItemBinding
                .inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        return ViewHolder(binding, clickListener)
    }

    class ViewHolder(val binding: LightRecyclerViewItemBinding, listener : OnItemClickListener) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION)
                listener.onItemClick(position)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(lightRecyclerAdapterList[position]) {
                binding.labelRemoteName.text = remoteName
                binding.labelColorTemperature.text = "${colorTemperature}K"
                if (brightness != 0) {
                    binding.labelBrightness.text = "$brightness%"
                } else {
                    binding.labelBrightness.text = "Выкл"
                }
            }
        }
    }

    override fun getItemCount() = lightRecyclerAdapterList.size

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener : OnItemClickListener) {
        clickListener = listener
    }
}