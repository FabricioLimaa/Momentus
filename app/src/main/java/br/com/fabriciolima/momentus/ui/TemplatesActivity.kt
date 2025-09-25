// ARQUIVO: ui/TemplatesActivity.kt (CÓDIGO COMPLETO)
package br.com.fabriciolima.momentus.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.fabriciolima.momentus.MomentusApplication
import br.com.fabriciolima.momentus.databinding.ActivityTemplatesBinding
import br.com.fabriciolima.momentus.viewmodel.TemplateViewModel
import br.com.fabriciolima.momentus.viewmodel.TemplateViewModelFactory

class TemplatesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTemplatesBinding
    private val viewModel: TemplateViewModel by viewModels {
        TemplateViewModelFactory((application as MomentusApplication).repository)
    }
    private lateinit var adapter: TemplateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTemplatesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()

        viewModel.todosOsTemplates.observe(this) { templates ->
            adapter.submitList(templates)
        }

        binding.buttonSaveCurrent.setOnClickListener {
            showSaveTemplateDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = TemplateAdapter(
            onLoadClicked = { template ->
                viewModel.loadTemplate(template)
                Toast.makeText(this, "Template '${template.nome}' carregado!", Toast.LENGTH_SHORT).show()
                finish() // Volta para a tela de cronograma
            },
            onDeleteClicked = { template ->
                viewModel.deleteTemplate(template)
                Toast.makeText(this, "Template '${template.nome}' deletado.", Toast.LENGTH_SHORT).show()
            }
        )
        binding.recyclerViewTemplates.adapter = adapter
        binding.recyclerViewTemplates.layoutManager = LinearLayoutManager(this)
    }

    private fun showSaveTemplateDialog() {
        val editText = EditText(this).apply {
            hint = "Nome do Template (ex: Semana de Provas)"
        }

        AlertDialog.Builder(this)
            .setTitle("Salvar Cronograma Atual")
            .setView(editText)
            .setPositiveButton("Salvar") { _, _ ->
                val nome = editText.text.toString()
                if (nome.isNotBlank()) {
                    viewModel.saveTemplate(nome)
                    Toast.makeText(this, "Template '$nome' salvo!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "O nome não pode ser vazio.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}