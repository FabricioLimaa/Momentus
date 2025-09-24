// ARQUIVO: ui/StatsActivity.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.ui

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import br.com.fabriciolima.momentus.MomentusApplication
import br.com.fabriciolima.momentus.data.StatsResult
import br.com.fabriciolima.momentus.databinding.ActivityStatsBinding
import br.com.fabriciolima.momentus.viewmodel.StatsViewModel
import br.com.fabriciolima.momentus.viewmodel.StatsViewModelFactory
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate

class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding

    // --- MODIFICAÇÃO INICIA AQUI ---
    // 1. Instanciamos o novo ViewModel usando a Factory.
    private val viewModel: StatsViewModel by viewModels {
        StatsViewModelFactory((application as MomentusApplication).repository)
    }
    // --- MODIFICAÇÃO TERMINA AQUI ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // --- MODIFICAÇÃO INICIA AQUI ---
        // 2. Configuramos a aparência inicial do nosso gráfico.
        setupChart()

        // 3. Observamos o LiveData do ViewModel.
        viewModel.stats.observe(this) { statsList ->
            // Quando os dados chegam do banco, chamamos a função para atualizar o gráfico.
            if (statsList.isNotEmpty()) {
                updateChart(statsList)
            }
        }
        // --- MODIFICAÇÃO TERMINA AQUI ---
    }

    private fun setupChart() {
        binding.pieChart.apply {
            // Usamos o formatador de porcentagem para os valores no gráfico.
            setUsePercentValues(true)
            // Texto de descrição do gráfico.
            description.text = "Distribuição de tempo por rotina"
            // Desativamos a legenda, pois os nomes ficarão no próprio gráfico.
            legend.isEnabled = false
            // Animação de entrada.
            animateY(1400)
        }
    }

    private fun updateChart(data: List<StatsResult>) {
        // 1. Convertendo nossa lista de 'StatsResult' para uma lista de 'PieEntry' que o gráfico entende.
        val entries = ArrayList<PieEntry>()
        for (item in data) {
            entries.add(PieEntry(item.totalMinutos.toFloat(), item.nomeRotina))
        }

        // 2. Convertendo nossas cores hexadecimais para uma lista de inteiros que o gráfico entende.
        val colors = ArrayList<Int>()
        for (item in data) {
            try {
                colors.add(Color.parseColor(item.corRotina))
            } catch (e: Exception) {
                colors.add(Color.GRAY) // Cor padrão em caso de erro
            }
        }

        // 3. Criando o conjunto de dados para o gráfico.
        val dataSet = PieDataSet(entries, "Rotinas")
        dataSet.colors = colors // Aplicando nossa paleta de cores
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.BLACK

        // 4. Criando o objeto de dados final e aplicando o formatador de porcentagem.
        val pieData = PieData(dataSet)
        pieData.setValueFormatter(PercentFormatter(binding.pieChart))

        // 5. Entregando os dados para o gráfico e atualizando-o.
        binding.pieChart.data = pieData
        binding.pieChart.invalidate()
    }

    // Função para fazer o botão de "voltar" na toolbar funcionar.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}