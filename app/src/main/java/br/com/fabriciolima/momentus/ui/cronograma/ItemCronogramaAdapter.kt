// ARQUIVO: ui/cronograma/ItemCronogramaAdapter.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.ui.cronograma

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.databinding.ItemCronogramaBinding

// --- MODIFICAÇÃO INICIA AQUI ---
// 1. Herda de ListAdapter e remove a lista do construtor.
class ItemCronogramaAdapter(
    private val onItemClicked: (ItemCronograma) -> Unit
) : ListAdapter<ItemCronograma, ItemCronogramaAdapter.ItemViewHolder>(DiffCallback) {

    // 2. O mapa de rotinas ainda é útil, então o mantemos e criamos um método para atualizá-lo.
    private var rotinasMap = emptyMap<String, Rotina>()

    // --- MODIFICAÇÃO TERMINA AQUI ---

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemCronogramaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        // 3. Usa getItem(position) para pegar o item.
        val item = getItem(position)
        val rotinaCorrespondente = rotinasMap[item.rotinaId]
        holder.bind(item, rotinaCorrespondente)
    }

    // --- MODIFICAÇÃO INICIA AQUI ---
    // 4. Em vez de 'setData', temos um método separado para atualizar o mapa de rotinas.
    //    A lista de itens será atualizada pelo 'submitList'.
    fun setRotinas(todasAsRotinas: List<Rotina>) {
        this.rotinasMap = todasAsRotinas.associateBy { it.id }
        notifyDataSetChanged() // Usamos aqui para garantir que os nomes sejam atualizados se uma rotina for editada.
    }
    // --- MODIFICAÇÃO TERMINA AQUI ---

    fun getItemAt(position: Int): ItemCronograma {
        return getItem(position)
    }

    inner class ItemViewHolder(private val binding: ItemCronogramaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ItemCronograma, rotina: Rotina?) {
            binding.textViewHorario.text = item.horarioInicio
            binding.textViewNomeRotina.text = rotina?.nome ?: "Rotina não encontrada"
            try {
                binding.viewCor.setBackgroundColor(Color.parseColor(rotina?.cor ?: "#808080"))
            } catch (e: Exception) {
                binding.viewCor.setBackgroundColor(Color.GRAY)
            }

            binding.root.setOnClickListener {
                onItemClicked(item)
            }
        }
    }

    // --- MODIFICAÇÃO INICIA AQUI ---
    // 5. Adicionamos o DiffUtil.ItemCallback para o ItemCronograma.
    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ItemCronograma>() {
            override fun areItemsTheSame(oldItem: ItemCronograma, newItem: ItemCronograma): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ItemCronograma, newItem: ItemCronograma): Boolean {
                return oldItem == newItem
            }
        }
    }
    // --- MODIFICAÇÃO TERMINA AQUI ---
}