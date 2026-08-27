package com.lucho.dividirgastosia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lucho.dividirgastosia.ui.theme.DividirGastosIATheme

/**
 * Calculadora inspirada en las pilas RPN de las calculadoras HP.
 *
 * Permite apilar hasta [RpnStack.MAX_SIZE] costos (tecla Enter) y muestra
 * en todo momento una casilla con la suma de todos los ítems apilados.
 * La única operación matemática es la suma («+» suma los dos valores más
 * recientes); «Drop» y «Limpiar» solo administran la pila.
 */
@Composable
fun RpnSumCalculatorCard(modifier: Modifier = Modifier) {
    // Pila en céntimos; índice 0 = registro X (último valor apilado).
    val stack = remember { mutableStateListOf<Long>() }
    var input by remember { mutableStateOf("") }

    val inputCents = parseAmountToCents(input)
    val canPush = input.isNotBlank() && inputCents != null && stack.size < RpnStack.MAX_SIZE
    val canAdd = stack.size >= 2

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Calculadora RPN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Apilá hasta ${RpnStack.MAX_SIZE} costos; abajo se ve la " +
                    "suma de todos los ítems. Solo suma.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Pila: 6 lugares; el nivel 1 (abajo) es el último ingreso ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (level in RpnStack.MAX_SIZE downTo 1) {
                        RpnStackSlot(level = level, cents = stack.getOrNull(level - 1))
                    }
                }
            }

            // ── Ingreso del costo + Enter ────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (isValidAmountInput(it)) input = it },
                    label = { Text("Costo") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val cents = inputCents
                        if (cents != null) {
                            val newStack = RpnStack.push(stack.toList(), cents)
                            if (newStack != null) {
                                stack.clear()
                                stack.addAll(newStack)
                                input = ""
                            }
                        }
                    },
                    enabled = canPush
                ) {
                    Text("Enter")
                }
            }

            // ── Única operación: suma. Drop/Limpiar solo manejan la pila ─
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        RpnStack.addTop(stack.toList())?.let { newStack ->
                            stack.clear()
                            stack.addAll(newStack)
                        }
                    },
                    enabled = canAdd,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
                OutlinedButton(
                    onClick = {
                        val newStack = RpnStack.drop(stack.toList())
                        stack.clear()
                        stack.addAll(newStack)
                    },
                    enabled = stack.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Drop")
                }
                OutlinedButton(
                    onClick = { stack.clear() },
                    enabled = stack.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Limpiar")
                }
            }

            Text(
                text = "Enter apila el costo · + suma los dos últimos · " +
                    "Drop borra el último · Limpiar vacía la pila",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (stack.size >= RpnStack.MAX_SIZE) {
                Text(
                    text = "Pila llena (${RpnStack.MAX_SIZE}/${RpnStack.MAX_SIZE}): " +
                        "usá +, Drop o Limpiar para seguir cargando.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // ── Casilla final: suma de todos los ítems de la pila ────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (stack.size) {
                            0 -> "Suma de los ítems"
                            1 -> "Suma de 1 ítem"
                            else -> "Suma de ${stack.size} ítems"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = formatCents(RpnStack.total(stack)),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

/** Un lugar de la pila: número de nivel a la izquierda, importe a la derecha. */
@Composable
private fun RpnStackSlot(level: Int, cents: Long?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = level.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(20.dp)
        )
        Text(
            text = cents?.let { formatCents(it) } ?: "—",
            style = if (level == 1) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyMedium
            },
            fontWeight = if (level == 1) FontWeight.SemiBold else FontWeight.Normal,
            fontStyle = if (cents == null) FontStyle.Italic else FontStyle.Normal,
            color = if (cents == null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RpnSumCalculatorPreview() {
    DividirGastosIATheme(dynamicColor = false) {
        RpnSumCalculatorCard()
    }
}