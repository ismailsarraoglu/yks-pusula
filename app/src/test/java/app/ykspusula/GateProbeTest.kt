package app.ykspusula

import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Kalite kapısı denemesi: yalnızca KAPI_DENEMESI=birim ortam değişkeniyle kasıtlı olarak başarısız olur. */
class GateProbeTest {
    @Test fun deliberateFailureWhenAsked() {
        assumeTrue(System.getenv("KAPI_DENEMESI") == "birim")
        fail("Kasıtlı hata: kalite kapısı denemesi")
    }
}
