package com.sbfinance.app.logic

import com.sbfinance.app.data.Entry
import com.sbfinance.app.data.nextMonth
import org.junit.Assert.*
import org.junit.Test

class FinanceLogicTest {
    @Test fun brazilianMoney() {
        assertEquals(500L, amountToCents("5"))
        assertEquals(123456L, amountToCents("1.234,56"))
        assertEquals(250000L, amountToCents("2.500"))
        assertNull(amountToCents("-1"))
    }
    @Test fun naturalLanguage() {
        val expense = parseNatural("Gastei 5 reais com pastel")!!
        assertEquals(500L, expense.cents)
        assertEquals("Alimentação", expense.category)
        val income = parseNatural("Recebi 2500 de salário")!!
        assertEquals(250000L, income.cents)
        assertEquals("income", income.kind)
    }
    @Test fun preserveDueDay() {
        assertEquals("2027-02-28", nextMonth("2027-01-31", 31))
        assertEquals("2027-03-31", nextMonth("2027-02-28", 31))
    }
    @Test fun sums() {
        val es = listOf(Entry(title="Renda", cents=250000, kind="income", category="Outros"), Entry(title="Pastel", cents=500, kind="expense", category="Alimentação"))
        assertEquals(249500L,balance(es))
    }
}
