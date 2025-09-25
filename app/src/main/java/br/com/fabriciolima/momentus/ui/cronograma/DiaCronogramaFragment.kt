// ARQUIVO: ui/cronograma/DiaCronogramaFragment.kt (CÓDIGO COMPLETO)

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
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.databinding.FragmentDiaCronogramaBinding
import br.com.fabriciolima.momentus.viewmodel.CronogramaViewModel
import br.com.fabriciolima.momentus.viewmodel.CronogramaViewModelFactory
import com.google.android.material.snackbar.Snackbar

class DiaCronogramaFragment : Fragment() {

    private var _binding: FragmentDiaCronogramaBinding? = null
    private val binding get() = _binding!!

    // --- MODIFICAÇÃO 1: Definimos a interface de comunicação ---
    interface OnSwipeListener {
        fun onSwipeStateChanged(isSwiping: Boolean)
    }

    private var swipeListener: OnSwipeListener? = null
    // --- FIM DA MODIFICAÇÃO ---

    private lateinit var adapter: ItemCronogramaAdapter
    private var diaDaSemana: String? = null
    private var rotinasDisponiveis: List<Rotina> = emptyList()

    private val viewModel: CronogramaViewModel by activityViewModels {
        CronogramaViewModelFactory(
            (requireActivity().application as MomentusApplication).repository,
            requireActivity().application
        )
    }

    // --- MODIFICAÇÃO 2: Garantimos que a Activity implementa a interface ao anexar o fragmento ---
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnSwipeListener) {
            swipeListener = context
        } else {
            throw RuntimeException("$context must implement OnSwipeListener")
        }
    }
    // --- FIM DA MODIFICAÇÃO ---

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
            if (::adapter.isInitialized) {
                adapter.setRotinas(rotinas)
            }
        }

        diaDaSemana?.let { dia ->
            viewModel.getItensDoDia(dia).observe(viewLifecycleOwner) { itensDoDia ->
                if (itensDoDia.isEmpty()) {
                    binding.recyclerViewItensDoDia.visibility = View.GONE
                    binding.emptyStateLayoutFragment.visibility = View.VISIBLE
                } else {
                    binding.recyclerViewItensDoDia.visibility = View.VISIBLE
                    binding.emptyStateLayoutFragment.visibility = View.GONE
                }
                if (::adapter.isInitialized) {
                    adapter.submitList(itensDoDia)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ItemCronogramaAdapter { itemClicado ->
            AddItemCronogramaDialog.newInstance(itemClicado).show(childFragmentManager, "EditItemDialog")
        }
        binding.recyclerViewItensDoDia.adapter = adapter
        binding.recyclerViewItensDoDia.layoutManager = LinearLayoutManager(requireContext())

        // --- MODIFICAÇÃO 3: Removemos a referência direta ao ViewPager daqui ---

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

            // A LÓGICA QUE RESOLVE O CONFLITO ESTÁ AQUI
            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                // --- MODIFICAÇÃO 4: Usamos a interface para avisar a Activity ---
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    swipeListener?.onSwipeStateChanged(true) // Avisa: "Comecei a deslizar"
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                // --- MODIFICAÇÃO 5: Usamos a interface para avisar a Activity ---
                swipeListener?.onSwipeStateChanged(false) // Avisa: "Parei de deslizar"
            }
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.recyclerViewItensDoDia)
    }

    // --- MODIFICAÇÃO 6: Limpamos a referência ao listener para evitar vazamento de memória ---
    override fun onDetach() {
        super.onDetach()
        swipeListener = null
    }
    // --- FIM DA MODIFICAÇÃO ---

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