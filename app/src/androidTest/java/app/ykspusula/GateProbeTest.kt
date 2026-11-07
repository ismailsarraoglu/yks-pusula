package app.ykspusula

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Kalite kapısı denemesi: yalnızca "-e kapi cihaz" verildiğinde kasıtlı olarak başarısız olur.
 * CI'ın cihaz testindeki bir hatayı gerçekten başarısız saydığını kanıtlamak için kullanılır.
 */
@RunWith(AndroidJUnit4::class)
class GateProbeTest {
    @Test fun deliberateFailureWhenAsked() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("kapi") == "cihaz")
        fail("Kasıtlı hata: kalite kapısı denemesi")
    }
}
