package app.ykspusula

import app.ykspusula.domain.*
import org.junit.Assert.*
import org.junit.Test

class AnalysisLogicTest {
    @Test fun mockMissesParseAndKeepOldFormat() {
        val list = MockMisses.parse("t1:2:1, t2, t3:0:4,t1:9:9,,:3:3")
        assertEquals(listOf("t1", "t2", "t3"), list.map { it.topicId })
        assertEquals(TopicMiss("t1", 2, 1), list[0])
        assertEquals(TopicMiss("t2", 0, 0), list[1])
        assertEquals("t1:2:1,t2:0:0,t3:0:4", MockMisses.format(list))
        assertEquals(list, MockMisses.parse(MockMisses.format(list)))
        assertEquals(200, MockMisses.parse("x:5000:0").single().wrong)
        assertTrue(MockMisses.parse("").isEmpty())
    }

    private fun gt(id: String, status: Int = 1, success: Int? = null, due: Boolean = false, mock: Int = 0, open: Int = 0, days: Int? = null, gap: Double? = null) =
        GapTopic(id, id, "s", "Ders", status, success, due, mock, open, days, gap)

    @Test fun priorityUsesOnlyRealSignals() {
        val r = TopicPriority.rank(listOf(gt("bos", status = 1), gt("deneme", mock = 3), gt("tekrar", due = true, days = 20), gt("zayif", success = 30), gt("ogrenildi", status = 3)))
        assertEquals("deneme", r.first().topic.id)
        assertTrue(r.none { it.topic.id == "bos" })
        assertTrue(r.none { it.topic.id == "ogrenildi" })
        assertTrue(r.first { it.topic.id == "tekrar" }.reasons.any { "20 gündür" in it })
        assertTrue(r.first { it.topic.id == "zayif" }.reasons.any { "%30" in it })
        assertTrue(r.all { it.reasons.isNotEmpty() })
    }

    @Test fun finalModeDropsUnstartedBonus() {
        val list = listOf(gt("yeni", status = 0))
        assertEquals(1, TopicPriority.rank(list).size)
        assertTrue(TopicPriority.rank(list, final = true).isEmpty())
    }

    @Test fun learnedTopicReturnsWhenWrongAgain() {
        assertTrue(TopicPriority.rank(listOf(gt("x", status = 3))).isEmpty())
        assertEquals(1, TopicPriority.rank(listOf(gt("x", status = 3, open = 2))).size)
    }

    @Test fun gapReasonShown() {
        val r = TopicPriority.rank(listOf(gt("g", gap = 4.5)))
        assertTrue(r.single().reasons.any { "4,50" in it })
    }
}
