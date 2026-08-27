package com.lucho.dividirgastosia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTest {

    @Test
    fun `formato de pesos con miles y decimales`() {
        assertEquals("$ 1.234,50", formatCents(123_450L))
        assertEquals("$ 0,99", formatCents(99L))
        assertEquals("$ 12.345.678,90", formatCents(1_234_567_890L))
    }

    @Test
    fun `parseo de importes con separadores argentinos`() {
        assertEquals(0L, parseAmountToCents(""))
        assertEquals(100_00L, parseAmountToCents("100"))
        assertEquals(125_0L, parseAmountToCents("12,5"))
        assertEquals(125_0L, parseAmountToCents("12.5"))
        // Punto seguido de exactamente 3 dígitos = miles
        assertEquals(150_000L, parseAmountToCents("1.500"))
        assertEquals(150L, parseAmountToCents("1.5"))
        // Con coma, los puntos son siempre miles
        assertEquals(123_456L, parseAmountToCents("1.234,56"))
        assertNull(parseAmountToCents("abc"))
        assertNull(parseAmountToCents("1.2.3"))
    }

    @Test
    fun `filtro de entrada valida solo digitos separadores y 2 decimales`() {
        // Entradas válidas
        assertTrue(isValidAmountInput(""))          // vacío ok (borrado)
        assertTrue(isValidAmountInput("123"))       // solo dígitos
        assertTrue(isValidAmountInput("1.500"))     // punto de miles
        assertTrue(isValidAmountInput("12,5"))      // coma decimal
        assertTrue(isValidAmountInput("12,50"))     // hasta 2 decimales
        assertTrue(isValidAmountInput("1.234,56"))  // formato completo

        // Entradas inválidas (se rechazan y no llegan al campo)
        assertFalse(isValidAmountInput("abc"))      // letras
        assertFalse(isValidAmountInput("12a"))      // mezcla letra/número
        assertFalse(isValidAmountInput("12,345"))   // más de 2 decimales
        assertFalse(isValidAmountInput("1,2,3"))    // más de una coma
        assertFalse(isValidAmountInput("1-2"))      // signo no permitido
    }
}
