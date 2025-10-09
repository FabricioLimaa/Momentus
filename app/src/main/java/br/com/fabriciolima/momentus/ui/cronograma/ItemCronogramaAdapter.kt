package br.com.fabriciolima.momentus.ui.cronograma

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fabriciolima.momentus.data.model.ItemCronograma
import br.com.fabriciolima.momentus.data.model.Rotina
// CORREÇÃO: Importar a classe de vinculação gerada pelo View Binding
import br.com.fabriciolima.momentus.databinding.ItemCronogramaBinding
import java.time.format.DateTimeFormatter

class ItemCronogramaAdapter(
    private var itens: List<ItemCronograma>,
    private var rotinas: List<Rotina>,
    private var habitosConcluidos: Set<String>,
    private val onHabitoConcluidoListener: (String, Boolean) -> Unit
) : RecyclerView.Adapter<ItemCronogramaAdapter.ViewHolder>() {

    private val rotinasMap by lazy { rotinas.associateBy { it.id } }
    private val formatter = DateTimeFormatter.ofPattern("HH:mm")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // CORREÇÃO: Inflar o layout usando a classe de vinculação (View Binding)
        val binding = ItemCronogramaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itens[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = itens.size

    fun updateData(newItens: List<ItemCronograma>, newRotinas: List<Rotina>, newHabitos: Set<String>) {
        this.itens = newItens
        this.rotinas = newRotinas
        this.habitosConcluidos = newHabitos
        notifyDataSetChanged()
    }

    // CORREÇÃO: O ViewHolder agora usa ItemCronogramaBinding em vez de uma View genérica e findViewById
    inner class ViewHolder(private val binding: ItemCronogramaBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ItemCronograma) {
            // CORREÇÃO: Usando os nomes de ID corretos do arquivo XML (ex: itemCronogramaTitulo -> textViewNomeRotina)
            binding.textViewNomeRotina.text = item.titulo
            binding.textViewHorario.text = "${item.horarioInicio.format(formatter)} - ${item.horarioTermino.format(formatter)}"
            binding.textViewTag.text = rotinasMap[item.rotinaId]?.nome ?: ""

            binding.checkBoxConcluido.setOnCheckedChangeListener(null) // Evitar chamadas recursivas
            binding.checkBoxConcluido.isChecked = habitosConcluidos.contains(item.id)

            binding.checkBoxConcluido.setOnCheckedChangeListener { _, isChecked ->
                onHabitoConcluidoListener(item.id, isChecked)
            }
        }
    }
}
