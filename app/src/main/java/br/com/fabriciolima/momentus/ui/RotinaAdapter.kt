// ARQUIVO: ui/RotinaAdapter.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.data.RotinaComMeta
import br.com.fabriciolima.momentus.databinding.ItemRotinaBinding

// --- MODIFICAÇÃO 1: O adapter agora trabalha com o tipo 'RotinaComMeta' ---
class RotinaAdapter(
    private val onItemClicked: (Rotina) -> Unit
) : ListAdapter<RotinaComMeta, RotinaAdapter.RotinaViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RotinaViewHolder {
        val binding = ItemRotinaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RotinaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RotinaViewHolder, position: Int) {
        val rotinaComMeta = getItem(position)
        holder.bind(rotinaComMeta)
    }

    fun getRotinaAt(position: Int): Rotina {
        // A função de deletar ainda precisa apenas do objeto 'Rotina'.
        return getItem(position).rotina
    }

    inner class RotinaViewHolder(private val binding: ItemRotinaBinding) : RecyclerView.ViewHolder(binding.root) {
        // --- MODIFICAÇÃO 2: O método 'bind' agora recebe um objeto 'RotinaComMeta' ---
        fun bind(rotinaComMeta: RotinaComMeta) {
            val rotina = rotinaComMeta.rotina
            val meta = rotinaComMeta.meta

            binding.textViewNome.text = rotina.nome
            binding.textViewDuracao.text = "Duração: ${rotina.duracaoPadraoMinutos} minutos"

            // --- MODIFICAÇÃO 3: Mostra a meta se ela existir ---
            if (meta != null && meta.metaMinutosSemanal > 0) {
                val metaHoras = meta.metaMinutosSemanal / 60
                binding.textViewMeta.text = "Meta: $metaHoras horas/semana"
                binding.textViewMeta.visibility = View.VISIBLE
            } else {
                binding.textViewMeta.visibility = View.GONE
            }
            // (Vamos adicionar o textViewMeta ao layout no próximo passo)

            try {
                binding.viewCor.setBackgroundColor(Color.parseColor(rotina.cor))
            } catch (e: Exception) {
                binding.viewCor.setBackgroundColor(Color.GRAY)
            }

            binding.root.setOnClickListener {
                // O clique ainda envia apenas o objeto 'Rotina' para a tela de edição.
                onItemClicked(rotina)
            }
        }
    }

    companion object {
        // --- MODIFICAÇÃO 4: O DiffCallback agora compara objetos 'RotinaComMeta' ---
        private val DiffCallback = object : DiffUtil.ItemCallback<RotinaComMeta>() {
            override fun areItemsTheSame(oldItem: RotinaComMeta, newItem: RotinaComMeta): Boolean {
                return oldItem.rotina.id == newItem.rotina.id
            }

            override fun areContentsTheSame(oldItem: RotinaComMeta, newItem: RotinaComMeta): Boolean {
                return oldItem == newItem
            }
        }
    }
}