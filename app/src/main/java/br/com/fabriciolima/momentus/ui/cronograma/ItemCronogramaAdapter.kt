// ARQUIVO: ui/cronograma/ItemCronogramaAdapter.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.ui.cronograma

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.databinding.ItemCronogramaBinding

// --- MODIFICAÇÃO INICIA AQUI ---
// 1. Adicionamos uma função 'onItemClicked' ao construtor, que será chamada quando um item for clicado.
class ItemCronogramaAdapter(
    private val onItemClicked: (ItemCronograma) -> Unit
) : RecyclerView.Adapter<ItemCronogramaAdapter.ItemViewHolder>() {
// --- MODIFICAÇÃO TERMINA AQUI ---

    private var itens = emptyList<ItemCronograma>()
    private var rotinasMap = emptyMap<String, Rotina>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemCronogramaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = itens[position]
        val rotinaCorrespondente = rotinasMap[item.rotinaId]
        holder.bind(item, rotinaCorrespondente)
    }

    override fun getItemCount() = itens.size

    fun setData(novosItens: List<ItemCronograma>, todasAsRotinas: List<Rotina>) {
        this.itens = novosItens
        this.rotinasMap = todasAsRotinas.associateBy { it.id }
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): ItemCronograma {
        return itens[position]
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

            // --- MODIFICAÇÃO INICIA AQUI ---
            // 2. Adicionamos um listener de clique ao cartão inteiro.
            // Quando clicado, ele chama a função que passamos para o adapter,
            // enviando o objeto 'ItemCronograma' deste cartão específico.
            binding.root.setOnClickListener {
                onItemClicked(item)
            }
            // --- MODIFICAÇÃO TERMINA AQUI ---
        }
    }
}