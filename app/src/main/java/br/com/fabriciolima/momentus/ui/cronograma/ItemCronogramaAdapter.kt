// ARQUIVO: ui/cronograma/ItemCronogramaAdapter.kt (CÓDIGO COMPLETO)
package br.com.fabriciolima.momentus.ui.cronograma

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.fabriciolima.momentus.data.* // Wildcard
import br.com.fabriciolima.momentus.databinding.ItemCronogramaBinding
import java.util.concurrent.TimeUnit

// 1. MODIFICAÇÃO: O adapter agora espera um listener para o CheckBox.
class ItemCronogramaAdapter(
    private val onItemClicked: (ItemCronograma) -> Unit,
    private val onCheckedChange: (ItemCronograma, Boolean) -> Unit
) : ListAdapter<ItemCronogramaCompletado, ItemCronogramaAdapter.ItemViewHolder>(DiffCallback) {

    private var rotinasMap = emptyMap<String, Rotina>()

    // --- MODIFICAÇÃO TERMINA AQUI ---

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemCronogramaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val itemCompletado = getItem(position)
        val rotinaCorrespondente = rotinasMap[itemCompletado.item.rotinaId]
        holder.bind(itemCompletado, rotinaCorrespondente)
    }

    // ... (getItemAt agora usa .item para pegar o ItemCronograma)
    fun getItemAt(position: Int): ItemCronograma {
        return getItem(position).item
    }

    // --- MODIFICAÇÃO INICIA AQUI ---
    // 4. Em vez de 'setData', temos um método separado para atualizar o mapa de rotinas.
    //    A lista de itens será atualizada pelo 'submitList'.
    fun setRotinas(todasAsRotinas: List<Rotina>) {
        this.rotinasMap = todasAsRotinas.associateBy { it.id }
        notifyDataSetChanged() // Usamos aqui para garantir que os nomes sejam atualizados se uma rotina for editada.
    }
    // --- MODIFICAÇÃO TERMINA AQUI ---

    inner class ItemViewHolder(private val binding: ItemCronogramaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(itemCompletado: ItemCronogramaCompletado, rotina: Rotina?) {
            val item = itemCompletado.item

            // Preenche os campos com os novos dados
            binding.textViewNomeRotina.text = rotina?.nome ?: "Rotina desconhecida"

            // Calcula o horário de término
            val fim = item.horarioInicio.split(":").map { it.toInt() }
            val fimCalendar = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, fim[0]); set(java.util.Calendar.MINUTE, fim[1]); add(java.util.Calendar.MINUTE, rotina?.duracaoPadraoMinutos ?: 0) }
            val fimStr = String.format("%02d:%02d", fimCalendar.get(java.util.Calendar.HOUR_OF_DAY), fimCalendar.get(java.util.Calendar.MINUTE))
            binding.textViewHorario.text = "${item.horarioInicio} - $fimStr"

            binding.textViewDescricao.text = rotina?.descricao
            binding.textViewDescricao.visibility = if (rotina?.descricao.isNullOrBlank()) View.GONE else View.VISIBLE

            binding.textViewTag.text = rotina?.tag
            binding.textViewTag.visibility = if (rotina?.tag.isNullOrBlank()) View.GONE else View.VISIBLE

            try {
                binding.viewCor.setBackgroundColor(Color.parseColor(rotina?.cor ?: "#808080"))
            } catch (e: Exception) {
                binding.viewCor.setBackgroundColor(Color.GRAY)
            }

            // Lógica do CheckBox
            binding.checkBoxConcluido.setOnCheckedChangeListener(null)
            binding.checkBoxConcluido.isChecked = itemCompletado.completado
            binding.checkBoxConcluido.setOnCheckedChangeListener { _, isChecked ->
                onCheckedChange(item, isChecked)
            }

            // Altera a aparência do item se estiver concluído
            if (itemCompletado.completado) {
                binding.textViewNomeRotina.paintFlags = binding.textViewNomeRotina.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.itemContainer.alpha = 0.6f
            } else {
                binding.textViewNomeRotina.paintFlags = binding.textViewNomeRotina.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.itemContainer.alpha = 1.0f
            }

            binding.root.setOnClickListener {
                onItemClicked(item)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ItemCronogramaCompletado>() {
            override fun areItemsTheSame(old: ItemCronogramaCompletado, new: ItemCronogramaCompletado) = old.item.id == new.item.id
            override fun areContentsTheSame(old: ItemCronogramaCompletado, new: ItemCronogramaCompletado) = old == new
        }
    }
}