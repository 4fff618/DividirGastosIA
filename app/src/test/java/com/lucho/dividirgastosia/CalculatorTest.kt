package com.lucho.dividirgastosia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorTest {

    @Test
    fun `split exacto entre dos personas`() {
        val result = Calculator.calculateSplit(
            totalCents = 10_000L,
            people = listOf(
                PersonInput("A", paidCents = 10_000L, exclusiveCents = 0),
                PersonInput("B", paidCents = 0, exclusiveCents = 0),
            )
        )
        val success = result as SplitResult.Success
        assertEquals(listOf(5_000L, 5_000L), success.shares.map { it.amountCents })
        assertEquals(5_000L, success.balances[0].amountCents)
        assertEquals(-5_000L, success.balances[1].amountCents)
    }

    @Test
    fun `reparte el resto de centimos entre las primeras personas`() {
        // 100 € entre 3 personas → 3333,34 / 3333,33 / 3333,33 céntimos
        val result = Calculator.calculateSplit(
            totalCents = 10_000L,
            people = List(3) { PersonInput("P$it", 0, 0) }
        ) as SplitResult.Success

        assertEquals(listOf(3_334L, 3_333L, 3_333L), result.shares.map { it.amountCents })
        assertEquals(10_000L, result.shares.sumOf { it.amountCents })
    }

    @Test
    fun `los gastos exclusivos no afectan al resto`() {
        // Cena compartida 90 €; C se tomó un postre de 10 € pagado aparte.
        val result = Calculator.calculateSplit(
            totalCents = 9_000L,
            people = listOf(
                PersonInput("A", 6_000L, 0),
                PersonInput("B", 3_000L, 0),
                PersonInput("C", 0, 1_000L)
            )
        ) as SplitResult.Success

        // Aporte esperado: A=30 €, B=30 €, C=30+10=40 € (postre incluido).
        assertEquals(
            listOf(3_000L, 3_000L, 4_000L),
            result.shares.map { it.amountCents }
        )
        // Balance del bote común: el postre de C no genera deuda a nadie.
        assertEquals(3_000L, result.balances[0].amountCents)   // A: +30 €
        assertEquals(0L, result.balances[1].amountCents)       // B: 0
        assertEquals(-3_000L, result.balances[2].amountCents)  // C: −30 €
        // C le paga 30 € a A (el postre ya lo cubrió él mismo).
        assertEquals(1, result.settlements.size)
        assertEquals("C", result.settlements[0].from)
        assertEquals("A", result.settlements[0].to)
        assertEquals(3_000L, result.settlements[0].amountCents)
    }

    @Test
    fun `liquidacion cuadra con varios deudores y acreedores`() {
        // Total 150 € entre 4 personas (37,50 cada uno), adelantaron:
        // acreedor1 80 € y acreedor2 70 €; los deudores 0 €.
        val people = listOf(
            PersonInput("acreedor1", 8_000L, 0),
            PersonInput("acreedor2", 7_000L, 0),
            PersonInput("deudor1", 0, 0),
            PersonInput("deudor2", 0, 0)
        )
        val result = Calculator.calculateSplit(totalCents = 15_000L, people) as SplitResult.Success

        // balances: +4250 / +3250 / −3750 / −3750
        val settlements = result.settlements
        assertEquals(3, settlements.size)

        // Invariante: las transferencias anulan el balance neto de cada persona.
        assertSettlementsCancelBalances(result.balances, settlements)
    }

    @Test
    fun `invariante con exclusivos y pagos mixtos`() {
        // Σ pagados == total ⇒ la liquidación debe cancelar TODOS los balances.
        val people = listOf(
            PersonInput("Ana", 5_500L, 1_200L),   // paga 55, aporta 12 exclusivo
            PersonInput("Beto", 2_000L, 0),
            PersonInput("Caro", 0L, 800L),        // su exclusivo lo cubre ella
            PersonInput("Dani", 1_300L, 0)
        )
        val totalCents = 8_800L                  // Σ pagados = 88 €
        val result = Calculator.calculateSplit(totalCents, people) as SplitResult.Success

        // Conservación: Σ balance = Σ pagados − total = 0
        assertEquals(0L, result.balances.sumOf { it.amountCents })
        assertSettlementsCancelBalances(result.balances, result.settlements)
    }

    @Test
    fun `todos al dia produce cero liquidaciones`() {
        val result = Calculator.calculateSplit(
            totalCents = 2_000L,
            people = listOf(
                PersonInput("A", 1_000L, 0),
                PersonInput("B", 1_000L, 0)
            )
        ) as SplitResult.Success

        assertTrue(result.settlements.isEmpty())
        assertTrue(result.balances.all { it.amountCents == 0L })
    }

    @Test
    fun `una sola persona se paga todo ella misma`() {
        val result = Calculator.calculateSplit(
            totalCents = 5_000L,
            people = listOf(PersonInput("Solo", 5_000L, 0))
        ) as SplitResult.Success

        assertTrue(result.settlements.isEmpty())
        assertEquals(0L, result.balances[0].amountCents)
    }

    @Test
    fun `lista vacia devuelve error`() {
        val result = Calculator.calculateSplit(totalCents = 100L, people = emptyList())
        assertTrue(result is SplitResult.Error)
    }

    @Test
    fun `total negativo devuelve error`() {
        val result = Calculator.calculateSplit(
            totalCents = -1L,
            people = listOf(PersonInput("A", 0, 0))
        )
        assertTrue(result is SplitResult.Error)
    }

    /** Verifica que las transferencias dejan el balance neto de todos en cero. */
    private fun assertSettlementsCancelBalances(
        balances: List<Balance>,
        settlements: List<Settlement>
    ) {
        val net = mutableMapOf<String, Long>()
        for (s in settlements) {
            net.merge(s.from, -s.amountCents, Long::plus)
            net.merge(s.to, s.amountCents, Long::plus)
        }
        for (b in balances) {
            assertEquals(b.amountCents, net[b.name] ?: 0L)
        }
    }
}
