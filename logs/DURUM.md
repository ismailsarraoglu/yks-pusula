# CI kalite kapısı: FAIL

| İş | Sonuç | Neden |
|---|---|---|
| derle | PASS | 7 aşama geçti |
| a8 | FAIL | 1 aşama PASS değil: testler=FAIL |

### derle: PASS

| Aşama | Sonuç | Açıklama |
|---|---|---|
| plan | PASS | cihazlar: a8 |
| birim-testleri | PASS | 76 test, 0 başarısız, 0 hata, 1 atlandı |
| derleme | PASS | debug, sideload, androidTest APK |
| release-denetimi | PASS | beklenen hatayla durdu |
| manifest-debuggable-degil | PASS | sideload APK hata ayıklanabilir değil |
| manifest-hedef-sdk | PASS | targetSdkVersion:'36' |
| apk-imza | PASS | Verifies |

### a8: FAIL

| Aşama | Sonuç | Açıklama |
|---|---|---|
| acilis | PASS | android=8.0.0 api=26 imaj=system-images;android-26;default;x86_64 |
| test-kurulum | PASS | Success / Success |
| testler | FAIL | Tests run: 54,  Failures: 1 1) deliberateFailureWhenAsked(app.ykspusula.GateProbeTest) |
| release-kurulum | PASS | Success |
| release-cokme | PASS | uygulama çökmesi: 0 |
| release-anr | PASS | ANR: 0 |
| release-acik-kaldi | PASS | süreç 3220 |
