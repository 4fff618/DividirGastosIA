package com.lucho.dividirgastosia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RpnStackTest {

    @Test
    fun `push en pila vacia deja el valor como unico elemento`() {
        val stack = RpnStack.push(emptyList(), 1_000L)!!
        assertEquals(listOf(1_000L), stack)
    }

    @Test
    fun `push levanta la pila y el nuevo valor queda en el indice 0`() {
        val stack = RpnStack.push(listOf(300L, 200L, 100L), 400L)!!
        assertEquals(listOf(400L, 300L, 200L, 100L), stack)
    }

    @Test
    fun `push acepta hasta 6 elementos y luego rechaza`() {
        var stack = emptyList<Long>()
        repeat(RpnStack.MAX_SIZE) { i ->
            stack = RpnStack.push(stack, (i + 1).toLong())!!
        }
        assertEquals(RpnStack.MAX_SIZE, stack.size)
        assertNull(RpnStack.push(stack, 99L))
    }

    @Test
    fun `addTop suma los dos ultimos y conserva el resto`() {
        val stack = RpnStack.addTop(listOf(300L, 200L, 100L))!!
        assertEquals(listOf(500L, 100L), stack)
    }

    @Test
    fun `addTop requiere al menos dos valores`() {
        assertNull(RpnStack.addTop(emptyList()))
        assertNull(RpnStack.addTop(listOf(100L)))
    }

    @Test
    fun `addTop no cambia la suma total de la pila`() {
        val antes = listOf(150L, 250L, 600L)
        val despues = RpnStack.addTop(antes)!!
        assertEquals(RpnStack.total(antes), RpnStack.total(despues))
    }

    @Test
    fun `drop quita el ultimo valor apilado`() {
        assertEquals(listOf(200L, 100L), RpnStack.drop(listOf(300L, 200L, 100L)))
        assertTrue(RpnStack.drop(listOf(100L)).isEmpty())
    }

    @Test
    fun `total suma todos los items y devuelve cero si esta vacia`() {
        assertEquals(0L, RpnStack.total(emptyList()))
        assertEquals(1_900L, RpnStack.total(listOf(1_000L, 500L, 400L)))
    }
}