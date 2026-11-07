# CI kalite kapısı: FAIL

| İş | Sonuç | Neden |
|---|---|---|
| derle | PASS | 7 aşama geçti |
| a8 | FAIL | 1 aşama PASS değil: acilis=FAIL |

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
| acilis | FAIL | emülatör açılmadı (system-images;android-99;olmayan-imaj;x86_64) |
