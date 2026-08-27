package com.lucho.dividirgastosia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lucho.dividirgastosia.ui.theme.DividirGastosIATheme
import java.math.RoundingMode
import java.util.Locale

private val SPANISH_LOCALE: Locale = Locale.forLanguageTag("es-AR")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DividirGastosIATheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ExpenseSplitterScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/** Estado de una fila de amigo. */
class FriendRow(
    name: String = "",
    paid: String = "",
    exclusive: String = ""
) {
    var name by mutableStateOf(name)
    var paid by mutableStateOf(paid)
    var exclusive by mutableStateOf(exclusive)

    fun toInput(defaultNameIndex: Int): PersonInput? {
        val paidCents = parseAmountToCents(paid) ?: return null
        val exclusiveCents = parseAmountToCents(exclusive) ?: return null
        val finalName = name.trim().ifEmpty { "Persona ${defaultNameIndex + 1}" }
        return PersonInput(finalName, paidCents, exclusiveCents)
    }
}


/** Regex para números con puntos como separador de miles: «1.500», «12.345.678». */
private val THOUSANDS_ONLY = Regex("^\\d{1,3}(\\.\\d{3})+$")

/**
 * true si [text] es un importe en construcción válido:
 * solo dígitos, puntos de miles y una coma decimal con hasta 2 decimales.
 */
internal fun isValidAmountInput(text: String): Boolean {
    if (text.isEmpty()) return true
    // Solo dígitos, punto y coma.
    if (!text.all { it.isDigit() || it == '.' || it == ',' }) return false
    // Máximo una coma, con hasta 2 decimales después (si hay algo).
    val parts = text.split(',')
    if (parts.size > 2) return false
    val decimals = parts.getOrNull(1) ?: ""
    if (decimals.length > 2) return false
    return true
}

/** Convierte «12», «12,5», «1.500» o «1.234,56» a céntimos; vacío → 0; inválido → null. */
internal fun parseAmountToCents(text: String): Long? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return 0L

    // Con coma decimal, los puntos son separadores de miles.
    val normalized = if (trimmed.contains(',')) {
        trimmed.replace(".", "").replace(',', '.')
    } else if (THOUSANDS_ONLY.matches(trimmed)) {
        trimmed.replace(".", "")
    } else {
        trimmed
    }

    if (!normalized.all { it.isDigit() || it == '.' }) return null
    if (normalized.count { it == '.' } > 1) return null
    val bd = normalized.toBigDecimalOrNull() ?: return null
    return runCatching {
        val cents = bd.movePointRight(2).setScale(0, RoundingMode.HALF_UP)
        val asLong = cents.toBigInteger().toLong()
        require(cents == java.math.BigDecimal.valueOf(asLong))
        asLong
    }.getOrNull()
}

/** Formatea céntimos como pesos al estilo argentino: 123450 → "$ 1.234,50". */
internal fun formatCents(cents: Long): String =
    "$ %,.2f".format(SPANISH_LOCALE, cents / 100.0)

@Composable
fun ExpenseSplitterScreen(modifier: Modifier = Modifier) {
    // Por defecto 2 personas
    var personCount by remember { mutableIntStateOf(2) }
    val friends = remember { mutableStateListOf(FriendRow(), FriendRow()) }

    // Sincroniza el tamaño de la lista con personCount
    while (friends.size < personCount) friends.add(FriendRow())
    while (friends.size > personCount && friends.size > 1) friends.removeAt(friends.size - 1)

    var totalText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<SplitResult?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Dividir gastos entre amigos",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )

        HorizontalDivider()

        // ── Número de personas ────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Personas:", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { if (personCount > 1) personCount-- }) {
                Text("−", style = MaterialTheme.typography.titleLarge)
            }
            Text("$personCount", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = { if (personCount < 20) personCount++ }) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }

        // ── Gasto total ───────────────────────────────────────────────────
        OutlinedTextField(
            value = totalText,
            onValueChange = { if (isValidAmountInput(it)) totalText = it },
            label = { Text("Gasto total") },
            prefix = { Text("$") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // ── Filas por amigo ───────────────────────────────────────────────
        friends.forEachIndexed { index, friend ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = friend.name.trim().ifEmpty { "Amigo ${index + 1}" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = friend.name,
                        onValueChange = { friend.name = it },
                        label = { Text("Nombre (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = friend.paid,
                        onValueChange = { if (isValidAmountInput(it)) friend.paid = it },
                        label = { Text("¿Cuánto pagó?") },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = friend.exclusive,
                        onValueChange = { if (isValidAmountInput(it)) friend.exclusive = it },
                        label = {
                            Text(
                                "Gasto exclusivo de " +
                                    friend.name.trim().ifEmpty { "este amigo" }
                            )
                        },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ── Botón calcular ────────────────────────────────────────────────
        Button(
            onClick = {
                val totalCents = parseAmountToCents(totalText)
                if (totalText.isNotBlank() && totalCents == null) {
                    result = SplitResult.Error("El gasto total no es un número válido.")
                } else {
                    var errorIndex = -1
                    val inputs = friends.mapIndexed { i, f ->
                        f.toInput(i).also { if (it == null && errorIndex == -1) errorIndex = i }
                    }
                    result = if (errorIndex >= 0) {
                        SplitResult.Error("Revisa los importes del amigo ${errorIndex + 1}.")
                    } else {
                        Calculator.calculateSplit(totalCents ?: 0L, inputs.requireNoNulls())
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calcular")
        }

        // ── Resultado ─────────────────────────────────────────────────────
        result?.let { res ->
            ResultCard(result = res)
        }

        Text(
            text = "El gasto total se divide en partes iguales; los gastos " +
                "exclusivos los cubre solo quien los hizo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

@Composable
fun ResultCard(result: SplitResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (result) {
                is SplitResult.Error -> MaterialTheme.colorScheme.errorContainer
                is SplitResult.Success -> MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (result) {
                is SplitResult.Error -> {
                    Text(
                        text = result.message,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                is SplitResult.Success -> {
                    Text(
                        text = "Resultado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cuotas y balances:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    result.shares.forEachIndexed { i, share ->
                        val expected = share.amountCents
                        val balance = result.balances[i].amountCents
                        val balanceText = when {
                            balance > 0 -> "le deben ${formatCents(balance)}"
                            balance < 0 -> "debe ${formatCents(-balance)}"
                            else -> "está al día"
                        }
                        Text(
                            text = "${share.name}: aporta ${formatCents(expected)} ($balanceText)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (result.settlements.isEmpty()) {
                        Text(
                            text = "🎉 ¡Todos están al día!",
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "Liquidación:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        result.settlements.forEach { s ->
                            Text(
                                text = "${s.from} le paga ${formatCents(s.amountCents)} a ${s.to}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseSplitterPreview() {
    DividirGastosIATheme(dynamicColor = false) {
        ExpenseSplitterScreen()
    }
}
