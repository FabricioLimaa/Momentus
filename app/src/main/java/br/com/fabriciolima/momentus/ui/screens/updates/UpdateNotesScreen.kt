package br.com.fabriciolima.momentus.ui.screens.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import br.com.fabriciolima.momentus.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import br.com.fabriciolima.momentus.ui.screens.market.XPBadge
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateNotesScreen(
    navController: NavController,
    bottomBarPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notas de Atualização", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Fundo com gradiente sutil
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "O que há de novo?",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                val versionName = try {
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    packageInfo.versionName
                } catch (_: Exception) { "2.1.0" }

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "Versão $versionName • Store & Personalização",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Seções de Novidades
                UpdateSectionItem(
                    icon = Icons.Default.AutoAwesome,
                    title = "Stickers com Utilidade!",
                    description = "Agora você pode aplicar os stickers comprados às suas categorias. Eles aparecerão na timeline para destacar suas tarefas mais importantes."
                )

                UpdateSectionItem(
                    icon = Icons.Default.ShoppingBag,
                    title = "Momentus Store",
                    description = "Use seu XP para adquirir colecionáveis exclusivos. Personalize seu perfil e mostre sua dedicação à produtividade."
                )

                UpdateSectionItem(
                    icon = Icons.Default.Devices,
                    title = "Layout Universal Premium",
                    description = "Refizemos toda a arquitetura visual para garantir uma experiência fluida em qualquer aparelho, respeitando 100% as barras do sistema."
                )

                UpdateSectionItem(
                    icon = Icons.Default.Bolt,
                    title = "Economia de XP 2.0",
                    description = "Migramos nosso sistema de pontos para suportar acúmulos massivos. Ganhe recompensas sem limites enquanto foca nos seus objetivos."
                )

                UpdateSectionItem(
                    icon = Icons.Default.Timer,
                    title = "Foco Personalizado",
                    description = "Ajuste os tempos de foco e pausas conforme sua necessidade. Introduzimos o acompanhamento de ciclos para sessões de trabalho profundo."
                )

                Spacer(modifier = Modifier.height(40.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = br.com.fabriciolima.momentus.R.drawable.ic_launcher_logo_round),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Momentus Premium",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Viva o presente, planeje o futuro.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(bottomBarPadding + 48.dp))
            }
        }
    }
}

@Composable
fun UpdateSectionItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    GlassCard(
        modifier = Modifier.padding(vertical = 8.dp),
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
