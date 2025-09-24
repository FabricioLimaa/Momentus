// ARQUIVO: ui/EditorRotinaActivity.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.ui

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.children
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.databinding.ActivityEditorRotinaBinding
import com.google.android.material.textfield.TextInputEditText
import android.widget.Toast

class EditorRotinaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorRotinaBinding
    private var rotinaExistente: Rotina? = null
    // --- MODIFICAÇÃO INICIA AQUI ---
    // 1. Variável para guardar o código hexadecimal da cor selecionada.
    private var corSelecionada: String? = null
    // --- MODIFICAÇÃO TERMINA AQUI ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorRotinaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rotinaExistente = intent.getSerializableExtra("ROTINA_PARA_EDITAR") as? Rotina
        setupColorSelector() // 2. Chamamos a nova função para configurar o seletor.

        rotinaExistente?.let {
            binding.editTextNome.setText(it.nome)
            binding.editTextDuracao.setText(it.duracaoPadraoMinutos.toString())
            // 3. Selecionamos a cor da rotina existente.
            selecionarCorPeloCodigo(it.cor)
        }

        binding.buttonSalvar.setOnClickListener {
            salvarEFechar()
        }
    }

    // --- MODIFICAÇÃO INICIA AQUI ---
    // 4. Nova função para configurar os cliques nos círculos de cores.
    private fun setupColorSelector() {
        // Percorremos todos os 'FrameLayouts' dentro do nosso container de cores.
        binding.colorSelectorContainer.children.forEach { colorView ->
            // Acessamos o círculo colorido (o primeiro ImageView dentro do FrameLayout)
            val colorCircle = (colorView as FrameLayout).getChildAt(0) as ImageView
            // Pegamos a cor de fundo dele, que definimos no XML.
            val colorStateList = colorCircle.backgroundTintList
            val colorHex = colorStateList?.toHex()

            colorView.setOnClickListener {
                // Ao clicar, atualizamos a UI e guardamos o código da cor.
                selecionarCor(colorView, colorHex)
            }
        }
    }

    // 5. Função para atualizar a UI do seletor.
    private fun selecionarCor(viewSelecionada: View, corHex: String?) {
        // Primeiro, limpamos a seleção de todos os outros círculos.
        binding.colorSelectorContainer.children.forEach { child ->
            val border = (child as FrameLayout).getChildAt(1) as ImageView
            border.visibility = View.GONE
        }
        // Depois, mostramos a borda apenas no círculo que foi clicado.
        val border = (viewSelecionada as FrameLayout).getChildAt(1) as ImageView
        border.visibility = View.VISIBLE
        // E guardamos o código da cor.
        corSelecionada = corHex
    }

    // 6. Função para selecionar uma cor com base no código hexadecimal (usado ao editar).
    private fun selecionarCorPeloCodigo(codigoHex: String) {
        binding.colorSelectorContainer.children.forEach { colorView ->
            val colorCircle = (colorView as FrameLayout).getChildAt(0) as ImageView
            val colorStateList = colorCircle.backgroundTintList
            if (colorStateList?.toHex().equals(codigoHex, ignoreCase = true)) {
                // Se encontrarmos o círculo com a cor correspondente, simulamos um clique nele.
                colorView.performClick()
            }
        }
    }

    // 7. Função auxiliar para converter a cor para o formato hexadecimal.
    private fun ColorStateList.toHex(): String {
        return String.format("#%06X", (0xFFFFFF and this.defaultColor))
    }
    // --- MODIFICAÇÃO TERMINA AQUI ---

    private fun salvarEFechar() {
        val nome = binding.editTextNome.text.toString()
        val duracaoStr = binding.editTextDuracao.text.toString()

        // --- MODIFICAÇÃO INICIA AQUI ---
        // 8. Apagamos a referência ao editTextCor, pois ele não existe mais.
        // --- MODIFICAÇÃO TERMINA AQUI ---

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

        // --- MODIFICAÇÃO INICIA AQUI ---
        // 9. A validação agora checa se a variável 'corSelecionada' não é nula.
        if (corSelecionada == null) {
            Toast.makeText(this, "Por favor, selecione uma cor.", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        // --- MODIFICAÇÃO TERMINA AQUI ---

        if (isValid) {
            val rotinaParaSalvar = Rotina(
                id = rotinaExistente?.id ?: java.util.UUID.randomUUID().toString(),
                nome = nome,
                duracaoPadraoMinutos = duracaoStr.toInt(),
                // 10. Usamos a cor da variável 'corSelecionada'.
                cor = corSelecionada!!
            )

            val resultIntent = Intent()
            resultIntent.putExtra("ROTINA_SALVA", rotinaParaSalvar)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}