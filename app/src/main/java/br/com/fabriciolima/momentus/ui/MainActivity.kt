// ARQUIVO: ui/MainActivity.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.fabriciolima.momentus.MomentusApplication
import br.com.fabriciolima.momentus.R
import br.com.fabriciolima.momentus.data.Rotina
import br.com.fabriciolima.momentus.databinding.ActivityMainBinding
import br.com.fabriciolima.momentus.viewmodel.MainViewModel
import br.com.fabriciolima.momentus.viewmodel.MainViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.calendar.CalendarScopes
import com.google.android.material.snackbar.Snackbar
import android.Manifest // Adicione este import
import android.content.pm.PackageManager // Adicione este import
import android.os.Build // Adicione este import
import androidx.core.content.ContextCompat // Adicione este import

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as MomentusApplication).repository)
    }
    private lateinit var rotinaAdapter: RotinaAdapter
    private var googleAccount: GoogleSignInAccount? = null
    private lateinit var googleSignInClient: GoogleSignInClient

    private val editorRotinaLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val rotinaSalva = result.data?.getSerializableExtra("ROTINA_SALVA") as? Rotina
            rotinaSalva?.let {
                viewModel.addRotina(it)
            }
        }
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                googleAccount = task.getResult(ApiException::class.java)
                Toast.makeText(this, "Login bem-sucedido!", Toast.LENGTH_SHORT).show()
                invalidateOptionsMenu()
            } catch (e: ApiException) {
                Toast.makeText(this, "Falha no login. Código: ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- MODIFICAÇÃO INICIA AQUI ---
    // 1. Criamos um novo launcher para pedir a permissão de notificação.
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permissão para notificações concedida.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissão para notificações negada.", Toast.LENGTH_SHORT).show()
            }
        }
    // --- MODIFICAÇÃO TERMINA AQUI ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        setupRecyclerView()
        setupGoogleSignIn()

        viewModel.rotinas.observe(this) { listaDeRotinas ->
            if (listaDeRotinas.isEmpty()) {
                binding.recyclerViewRotinas.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.VISIBLE
            } else {
                binding.recyclerViewRotinas.visibility = View.VISIBLE
                binding.emptyStateLayout.visibility = View.GONE
            }
            // --- MODIFICAÇÃO INICIA AQUI ---
            // 1. Em vez de chamar 'updateData', agora usamos 'submitList'.
            //    O ListAdapter receberá a nova lista, calculará as diferenças
            //    e animará as mudanças automaticamente.
            rotinaAdapter.submitList(listaDeRotinas)
            // --- MODIFICAÇÃO TERMINA AQUI ---
        }

        binding.fab.setOnClickListener {
            val intent = Intent(this, EditorRotinaActivity::class.java)
            editorRotinaLauncher.launch(intent)
        }
        // --- MODIFICAÇÃO INICIA AQUI ---
        // 2. Chamamos a função para pedir a permissão.
        askNotificationPermission()
        // --- MODIFICAÇÃO TERMINA AQUI ---
    }

    // --- MODIFICAÇÃO INICIA AQUI ---
    // 3. Nova função que verifica e, se necessário, pede a permissão.
    private fun askNotificationPermission() {
        // A permissão só é necessária para Android 13 (API 33) e superior.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Se a permissão não foi concedida, nós a solicitamos.
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    // --- MODIFICAÇÃO TERMINA AQUI ---

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(CalendarScopes.CALENDAR))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleAccount = GoogleSignIn.getLastSignedInAccount(this)
        if (googleAccount != null) {
            Toast.makeText(this, "Logado como ${googleAccount?.email}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val signInItem = menu?.findItem(R.id.action_google_sign_in)
        val signOutItem = menu?.findItem(R.id.action_google_sign_out)

        if (googleAccount != null) {
            signInItem?.isVisible = false
            signOutItem?.isVisible = true
            signOutItem?.title = "Logout (${googleAccount?.email})"
        } else {
            signInItem?.isVisible = true
            signOutItem?.isVisible = false
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun setupRecyclerView() {
        // --- MODIFICAÇÃO INICIA AQUI ---
        // 2. O adapter não precisa mais da lista vazia no construtor.
        rotinaAdapter = RotinaAdapter { rotinaClicada ->
            val intent = Intent(this, EditorRotinaActivity::class.java)
            intent.putExtra("ROTINA_PARA_EDITAR", rotinaClicada)
            editorRotinaLauncher.launch(intent)
        }
        // --- MODIFICAÇÃO TERMINA AQUI ---
        binding.recyclerViewRotinas.apply {
            adapter = rotinaAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

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
                val rotinaParaDeletar = rotinaAdapter.getRotinaAt(position)
                viewModel.deleteRotina(rotinaParaDeletar)

                // --- MODIFICAÇÃO INICIA AQUI ---
                // 1. Em vez de só mostrar uma mensagem, agora criamos um Snackbar com uma ação.
                Snackbar.make(binding.root, "Rotina deletada", Snackbar.LENGTH_LONG)
                    .setAction("DESFAZER") {
                        // 2. A ação do botão é simplesmente chamar a nova função do ViewModel
                        // para reinserir a última rotina deletada.
                        viewModel.reinsereRotina()
                    }
                    .show()
                // --- MODIFICAÇÃO TERMINA AQUI ---
            }
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.recyclerViewRotinas)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_schedule -> {
                val intent = Intent(this, CronogramaActivity::class.java)
                startActivity(intent)
                true
            }
            // --- MODIFICAÇÃO INICIA AQUI ---
            R.id.action_stats -> {
                val intent = Intent(this, StatsActivity::class.java)
                startActivity(intent)
                true
            }
            // --- MODIFICAÇÃO TERMINA AQUI ---
            R.id.action_google_sign_in -> {
                signInWithGoogle()
                true
            }
            R.id.action_google_sign_out -> {
                signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun signInWithGoogle() {
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener(this) {
            googleAccount = null
            Toast.makeText(this, "Logout realizado.", Toast.LENGTH_SHORT).show()
            invalidateOptionsMenu()
        }
    }
}