package tr.bumba.time

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pomodoro yapılacaklar listesi (#16): ekleme, tekrarı önleme, tur sayacı, temizleme */
class PomoTodoTest {
    @Test fun addCreditClear() {
        var l = PomoTodo.add(emptyList(), "  Rapor  ")
        l = PomoTodo.add(l, "Rapor") // aynı bitmemiş görev ikinci kez eklenmez
        l = PomoTodo.add(l, "")
        l = PomoTodo.add(l, "Kod")
        assertEquals(listOf("Rapor", "Kod"), l.map { it.t })
        l = PomoTodo.credit(l, "Rapor")
        l = PomoTodo.credit(l, "Rapor ")
        l = PomoTodo.credit(l, "Başka")
        assertEquals(listOf(2, 0), l.map { it.n })
        l = PomoTodo.toggle(l, l[0].id)
        // Biten görev artık sayılmaz; aynı adla yeni görev eklenebilir
        assertEquals(l, PomoTodo.credit(l, "Rapor"))
        assertEquals(3, PomoTodo.add(l, "Rapor").size)
        assertEquals(listOf("Kod"), PomoTodo.clearDone(l).map { it.t })
        var many = emptyList<PomoTodo.Item>()
        repeat(55) { many = PomoTodo.add(many, "g$it") }
        assertEquals(50, many.size)
        assertEquals("g5", many.first().t)
    }
}
