package app.ykspusula.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Soru bankasını kaldıran geçiş. Sıra önemlidir:
 * önce yeni tablolar açılır, kişisel kayıtlar taşınır, en son eski soru tabloları silinir.
 * Eski cevaplar gün ve konu bazında performans kaydına, eski yanlışlar soru metniyle birlikte deftere dönüşür.
 * Yarım kalmış yerleşik denemeler cevaplanan sorulara göre tamamlanır.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        SCHEMA_1_2.forEach { db.execSQL(it) }
        DATA_1_2.forEach { db.execSQL(it) }
    }
}

private val SCHEMA_1_2 = listOf(
        "CREATE TABLE IF NOT EXISTS `StudySource` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `publisher` TEXT NOT NULL, `type` TEXT NOT NULL, `subjectId` TEXT, `topicId` TEXT, `description` TEXT NOT NULL, `totalPages` INTEGER NOT NULL, `startDate` TEXT NOT NULL, `endDate` TEXT NOT NULL, `lastPage` INTEGER NOT NULL, `pagesDone` INTEGER NOT NULL, `favorite` INTEGER NOT NULL, `archived` INTEGER NOT NULL, `note` TEXT NOT NULL, `libraryFileId` TEXT, `lastUsedAt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        "CREATE INDEX IF NOT EXISTS `index_StudySource_subjectId` ON `StudySource` (`subjectId`)",
        "CREATE INDEX IF NOT EXISTS `index_StudySource_topicId` ON `StudySource` (`topicId`)",
        "CREATE INDEX IF NOT EXISTS `index_StudySource_libraryFileId` ON `StudySource` (`libraryFileId`)",
        "CREATE TABLE IF NOT EXISTS `PracticeLog` (`id` TEXT NOT NULL, `at` INTEGER NOT NULL, `subjectId` TEXT, `topicId` TEXT, `sourceId` TEXT, `sessionId` TEXT, `mockExamId` TEXT, `pageFrom` INTEGER, `pageTo` INTEGER, `correct` INTEGER NOT NULL, `wrong` INTEGER NOT NULL, `blank` INTEGER NOT NULL, `origin` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`sourceId`) REFERENCES `StudySource`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
        "CREATE INDEX IF NOT EXISTS `index_PracticeLog_at` ON `PracticeLog` (`at`)",
        "CREATE INDEX IF NOT EXISTS `index_PracticeLog_topicId` ON `PracticeLog` (`topicId`)",
        "CREATE INDEX IF NOT EXISTS `index_PracticeLog_subjectId` ON `PracticeLog` (`subjectId`)",
        "CREATE INDEX IF NOT EXISTS `index_PracticeLog_sourceId` ON `PracticeLog` (`sourceId`)",
        "CREATE INDEX IF NOT EXISTS `index_PracticeLog_mockExamId` ON `PracticeLog` (`mockExamId`)",
        "CREATE INDEX IF NOT EXISTS `index_PracticeLog_sessionId` ON `PracticeLog` (`sessionId`)",
        "CREATE TABLE IF NOT EXISTS `WrongEntry` (`id` TEXT NOT NULL, `subjectId` TEXT, `topicId` TEXT, `sourceId` TEXT, `sourceLabel` TEXT NOT NULL, `page` INTEGER, `questionNo` TEXT NOT NULL, `reason` TEXT NOT NULL, `note` TEXT NOT NULL, `photoFile` TEXT, `questionText` TEXT NOT NULL, `givenAnswer` TEXT NOT NULL, `correctAnswer` TEXT NOT NULL, `firstWrongAt` INTEGER NOT NULL, `lastWrongAt` INTEGER NOT NULL, `nextReviewAt` INTEGER NOT NULL, `stage` INTEGER NOT NULL, `wrongCount` INTEGER NOT NULL, `resolved` INTEGER NOT NULL, `origin` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`sourceId`) REFERENCES `StudySource`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
        "CREATE INDEX IF NOT EXISTS `index_WrongEntry_nextReviewAt` ON `WrongEntry` (`nextReviewAt`)",
        "CREATE INDEX IF NOT EXISTS `index_WrongEntry_topicId` ON `WrongEntry` (`topicId`)",
        "CREATE INDEX IF NOT EXISTS `index_WrongEntry_subjectId` ON `WrongEntry` (`subjectId`)",
        "CREATE INDEX IF NOT EXISTS `index_WrongEntry_sourceId` ON `WrongEntry` (`sourceId`)",
        "CREATE TABLE IF NOT EXISTS `WrongReview` (`id` TEXT NOT NULL, `wrongId` TEXT NOT NULL, `at` INTEGER NOT NULL, `result` INTEGER NOT NULL, `stageAfter` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`wrongId`) REFERENCES `WrongEntry`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_WrongReview_wrongId` ON `WrongReview` (`wrongId`)",
        "CREATE INDEX IF NOT EXISTS `index_WrongReview_at` ON `WrongReview` (`at`)",
        "CREATE TABLE IF NOT EXISTS `RouteItem` (`id` TEXT NOT NULL, `date` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, `kind` TEXT NOT NULL, `title` TEXT NOT NULL, `subjectId` TEXT, `topicId` TEXT, `sourceId` TEXT, `minutes` INTEGER NOT NULL, `priority` INTEGER NOT NULL, `reason` TEXT NOT NULL, `status` TEXT NOT NULL, `movedTo` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        "CREATE INDEX IF NOT EXISTS `index_RouteItem_date` ON `RouteItem` (`date`)",
        "CREATE TABLE IF NOT EXISTS `PlanRevision` (`id` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `summary` TEXT NOT NULL, `snapshotJson` TEXT NOT NULL, PRIMARY KEY(`id`))",
        "CREATE TABLE IF NOT EXISTS `CardReview` (`id` TEXT NOT NULL, `cardId` TEXT NOT NULL, `at` INTEGER NOT NULL, `rating` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        "CREATE INDEX IF NOT EXISTS `index_CardReview_cardId` ON `CardReview` (`cardId`)",
        "CREATE INDEX IF NOT EXISTS `index_CardReview_at` ON `CardReview` (`at`)",
        "CREATE TABLE IF NOT EXISTS `ItemNote` (`kind` TEXT NOT NULL, `refId` TEXT NOT NULL, `text` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`kind`, `refId`))",
        "CREATE TABLE IF NOT EXISTS `NetGoal` (`exam` TEXT NOT NULL, `section` TEXT NOT NULL, `target` REAL NOT NULL, PRIMARY KEY(`exam`, `section`))",
        "CREATE TABLE IF NOT EXISTS `RestDay` (`date` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`date`))",
        "ALTER TABLE `UserProfile` ADD COLUMN maxDailyMinutes INTEGER NOT NULL DEFAULT 360",
        "ALTER TABLE `UserProfile` ADD COLUMN breakMinutes INTEGER NOT NULL DEFAULT 10",
        "ALTER TABLE `UserProfile` ADD COLUMN endHour INTEGER NOT NULL DEFAULT 22",
        "ALTER TABLE `UserProfile` ADD COLUMN priorityTopics TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE `ExamConfiguration` ADD COLUMN examYear INTEGER NOT NULL DEFAULT 2027",
        "ALTER TABLE `ExamConfiguration` ADD COLUMN curriculum TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE `ExamConfiguration` ADD COLUMN sourceUrl TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE `ExamConfiguration` ADD COLUMN lastUpdated TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE `ExamConfiguration` ADD COLUMN obpMultiplier REAL NOT NULL DEFAULT 5.0",
        "ALTER TABLE `ExamConfiguration` ADD COLUMN obpCoefficient REAL NOT NULL DEFAULT 0.12",
        "ALTER TABLE `ExamConfiguration` ADD COLUMN obpCoefficientPlaced REAL NOT NULL DEFAULT 0.06",
        "ALTER TABLE `StudyTask` ADD COLUMN reason TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE `StudyTask` ADD COLUMN priority INTEGER NOT NULL DEFAULT 2",
        "ALTER TABLE `StudyTask` ADD COLUMN sourceId TEXT",
        "ALTER TABLE `StudyTask` ADD COLUMN originalDate TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE `StudyTask` ADD COLUMN movedCount INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `StudyTask` ADD COLUMN skipped INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `StudyTask` ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `StudySession` ADD COLUMN sourceId TEXT",
        "ALTER TABLE `StudySession` ADD COLUMN plannedMinutes INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `StudySession` ADD COLUMN activity TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE `MockExam` ADD COLUMN pausedAt INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `MockExam` ADD COLUMN pausedTotalMs INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `MockExam` ADD COLUMN hardTopics TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE `MockExam` ADD COLUMN subjectId TEXT",
        "ALTER TABLE `MockExam` ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `TopicProgress` ADD COLUMN reviewCount INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `TopicProgress` ADD COLUMN lastReviewAt INTEGER",
        "ALTER TABLE `TopicProgress` ADD COLUMN checks TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE `UserNote` ADD COLUMN tags TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE `Reminder` ADD COLUMN frequency TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE `TargetDepartment` ADD COLUMN targetTytNet REAL",
        "ALTER TABLE `TargetDepartment` ADD COLUMN targetAytNet REAL",
        "ALTER TABLE `TargetDepartment` ADD COLUMN dataSource TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE `TargetDepartment` ADD COLUMN dataYear INTEGER",
        "ALTER TABLE `TargetDepartment` ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `LibraryFile` ADD COLUMN subjectId TEXT",
        "ALTER TABLE `LibraryFile` ADD COLUMN topicId TEXT",
        "ALTER TABLE `LibraryFile` ADD COLUMN lastOpenedAt INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `LibraryFile` ADD COLUMN lastPage INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `LibraryFile` ADD COLUMN pageCount INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `LibraryFile` ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `LibraryFile` ADD COLUMN note TEXT NOT NULL DEFAULT ''",
)

private val DATA_1_2 = listOf(
        """UPDATE UserProfile SET maxDailyMinutes = MAX(maxDailyMinutes, dailyMinutes)""",
        """INSERT OR IGNORE INTO PracticeLog (id, at, subjectId, topicId, sourceId, sessionId, mockExamId, pageFrom, pageTo, correct, wrong, blank, origin, note, createdAt, updatedAt)
SELECT 'legacy-' || date(answeredAt / 1000, 'unixepoch', 'localtime') || '-' || topicId || '-' || COALESCE(mockExamId, ''), MIN(answeredAt), subjectId, topicId, NULL, NULL, mockExamId, NULL, NULL,
SUM(CASE WHEN selected IS NOT NULL AND isCorrect = 1 THEN 1 ELSE 0 END), SUM(CASE WHEN selected IS NOT NULL AND isCorrect = 0 THEN 1 ELSE 0 END), SUM(CASE WHEN selected IS NULL THEN 1 ELSE 0 END),
'LEGACY', '', MIN(answeredAt), MIN(answeredAt)
FROM QuestionAttempt GROUP BY date(answeredAt / 1000, 'unixepoch', 'localtime'), topicId, mockExamId""",
        """INSERT OR IGNORE INTO MockExamResult (mockExamId, section, correct, wrong, blank, net, sortOrder)
SELECT r.mockExamId, r.sectionName, SUM(CASE WHEN r.selected IS NOT NULL AND o.isCorrect = 1 THEN 1 ELSE 0 END), SUM(CASE WHEN r.selected IS NOT NULL AND COALESCE(o.isCorrect, 0) = 0 THEN 1 ELSE 0 END),
SUM(CASE WHEN r.selected IS NULL THEN 1 ELSE 0 END),
SUM(CASE WHEN r.selected IS NOT NULL AND o.isCorrect = 1 THEN 1.0 ELSE 0.0 END) - SUM(CASE WHEN r.selected IS NOT NULL AND COALESCE(o.isCorrect, 0) = 0 THEN 1.0 ELSE 0.0 END) / COALESCE((SELECT c.wrongDivisor FROM ExamConfiguration c WHERE c.code = m.exam), 4.0),
MIN(r.orderIndex)
FROM AnswerRecord r JOIN MockExam m ON m.id = r.mockExamId LEFT JOIN AnswerOption o ON o.questionId = r.questionId AND o.label = r.selected
WHERE m.status = 'RUNNING' GROUP BY r.mockExamId, r.sectionName""",
        """INSERT OR IGNORE INTO PracticeLog (id, at, subjectId, topicId, sourceId, sessionId, mockExamId, pageFrom, pageTo, correct, wrong, blank, origin, note, createdAt, updatedAt)
SELECT 'mock-' || x.mockExamId || '-' || x.section, m.createdAt, NULL, NULL, NULL, NULL, x.mockExamId, NULL, NULL, x.correct, x.wrong, x.blank, 'MOCK', '', m.createdAt, m.createdAt
FROM MockExamResult x JOIN MockExam m ON m.id = x.mockExamId WHERE m.status = 'RUNNING'""",
        """UPDATE MockExam SET status = 'DONE', note = 'Önceki sürümde yarım kalan deneme; cevaplanan sorular üzerinden hesaplandı.' WHERE status = 'RUNNING'""",
        """INSERT OR IGNORE INTO WrongEntry (id, subjectId, topicId, sourceId, sourceLabel, page, questionNo, reason, note, photoFile, questionText, givenAnswer, correctAnswer, firstWrongAt, lastWrongAt, nextReviewAt, stage, wrongCount, resolved, origin, updatedAt)
SELECT 'legacy-' || w.questionId, w.subjectId, w.topicId, NULL, 'Önceki sürümün soru bankası', NULL, '', w.reason, w.note, NULL,
COALESCE(q.stem, 'Bu kayıt önceki sürümden taşındı.') || CASE WHEN q.explanation IS NOT NULL AND q.explanation != '' THEN char(10) || char(10) || 'Açıklama: ' || q.explanation ELSE '' END,
w.givenAnswer, w.correctAnswer, w.firstWrongAt, w.lastWrongAt, w.nextReviewAt, w.stage, w.wrongCount, w.resolved, 'LEGACY', w.lastWrongAt
FROM WrongQuestion w LEFT JOIN Question q ON q.id = w.questionId""",
        """UPDATE TopicProgress SET status = 1 WHERE status = 2""",
        """DELETE FROM Bookmark WHERE kind IN ('QUESTION', 'LATER')""",
        """DROP TABLE IF EXISTS AnswerRecord""",
        """DROP TABLE IF EXISTS QuestionAttempt""",
        """DROP TABLE IF EXISTS WrongQuestion""",
        """DROP TABLE IF EXISTS AnswerOption""",
        """DROP TABLE IF EXISTS Question""",
)
