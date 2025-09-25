// ARQUIVO: ui/CronogramaActivity.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.fabriciolima.momentus.MomentusApplication
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.databinding.ActivityCronogramaBinding
import br.com.fabriciolima.momentus.logic.GoogleCalendarManager
import br.com.fabriciolima.momentus.ui.cronograma.CronogramaPagerAdapter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.content.Intent
import br.com.fabriciolima.momentus.ui.cronograma.DiaCronogramaFragment

// --- MODIFICAÇÃO 2: A Activity agora "assina o contrato" da interface OnSwipeListener ---
class CronogramaActivity : AppCompatActivity(), DiaCronogramaFragment.OnSwipeListener {

    private lateinit var binding: ActivityCronogramaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCronogramaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val pagerAdapter = CronogramaPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        val diasDaSemana = listOf("DOM", "SEG", "TER", "QUA", "QUI", "SEX", "SÁB")

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = diasDaSemana[position]
        }.attach()

        val hoje = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(hoje))
    }

    // --- MODIFICAÇÃO 3: Implementamos a função da interface ---
    // Esta função será chamada pelo fragmento quando o estado do deslize mudar.
    override fun onSwipeStateChanged(isSwiping: Boolean) {
        // A Activity agora é responsável por controlar seu próprio componente.
        binding.viewPager.isUserInputEnabled = !isSwiping
    }
    // --- FIM DA MODIFICAÇÃO ---

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.cronograma_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_generate -> {
                iniciarGeracaoDeEventos()
                true
            }
            // --- MODIFICAÇÃO INICIA AQUI ---
            R.id.action_manage_templates -> {
                val intent = Intent(this, TemplatesActivity::class.java)
                startActivity(intent)
                true
            }
            // --- MODIFICAÇÃO TERMINA AQUI ---
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun iniciarGeracaoDeEventos() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account == null) {
            Toast.makeText(this, "Faça o login com Google primeiro na tela principal.", Toast.LENGTH_LONG).show()
            return
        }

        val repository = (application as MomentusApplication).repository
        val startDate = Calendar.getInstance()
        val endDate = Calendar.getInstance()

        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            startDate.set(year, month, dayOfMonth)

            DatePickerDialog(this, { _, endYear, endMonth, endDayOfMonth ->
                endDate.set(endYear, endMonth, endDayOfMonth)

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val textoDatas = "Gerar eventos de ${sdf.format(startDate.time)} até ${sdf.format(endDate.time)}?"

                AlertDialog.Builder(this)
                    .setTitle("Confirmar Geração")
                    .setMessage(textoDatas)
                    .setPositiveButton("Sim") { _, _ ->
                        // --- MODIFICAÇÃO INICIA AQUI ---
                        // 1. Tornamos a ProgressBar visível ANTES de iniciar a operação demorada.
                        binding.progressBar.visibility = View.VISIBLE
                        // --- MODIFICAÇÃO TERMINA AQUI ---

                        lifecycleScope.launch {
                            val resultado = GoogleCalendarManager.generateEvents(
                                this@CronogramaActivity, account, repository, startDate, endDate
                            )

                            // --- MODIFICAÇÃO INICIA AQUI ---
                            // 2. Após a operação terminar (seja com sucesso ou falha),
                            // escondemos a ProgressBar novamente.
                            binding.progressBar.visibility = View.GONE
                            // --- MODIFICAÇÃO TERMINA AQUI ---

                            resultado.onSuccess {
                                Toast.makeText(this@CronogramaActivity, "$it eventos criados com sucesso!", Toast.LENGTH_LONG).show()
                            }
                            resultado.onFailure {
                                Toast.makeText(this@CronogramaActivity, "Falha ao criar eventos. Verifique a internet.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    .setNegativeButton("Não", null)
                    .show()

            }, endDate.get(Calendar.YEAR), endDate.get(Calendar.MONTH), endDate.get(Calendar.DAY_OF_MONTH)).show()

        }, startDate.get(Calendar.YEAR), startDate.get(Calendar.MONTH), startDate.get(Calendar.DAY_OF_MONTH)).show()
    }
}