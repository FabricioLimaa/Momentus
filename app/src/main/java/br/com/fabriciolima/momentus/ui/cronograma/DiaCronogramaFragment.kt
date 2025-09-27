package br.com.fabriciolima.momentus.ui.cronograma

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.fabriciolima.momentus.MomentusApplication
import br.com.fabriciolima.momentus.data.ItemCronograma
import br.com.fabriciolima.momentus.data.ItemCronogramaCompletado
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.databinding.FragmentDiaCronogramaBinding
import br.com.fabriciolima.momentus.viewmodel.CronogramaViewModel
import br.com.fabriciolima.momentus.viewmodel.CronogramaViewModelFactory
import com.google.android.material.snackbar.Snackbar

class DiaCronogramaFragment : Fragment() {

    private var _binding: FragmentDiaCronogramaBinding? = null
    private val binding get() = _binding!!

    interface OnSwipeListener {
        fun onSwipeStateChanged(isSwiping: Boolean)
    }
    private var swipeListener: OnSwipeListener? = null

    // --- SEU TRECHO DE CÓDIGO VAI AQUI ---
    // Atualizamos a inicialização do adapter para passar os dois listeners
    private val adapter = ItemCronogramaAdapter(
        onItemClicked = { itemClicado ->
            AddItemCronogramaDialog.newInstance(itemClicado).show(childFragmentManager, "EditItemDialog")
        },
        onCheckedChange = { item, isChecked ->
            viewModel.onHabitoConcluidoChanged(item, isChecked)
        }
    )
    // --- FIM DO TRECHO ---

    private var diaDaSemana: String? = null
    private var rotinasDisponiveis: List<Rotina> = emptyList()

    private val viewModel: CronogramaViewModel by activityViewModels {
        CronogramaViewModelFactory(
            (requireActivity().application as MomentusApplication).repository,
            requireActivity().application
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnSwipeListener) {
            swipeListener = context
        } else {
            throw RuntimeException("$context must implement OnSwipeListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDiaCronogramaBinding.inflate(inflater, container, false)
        arguments?.let { diaDaSemana = it.getString(ARG_DIA_DA_SEMANA) }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        binding.fabAddItem.setOnClickListener {
            diaDaSemana?.let {
                AddItemCronogramaDialog.newInstance(it).show(childFragmentManager, "AddItemDialog")
            }
        }

        viewModel.todasAsRotinas.observe(viewLifecycleOwner) { rotinas ->
            rotinasDisponiveis = rotinas
            adapter.setRotinas(rotinas)
        }

        diaDaSemana?.let { dia ->
            viewModel.getItensDoDiaCompletados(dia).observe(viewLifecycleOwner) { itensCompletados ->
                if (itensCompletados.isEmpty()) {
                    binding.recyclerViewItensDoDia.visibility = View.GONE
                    binding.emptyStateLayoutFragment.visibility = View.VISIBLE
                } else {
                    binding.recyclerViewItensDoDia.visibility = View.VISIBLE
                    binding.emptyStateLayoutFragment.visibility = View.GONE
                }
                adapter.submitList(itensCompletados)
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewItensDoDia.adapter = adapter
        binding.recyclerViewItensDoDia.layoutManager = LinearLayoutManager(requireContext())

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val itemParaDeletar = adapter.getItemAt(position)
                viewModel.deleteItem(itemParaDeletar)

                Snackbar.make(binding.root, "Item agendado deletado", Snackbar.LENGTH_LONG)
                    .setAction("DESFAZER") {
                        viewModel.reinsereItem()
                    }
                    .show()
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    swipeListener?.onSwipeStateChanged(true)
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                swipeListener?.onSwipeStateChanged(false)
            }
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.recyclerViewItensDoDia)
    }

    override fun onDetach() {
        super.onDetach()
        swipeListener = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_DIA_DA_SEMANA = "DIA_DA_SEMANA"
        fun newInstance(dia: String): DiaCronogramaFragment {
            return DiaCronogramaFragment().apply {
                arguments = Bundle().apply { putString(ARG_DIA_DA_SEMANA, dia) }
            }
        }
    }
}