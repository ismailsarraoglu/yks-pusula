# Veritabanı geçişi (Room 1 → 2)

Dosyalar: `app/src/main/java/app/ykspusula/data/Migrations.kt`, `app/schemas/app.ykspusula.data.AppDatabase/1.json`, `2.json`. Test: `app/src/androidTest/java/app/ykspusula/MigrationTest.kt`.

Geçiş yıkıcı değildir. `fallbackToDestructiveMigration` kullanılmaz.

## Kaldırılan tablolar

Question, AnswerOption, QuestionAttempt, AnswerRecord, WrongQuestion.

## Silinmeden önce korunan veri

| Eski veri | Yeni yeri |
|---|---|
| Soru denemeleri (QuestionAttempt) | Gün, ders ve konu bazında toplanıp `LEGACY` kaynaklı PracticeLog satırları olur. Doğru, yanlış, boş ve süre korunur |
| Yarım kalan (RUNNING) denemeler | Cevaplanan sorular üzerinden sonucu hesaplanır, deneme tamamlandı olur ve notuna durum yazılır |
| WrongQuestion | Soru metni ve açıklamayla WrongEntry olur; tekrar tarihi korunur |
| QUESTION ve LATER yer imleri | Bağlı olduğu soru kalmadığı için silinir |
| Konu durumu 2 (soru çözüldü) | Konu çalışıldı (1) durumuna iner; ustalık yeni kayıtlarla hesaplanır |

Değişmeden kalan tablolar (yeni alanlar varsayılan değerle eklendi): Bookmark, ExamConfiguration, Flashcard, FlashcardProgress, Formula, ImportedContentPack, LibraryFile, MockExam, MockExamResult, Reminder, StudyPlan, StudySession, StudyTask, Subject, TargetDepartment, TargetUniversity, Topic, TopicProgress, UniversityRecord, UserNote, UserProfile.

## Eklenen tablolar

PracticeLog, StudySource, WrongEntry, WrongReview, RouteItem, PlanRevision, CardReview, ItemNote, NetGoal, RestDay.

## Bilinen kayıp

Eski yanlış kaydında sorunun görseli veya şekli varsa görsel gelmez, çünkü gömülü soru bankası kaldırıldı. Soru metni ve açıklama gelir.

Eski yedekler (biçim 1, JSON) `BackupDataV1` üzerinden yeni biçime çevrilerek okunur.
