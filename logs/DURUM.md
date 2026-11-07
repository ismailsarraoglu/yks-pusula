# CI kalite kapısı: FAIL

| İş | Sonuç | Neden |
|---|---|---|
| derle | FAIL | 6 aşama PASS değil: birim-testleri=FAIL, derleme=NOT RUN, release-denetimi=NOT RUN, manifest-debuggable-degil=NOT RUN, manifest-hedef-sdk=NOT RUN, apk-imza=NOT RUN |
| a8 | NOT RUN | iş çalışmadı (derleme kapısı geçilmedi, iptal edildi veya planlanmadı) |

### derle: FAIL

| Aşama | Sonuç | Açıklama |
|---|---|---|
| plan | PASS | cihazlar: a8 |
| birim-testleri | FAIL | 76 test, 1 başarısız, 0 hata, 0 atlandı; GateProbeTest > deliberateFailureWhenAsked FAILED > Task :app:testDebugUnitTest FAILED * What went wrong: |
| derleme | NOT RUN | önceki aşama başarısız olduğu için çalışmadı |
| release-denetimi | NOT RUN | önceki aşama başarısız olduğu için çalışmadı |
| manifest-debuggable-degil | NOT RUN | önceki aşama başarısız olduğu için çalışmadı |
| manifest-hedef-sdk | NOT RUN | önceki aşama başarısız olduğu için çalışmadı |
| apk-imza | NOT RUN | önceki aşama başarısız olduğu için çalışmadı |

### a8: NOT RUN

