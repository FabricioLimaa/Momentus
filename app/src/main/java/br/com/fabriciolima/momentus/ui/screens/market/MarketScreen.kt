package br.com.fabriciolima.momentus.ui.screens.market

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import br.com.fabriciolima.momentus.data.model.MarketItem
import br.com.fabriciolima.momentus.ui.theme.*
import br.com.fabriciolima.momentus.ui.viewmodel.MarketViewModel
import br.com.fabriciolima.momentus.ui.util.getIconForMarketItem
import androidx.compose.ui.unit.Dp
import br.com.fabriciolima.momentus.ui.components.PremiumSnackbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    navController: NavController,
    bottomBarPadding: Dp = 0.dp,
    viewModel: MarketViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error, uiState.purchaseSuccess) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it.message ?: "Erro ao processar compra")
            viewModel.onMessageShown()
        }
        uiState.purchaseSuccess?.let {
            snackbarHostState.showSnackbar("Parabéns! Você adquiriu: ${it.name}")
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { 
            SnackbarHost(snackbarHostState) { data ->
                PremiumSnackbar(data)
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Momentus Store", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    XPBadge(points = uiState.userData?.points ?: 0L)
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
        ) {
            // Background Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                        )
                    )
            )

            if (uiState.isLoading && uiState.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Column(modifier = Modifier
                    .padding(top = paddingValues.calculateTopPadding())
                    .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Personalize sua experiência com colecionáveis exclusivos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = bottomBarPadding + 32.dp)
                    ) {
                        items(uiState.items) { item ->
                            MarketItemCard(
                                item = item,
                                canAfford = (uiState.userData?.points ?: 0L) >= item.finalPrice,
                                onBuyClick = { viewModel.purchaseItem(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun XPBadge(points: Long) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        modifier = Modifier.padding(end = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = points.toString(),
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MarketItemCard(
    item: MarketItem,
    canAfford: Boolean,
    onBuyClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Raridade Badge
            Surface(
                color = item.rarity.color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(
                    text = item.rarity.label.uppercase(),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = item.rarity.color
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ícone do Item (Agora Dinâmico)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(item.rarity.color.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isOwned) Icons.Default.CheckCircle else (getIconForMarketItem(item.id) ?: Icons.Default.Inventory2),
                    contentDescription = null,
                    tint = if (item.isOwned) MaterialTheme.colorScheme.primary else item.rarity.color,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = item.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = item.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (item.isOwned) {
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("ADQUIRIDO", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onBuyClick,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canAfford) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (canAfford) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Bolt, null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${item.finalPrice}", fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
