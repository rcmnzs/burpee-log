package com.example.burpeelog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.burpeelog.BurpeeLogApplication
import com.example.burpeelog.data.model.DURACAO_MINUTOS
import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSummaryScreen(
    tipo: TipoExercicio,
    padrao: List<Int>,
    repsFeitasInicial: Int,
    repsPrevistas: Int,
    duracaoSegundos: Int,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as BurpeeLogApplication
    val repository = app.repository
    val scope = rememberCoroutineScope()

    // Regra: campo editável de reps feitas com padrão = reps previstas
    var repsFeitasTexto by remember { mutableStateOf(repsPrevistas.toString()) }
    var observacao by remember { mutableStateOf("") }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val repsFeitasNumero = repsFeitasTexto.toIntOrNull() ?: repsPrevistas
    val isCompleto = duracaoSegundos >= (DURACAO_MINUTOS * 60)
    val minutos = duracaoSegundos / 60
    val segundos = duracaoSegundos % 60
    val tempoFormatado = String.format("%02d:%02d", minutos, segundos)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_workout_summary"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Resumo do Treino",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDiscardDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("btn_descartar_treino"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Descartar",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            if (!isSaving) {
                                isSaving = true
                                scope.launch {
                                    val sessao = Sessao(
                                        dataHora = System.currentTimeMillis(),
                                        tipo = tipo,
                                        padraoIntervalosSegundos = padrao,
                                        repsPrevistas = repsPrevistas,
                                        repsFeitas = repsFeitasNumero,
                                        observacao = observacao.trim().ifEmpty { null },
                                        importada = false
                                    )
                                    repository.insert(sessao)
                                    app.workoutSessionManager.reset()
                                    onFinish()
                                }
                            }
                        },
                        enabled = !isSaving && repsFeitasNumero in 0..5000,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(54.dp)
                            .testTag("btn_salvar_treino"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Salvar Treino",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card de Status do Treino
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_summary_status"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCompleto) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCompleto) Icons.Default.CheckCircle else Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (isCompleto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = if (isCompleto) "Treino Completo Concluído!" else "Treino Parcial Finalizado",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tipo.titulo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("txt_summary_tipo")
                        )
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$tempoFormatado (${minutos}m ${segundos}s)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Card com Reps Previstas e Campo Editável de Reps Feitas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_summary_reps"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Repetições Previstas:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$repsPrevistas reps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.testTag("txt_summary_reps_previstas")
                        )
                    }

                    Text(
                        text = "REPETIÇÕES FEITAS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Campo editável de reps feitas com botões rápidos de incremento/decremento
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                val atual = repsFeitasTexto.toIntOrNull() ?: repsPrevistas
                                if (atual > 0) repsFeitasTexto = (atual - 1).toString()
                            },
                            modifier = Modifier.testTag("btn_reps_minus")
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Diminuir 1 rep")
                        }

                        OutlinedTextField(
                            value = repsFeitasTexto,
                            onValueChange = { novo ->
                                repsFeitasTexto = novo.filter { it.isDigit() }.take(4)
                            },
                            label = { Text("Reps Feitas") },
                            placeholder = { Text("$repsPrevistas") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_reps_feitas_summary"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        FilledTonalIconButton(
                            onClick = {
                                val atual = repsFeitasTexto.toIntOrNull() ?: repsPrevistas
                                repsFeitasTexto = (atual + 1).toString()
                            },
                            modifier = Modifier.testTag("btn_reps_plus")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Aumentar 1 rep")
                        }
                    }

                    Text(
                        text = "Padrão inicial preenchido com as repetições previstas ($repsPrevistas reps). Altere se necessário.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Detalhes da Sessão
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Detalhes da Sessão",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Exercício:",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = tipo.titulo,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Padrão de intervalos:",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${padrao.joinToString("s, ")}s por minuto",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Duração oficial Busy Dad:",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$DURACAO_MINUTOS minutos",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Campo de Observações Opcional
            OutlinedTextField(
                value = observacao,
                onValueChange = { observacao = it.take(500) },
                label = { Text("Observação (opcional)") },
                placeholder = { Text("Ex.: Bom ritmo, treino em jejum, 10 reps a mais") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_observacao_summary"),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Diálogo de confirmação para descarte
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = {
                Text(text = "Descartar Treino?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(text = "Os dados desta sessão não serão salvos no histórico.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        app.workoutSessionManager.reset()
                        onFinish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirmar_descarte")
                ) {
                    Text("Sim, Descartar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
