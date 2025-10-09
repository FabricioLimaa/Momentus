package br.com.fabriciolima.momentus.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import br.com.fabriciolima.momentus.MomentusApplication
import br.com.fabriciolima.momentus.data.model.Meta
import br.com.fabriciolima.momentus.data.model.Rotina
import br.com.fabriciolima.momentus.databinding.ActivityEditorRotinaBinding
import br.com.fabriciolima.momentus.ui.viewmodel.MainViewModel
import br.com.fabriciolima.momentus.ui.viewmodel.MainViewModelFactory

class EditorRotinaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorRotinaBinding
    private var rotinaExistente: Rotina? = null
    private var corSelecionada: String? = null

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as MomentusApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorRotinaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rotinaExistente = intent.getSerializableExtra("ROTINA_PARA_EDITAR") as? Rotina
        setupColorSelector()

        rotinaExistente?.let { rotina ->
            binding.editTextNome.setText(rotina.nome)
            binding.editTextDescricao.setText(rotina.descricao)
            binding.editTextTag.setText(rotina.tag)
            binding.editTextDuracao.setText(rotina.duracaoPadraoMinutos.toString())
            selecionarCorPeloCodigo(rotina.cor)

            // CORREÇÃO: a propriedade correta é 'rotinasComMetas'
            viewModel.rotinasComMetas.observe(this) { rotinasComMetas ->
                val rotinaAtual = rotinasComMetas.find { r -> r.rotina.id == rotina.id }
                rotinaAtual?.meta?.let { meta ->
                    binding.editTextMeta.setText((meta.metaMinutosSemanal / 60).toString())
                }
            }
        }

        binding.buttonSalvar.setOnClickListener {
            salvarEFechar()
        }
    }

    private fun setupColorSelector() {
        binding.colorSelectorContainer.children.forEach { colorView ->
            val colorCircle = (colorView as FrameLayout).getChildAt(0) as ImageView
            val colorStateList = colorCircle.backgroundTintList
            val colorHex = colorStateList?.toHex()

            colorView.setOnClickListener {
                selecionarCor(it, colorHex)
            }
        }

        if (corSelecionada == null) {
            (binding.colorSelectorContainer.getChildAt(0) as FrameLayout).performClick()
        }
    }

    private fun selecionarCor(viewSelecionada: View, corHex: String?) {
        binding.colorSelectorContainer.children.forEach { child ->
            val border = (child as FrameLayout).getChildAt(1) as ImageView
            border.visibility = View.GONE
        }
        val border = (viewSelecionada as FrameLayout).getChildAt(1) as ImageView
        border.visibility = View.VISIBLE
        corSelecionada = corHex
    }

    private fun selecionarCorPeloCodigo(codigoHex: String) {
        binding.colorSelectorContainer.children.forEach { colorView ->
            val colorCircle = (colorView as FrameLayout).getChildAt(0) as ImageView
            val colorStateList = colorCircle.backgroundTintList
            if (colorStateList?.toHex().equals(codigoHex, ignoreCase = true)) {
                colorView.performClick()
            }
        }
    }

    private fun ColorStateList.toHex(): String {
        return String.format("#%06X", (0xFFFFFF and this.defaultColor))
    }

    private fun salvarEFechar() {
        val nome = binding.editTextNome.text.toString()
        val duracaoStr = binding.editTextDuracao.text.toString()
        val descricao = binding.editTextDescricao.text.toString()
        val tag = binding.editTextTag.text.toString()
        val metaHorasStr = binding.editTextMeta.text.toString()

        var isValid = true

        if (nome.isBlank()) {
            binding.layoutNome.error = "Campo obrigatório"
            isValid = false
        } else {
            binding.layoutNome.error = null
        }

        if (duracaoStr.isBlank()) {
            binding.layoutDuracao.error = "Campo obrigatório"
            isValid = false
        } else {
            binding.layoutDuracao.error = null
        }

        if (corSelecionada == null) {
            Toast.makeText(this, "Por favor, selecione uma cor.", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (isValid) {
            val rotinaParaSalvar = Rotina(
                id = rotinaExistente?.id ?: java.util.UUID.randomUUID().toString(),
                nome = nome,
                duracaoPadraoMinutos = duracaoStr.toInt(),
                cor = corSelecionada!!,
                descricao = descricao.takeIf { it.isNotBlank() },
                tag = tag.takeIf { it.isNotBlank() }
            )

            // CORREÇÃO: O método correto é 'insertRotina'
            viewModel.insertRotina(rotinaParaSalvar)

            val metaHoras = if (metaHorasStr.isNotBlank()) metaHorasStr.toInt() else 0
            if (metaHoras > 0) {
                // CORREÇÃO: Criamos o objeto 'Meta' que a função espera
                val metaParaSalvar = Meta(
                    rotinaId = rotinaParaSalvar.id,
                    metaMinutosSemanal = metaHoras * 60 // Convertendo horas para minutos
                )
                viewModel.salvarMeta(metaParaSalvar)
            }

            val resultIntent = Intent()
            resultIntent.putExtra("ROTINA_SALVA", rotinaParaSalvar)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}
