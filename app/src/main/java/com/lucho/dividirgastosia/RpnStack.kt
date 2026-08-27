package com.lucho.dividirgastosia

/**
 * Pila RPN de tamaño fijo para la calculadora de costos.
 *
 * Convención: el índice 0 de la lista es el registro X (el último valor
 * apilado); los valores anteriores quedan por encima (índices 1, 2, …).
 * La pila admite como máximo [MAX_SIZE] elementos y la única operación
 * matemática permitida es la suma.
 */
object RpnStack {

    /** Cantidad máxima de lugares de la pila. */
    const val MAX_SIZE = 6

    /**
     * Apila [value] en el registro X y levanta el resto de la pila.
     * Devuelve la nueva pila, o null si está llena (operación rechazada).
     */
    fun push(stack: List<Long>, value: Long): List<Long>? {
        if (stack.size >= MAX_SIZE) return null
        return listOf(value) + stack
    }

    /**
     * Suma los dos valores más recientes (X + Y) y los reemplaza por el
     * resultado en el registro X. Devuelve la nueva pila, o null si hay
     * menos de dos valores apilados.
     */
    fun addTop(stack: List<Long>): List<Long>? {
        if (stack.size < 2) return null
        return listOf(stack[0] + stack[1]) + stack.drop(2)
    }

    /** Quita el último valor apilado (registro X). */
    fun drop(stack: List<Long>): List<Long> = stack.drop(1)

    /** Suma de todos los valores apilados; 0 si la pila está vacía. */
    fun total(stack: List<Long>): Long = stack.sum()
}