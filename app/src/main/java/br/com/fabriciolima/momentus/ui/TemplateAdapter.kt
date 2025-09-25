// ARQUIVO: ui/TemplateAdapter.kt
package br.com.fabriciolima.momentus.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.fabriciolima.momentus.data.Template
import br.com.fabriciolima.momentus.databinding.ItemTemplateBinding

class TemplateAdapter(
    private val onLoadClicked: (Template) -> Unit,
    private val onDeleteClicked: (Template) -> Unit
) : ListAdapter<Template, TemplateAdapter.TemplateViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val binding = ItemTemplateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TemplateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TemplateViewHolder(private val binding: ItemTemplateBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(template: Template) {
            binding.textViewTemplateName.text = template.nome
            binding.buttonLoadTemplate.setOnClickListener { onLoadClicked(template) }
            binding.buttonDeleteTemplate.setOnClickListener { onDeleteClicked(template) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Template>() {
            override fun areItemsTheSame(oldItem: Template, newItem: Template) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Template, newItem: Template) = oldItem == newItem
        }
    }
}