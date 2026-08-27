package com.lucho.dividirgastosia

/**
 * Entrada de una persona para el cálculo.
 * Todos los importes están en céntimos para evitar errores de coma flotante.
 */
data class PersonInput(
    val name: String,
    val paidCents: Long,
    val exclusiveCents: Long
)

/**
 * Una transferencia resultante de la liquidación:
 * [from] le paga [amountCents] a [to].
 */
data class Settlement(val from: String, val to: String, val amountCents: Long)

/**
 * Aporte total esperado de una persona:
 * [amountCents] = parte compartida + sus gastos exclusivos ([exclusiveCents]).
 */
data class Share(
    val name: String,
    val amountCents: Long,
    val exclusiveCents: Long
)

/** Balance de una persona: positivo = le deben dinero; negativo = debe dinero. */
data class Balance(val name: String, val amountCents: Long)

sealed interface SplitResult {
    data class Success(
        val shares: List<Share>,
        val balances: List<Balance>,
        val settlements: List<Settlement>
    ) : SplitResult

    data class Error(val message: String) : SplitResult
}

object Calculator {

    /**
     * Divide [totalCents] entre las personas dadas.
     *
     * Modelo:
     *  - El gasto total se reparte en partes iguales: cuota_i = total / N.
     *  - Los gastos exclusivos los cubre solo quien los hizo: aumentan su
     *    aporte personal ([Share.amountCents]) pero NO generan deuda ni
     *    crédito hacia el resto del grupo.
     *  - balance_i = pagado_i − cuota_i  (balance del bote común;
     *    + le deben dinero, − debe dinero). Por construcción Σ balances =
     *    Σ pagados − total, así que si Σ pagados = total la liquidación
     *    cuadra al céntimo.
     *
     * La liquidación usa emparejamiento greedy (mayor deuda ↔ mayor crédito).
     */
    fun calculateSplit(totalCents: Long, people: List<PersonInput>): SplitResult {
        if (people.isEmpty()) {
            return SplitResult.Error("Debe haber al menos una persona.")
        }
        if (totalCents < 0) {
            return SplitResult.Error("El gasto total no puede ser negativo.")
        }

        val n = people.size
        val baseShare = totalCents / n
        val remainder = totalCents % n // céntimos sobrantes a repartir

        val equalParts = people.indices.map { i ->
            baseShare + if (i < remainder) 1 else 0
        }

        // Aporte total esperado = parte compartida + gastos exclusivos propios.
        val shares = people.mapIndexed { index, person ->
            Share(
                name = person.name,
                amountCents = equalParts[index] + person.exclusiveCents,
                exclusiveCents = person.exclusiveCents
            )
        }

        // Balance del bote común: los exclusivos no afectan a nadie más.
        val balances = people.mapIndexed { index, person ->
            Balance(name = person.name, amountCents = person.paidCents - equalParts[index])
        }

        return SplitResult.Success(
            shares = shares,
            balances = balances,
            settlements = settle(balances)
        )
    }

    /**
     * Empareja deudores con acreedores. Devuelve la lista de transferencias
     * que anula todos los balances (si Σ balances == 0). No muta la entrada.
     */
    private fun settle(balances: List<Balance>): List<Settlement> {
        // Pares (nombre, importe pendiente); se van reduciendo con cada transferencia.
        val creditors = balances.filter { it.amountCents > 0 }
            .sortedByDescending { it.amountCents }
            .map { Pair(it.name, it.amountCents) }
            .toMutableList()
        val debtors = balances.filter { it.amountCents < 0 }
            .sortedBy { it.amountCents } // más endeudado primero
            .map { Pair(it.name, -it.amountCents) }
            .toMutableList()

        val settlements = mutableListOf<Settlement>()

        while (creditors.isNotEmpty() && debtors.isNotEmpty()) {
            val (cname, cAmt) = creditors[0]
            val (dname, dAmt) = debtors[0]
            val amount = minOf(cAmt, dAmt)
            if (amount <= 0) break
            settlements.add(Settlement(from = dname, to = cname, amountCents = amount))

            if (cAmt == amount) creditors.removeAt(0)
            else creditors[0] = cname to (cAmt - amount)

            if (dAmt == amount) debtors.removeAt(0)
            else debtors[0] = dname to (dAmt - amount)
        }
        return settlements
    }
}
