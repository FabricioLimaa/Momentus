package br.com.fabriciolima.momentus.ui.cronograma

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.databinding.ItemCronogramaBinding
import java.time.format.DateTimeFormatter

// Data class para representar o estado completo de um item na UI
data class CronogramaViewItem(
    val item: ItemCronograma,
    val rotinaNome: String?,
    val isChecked: Boolean
)

class ItemCronogramaAdapter(
    private val onHabitoConcluidoListener: (String, Boolean) -> Unit
) : ListAdapter<CronogramaViewItem, ItemCronogramaAdapter.ViewHolder>(DiffCallback) {

    private val formatter = DateTimeFormatter.ofPattern("HH:mm")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCronogramaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val viewItem = getItem(position)
        holder.bind(viewItem)
    }

    fun updateData(newItens: List<ItemCronograma>, newRotinas: List<Rotina>, newHabitos: Set<String>) {
        val rotinasMap = newRotinas.associateBy { it.id }
        val viewItems = newItens.map {
            CronogramaViewItem(
                item = it,
                rotinaNome = rotinasMap[it.rotinaId]?.nome,
                isChecked = newHabitos.contains(it.id)
            )
        }
        submitList(viewItems)
    }

    inner class ViewHolder(private val binding: ItemCronogramaBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(viewItem: CronogramaViewItem) {
            val item = viewItem.item
            binding.textViewNomeRotina.text = item.titulo
            binding.textViewHorario.text = "${item.horarioInicio.format(formatter)} - ${item.horarioTermino.format(formatter)}"
            binding.textViewTag.text = viewItem.rotinaNome ?: ""

            binding.checkBoxConcluido.setOnCheckedChangeListener(null) // Evitar chamadas recursivas
            binding.checkBoxConcluido.isChecked = viewItem.isChecked

            binding.checkBoxConcluido.setOnCheckedChangeListener { _, isChecked ->
                onHabitoConcluidoListener(item.id, isChecked)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<CronogramaViewItem>() {
            override fun areItemsTheSame(oldItem: CronogramaViewItem, newItem: CronogramaViewItem): Boolean {
                return oldItem.item.id == newItem.item.id
            }

            override fun areContentsTheSame(oldItem: CronogramaViewItem, newItem: CronogramaViewItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
