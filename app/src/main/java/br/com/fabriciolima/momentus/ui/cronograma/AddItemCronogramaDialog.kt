// ARQUIVO: ui/cronograma/AddItemCronogramaDialog.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.ui.cronograma

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import br.com.fabriciolima.momentus.MomentusApplication
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.viewmodel.CronogramaViewModel
import br.com.fabriciolima.momentus.viewmodel.CronogramaViewModelFactory
import java.util.Calendar

class AddItemCronogramaDialog : DialogFragment() {

    // --- MODIFICAÇÃO INICIA AQUI ---
    // Adicionamos o segundo parâmetro 'requireActivity().application'
    // que estava faltando na criação da Factory.
    private val viewModel: CronogramaViewModel by activityViewModels {
        CronogramaViewModelFactory(
            (requireActivity().application as MomentusApplication).repository,
            requireActivity().application
        )
    }
    // --- MODIFICAÇÃO TERMINA AQUI ---

    private var itemExistente: ItemCronograma? = null
    private var diaDaSemana: String? = null
    private var horaSelecionada: String? = null
    private var rotinasDisponiveis = listOf<Rotina>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            diaDaSemana = it.getString(ARG_DIA_DA_SEMANA)
            val itemId = it.getString(ARG_ITEM_ID)
            val itemRotinaId = it.getString(ARG_ITEM_ROTINA_ID)
            val itemHorario = it.getString(ARG_ITEM_HORARIO)

            if (itemId != null && itemRotinaId != null && itemHorario != null) {
                itemExistente = ItemCronograma(itemId, diaDaSemana!!, itemHorario, itemRotinaId)
                horaSelecionada = itemHorario
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_add_item_cronograma, null)

        val spinner: Spinner = view.findViewById(R.id.spinnerRotinas)
        val buttonHora: Button = view.findViewById(R.id.buttonEscolherHora)

        itemExistente?.let {
            buttonHora.text = it.horarioInicio
        }

        viewModel.todasAsRotinas.observe(this) { rotinas ->
            rotinasDisponiveis = rotinas
            val nomesRotinas = rotinas.map { it.nome }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, nomesRotinas)
            spinner.adapter = adapter

            itemExistente?.let { item ->
                val rotinaIndex = rotinas.indexOfFirst { it.id == item.rotinaId }
                if (rotinaIndex != -1) {
                    spinner.setSelection(rotinaIndex)
                }
            }
        }

        buttonHora.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    horaSelecionada = String.format("%02d:%02d", hourOfDay, minute)
                    buttonHora.text = horaSelecionada
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        builder.setView(view)
            .setPositiveButton("Salvar") { _, _ ->
                val rotinaSelecionada = rotinasDisponiveis.getOrNull(spinner.selectedItemPosition)
                if (validar(rotinaSelecionada)) {
                    val itemParaSalvar = ItemCronograma(
                        id = itemExistente?.id ?: java.util.UUID.randomUUID().toString(),
                        diaDaSemana = diaDaSemana!!,
                        horarioInicio = horaSelecionada!!,
                        rotinaId = rotinaSelecionada!!.id
                    )
                    viewModel.insertItem(itemParaSalvar)
                }
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }

        return builder.create()
    }

    private fun validar(rotina: Rotina?): Boolean {
        if (rotina == null) {
            Toast.makeText(context, "Nenhuma rotina selecionada.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (horaSelecionada == null) {
            Toast.makeText(context, "Nenhum horário selecionado.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (diaDaSemana == null) {
            Toast.makeText(context, "Erro: Dia da semana inválido.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    companion object {
        private const val ARG_DIA_DA_SEMANA = "DIA_DA_SEMANA"
        private const val ARG_ITEM_ID = "ITEM_ID"
        private const val ARG_ITEM_ROTINA_ID = "ITEM_ROTINA_ID"
        private const val ARG_ITEM_HORARIO = "ITEM_HORARIO"

        fun newInstance(dia: String): AddItemCronogramaDialog {
            return AddItemCronogramaDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_DIA_DA_SEMANA, dia)
                }
            }
        }

        fun newInstance(item: ItemCronograma): AddItemCronogramaDialog {
            return AddItemCronogramaDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_DIA_DA_SEMANA, item.diaDaSemana)
                    putString(ARG_ITEM_ID, item.id)
                    putString(ARG_ITEM_ROTINA_ID, item.rotinaId)
                    putString(ARG_ITEM_HORARIO, item.horarioInicio)
                }
            }
        }
    }
}