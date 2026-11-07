package app.ykspusula

import app.ykspusula.domain.AgeGate
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AgeGateTest {
    private val today = LocalDate.of(2026, 11, 7)

    @Test fun adultOnlyWhenEighteenIsCertain() {
        assertTrue(AgeGate.isVerifiedAdult(2008, 10, today))   // Ekim 2008: 18 yaşını doldurdu
        assertFalse(AgeGate.isVerifiedAdult(2008, 11, today))  // Doğum ayı: gün bilinmediği için ihtiyatlı
        assertFalse(AgeGate.isVerifiedAdult(2008, 12, today))
        assertFalse(AgeGate.isVerifiedAdult(2010, 1, today))
        assertTrue(AgeGate.isVerifiedAdult(1990, 6, today))
    }

    @Test fun unknownOrDeclinedIsNeverAdult() {
        assertFalse(AgeGate.isVerifiedAdult(AgeGate.NOT_SET, 0, today))
        assertFalse(AgeGate.isVerifiedAdult(AgeGate.DECLINED, 0, today))
        assertFalse(AgeGate.isVerifiedAdult(1990, 0, today))
        assertFalse(AgeGate.isVerifiedAdult(1990, 13, today))
    }

    @Test fun freeEntryValidation() {
        assertEquals(2007 to 4, AgeGate.validate("4", "2007", today))
        assertEquals(2007 to 4, AgeGate.validate(" 04 ", "2007", today))
        assertNull(AgeGate.validate("", "2007", today))
        assertNull(AgeGate.validate("13", "2007", today))
        assertNull(AgeGate.validate("0", "2007", today))
        assertNull(AgeGate.validate("5", "2027", today))
        assertNull(AgeGate.validate("12", "2026", today))
        assertNull(AgeGate.validate("5", "1900", today))
    }
}
