package app.ykspusula

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ykspusula.data.AppDatabase
import app.ykspusula.data.MIGRATION_1_2
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Soru bankalı sürümden gerçek bir veritabanı kurulur ve yeni sürüme taşınır. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val name = "goc-testi"
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test fun oldUserDataSurvives() {
        helper.createDatabase(name, 1).apply {
            execSQL("INSERT INTO UserProfile (id,name,grade,examYear,field,ydtLanguage,targetUniversity,targetDepartment,dailyMinutes,studyDays,dailyQuestions,strongSubjects,weakSubjects,startHour,onboarded) VALUES (1,'Eski','12',2027,'SAY','İngilizce','ODTÜ','Tıp',240,'1,2',60,'','',9,1)")
            execSQL("INSERT INTO Subject (id,exam,name,groupName,fields,sortOrder,language,packId) VALUES ('s','TYT','Matematik','Matematik','ALL',1,NULL,'base')")
            execSQL("INSERT INTO Topic (id,subjectId,parentId,name,summary,lesson,keyPoints,example,curriculum,validFromYear,validToYear,scope,scopeNote,estimatedMinutes,sortOrder,packId) VALUES ('t','s',NULL,'Kümeler','','','','','c',2027,2027,'IN','',100,0,'base')")
            execSQL("INSERT INTO Question (id,exam,subjectId,topicId,difficulty,stem,figure,explanation,steps,examYear,curriculum,source,license,generated,contentHash,packId) VALUES ('q1','TYT','s','t',1,'2 kaç?',NULL,'İki','',2027,'c','x','y',0,'h1','base')")
            execSQL("INSERT INTO Question (id,exam,subjectId,topicId,difficulty,stem,figure,explanation,steps,examYear,curriculum,source,license,generated,contentHash,packId) VALUES ('q2','TYT','s','t',1,'3 kaç?',NULL,'Üç','',2027,'c','x','y',0,'h2','base')")
            listOf("q1", "q2").forEach { q ->
                execSQL("INSERT INTO AnswerOption (questionId,label,text,isCorrect) VALUES ('$q','A','doğru',1)")
                execSQL("INSERT INTO AnswerOption (questionId,label,text,isCorrect) VALUES ('$q','B','yanlış',0)")
            }
            val day = 1_800_000_000_000L
            execSQL("INSERT INTO QuestionAttempt (id,questionId,topicId,subjectId,selected,isCorrect,answeredAt,source,mockExamId) VALUES ('a1','q1','t','s','A',1,$day,'BANK',NULL)")
            execSQL("INSERT INTO QuestionAttempt (id,questionId,topicId,subjectId,selected,isCorrect,answeredAt,source,mockExamId) VALUES ('a2','q2','t','s','B',0,${day + 1000},'BANK',NULL)")
            execSQL("INSERT INTO QuestionAttempt (id,questionId,topicId,subjectId,selected,isCorrect,answeredAt,source,mockExamId) VALUES ('a3','q2','t','s',NULL,0,${day + 2000},'BANK',NULL)")
            execSQL("INSERT INTO WrongQuestion (questionId,topicId,subjectId,givenAnswer,correctAnswer,firstWrongAt,lastWrongAt,nextReviewAt,stage,wrongCount,reason,note,resolved) VALUES ('q2','t','s','yanlış','doğru',1,2,3,1,2,'Dikkatsizlik','notum',0)")
            execSQL("INSERT INTO TopicProgress (topicId,status,lastStudiedAt,nextReviewAt,completedAt) VALUES ('t',2,5,NULL,NULL)")
            execSQL("INSERT INTO Bookmark (kind,refId,createdAt) VALUES ('QUESTION','q1',1)")
            execSQL("INSERT INTO Bookmark (kind,refId,createdAt) VALUES ('LATER','q1',1)")
            execSQL("INSERT INTO Bookmark (kind,refId,createdAt) VALUES ('FORMULA','f',1)")
            execSQL("INSERT INTO UserNote (id,title,body,subjectId,topicId,createdAt,updatedAt,favorite) VALUES ('n','Not','x',NULL,NULL,1,1,0)")
            execSQL("INSERT INTO MockExam (id,title,kind,exam,createdAt,endsAt,durationSec,status,isExternal,publisher,examDate,spentMinutes,note) VALUES ('m1','Bitmiş','EXTERNAL','TYT',10,10,600,'DONE',1,'','2026-11-01',10,'')")
            execSQL("INSERT INTO MockExamResult (mockExamId,section,correct,wrong,blank,net,sortOrder) VALUES ('m1','Türkçe',30,8,2,28.0,0)")
            execSQL("INSERT INTO MockExam (id,title,kind,exam,createdAt,endsAt,durationSec,status,isExternal,publisher,examDate,spentMinutes,note) VALUES ('m2','Yarım','GENERAL','TYT',$day,${day + 600000},600,'RUNNING',0,'','',0,'')")
            execSQL("INSERT INTO AnswerRecord (mockExamId,questionId,orderIndex,sectionName,selected,flagged,answeredAt) VALUES ('m2','q1',0,'Matematik','A',0,NULL)")
            execSQL("INSERT INTO AnswerRecord (mockExamId,questionId,orderIndex,sectionName,selected,flagged,answeredAt) VALUES ('m2','q2',1,'Matematik','B',0,NULL)")
            execSQL("INSERT INTO StudySession (id,subjectId,topicId,label,startedAt,minutes,kind,note) VALUES ('ss','s','t','Odak',1,30,'POMODORO','')")
            execSQL("INSERT INTO StudyTask (id,planId,date,time,subjectId,topicId,title,kind,minutes,questionGoal,done,doneAt) VALUES ('tk',NULL,'2026-11-01','09:00','s','t','Görev','STUDY',50,10,0,NULL)")
            execSQL("INSERT INTO Reminder (type,enabled,hour,minute,dayOfWeek) VALUES ('DAILY',1,20,0,7)")
            execSQL("INSERT INTO LibraryFile (id,name,fileName,sizeBytes,addedAt,subjectName) VALUES ('l','a.pdf','a.pdf',1,1,'')")
            close()
        }
        val db = helper.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2)
        fun int(sql: String): Int = db.query(sql).use { it.moveToFirst(); it.getInt(0) }
        fun str(sql: String): String? = db.query(sql).use { it.moveToFirst(); it.getString(0) }

        assertEquals("Eski", str("SELECT name FROM UserProfile"))
        assertEquals(360, int("SELECT maxDailyMinutes FROM UserProfile"))
        assertEquals(1, int("SELECT correct FROM PracticeLog WHERE origin='LEGACY'"))
        assertEquals(1, int("SELECT wrong FROM PracticeLog WHERE origin='LEGACY'"))
        assertEquals(1, int("SELECT blank FROM PracticeLog WHERE origin='LEGACY'"))
        assertTrue(str("SELECT questionText FROM WrongEntry")!!.startsWith("3 kaç?"))
        assertTrue(str("SELECT questionText FROM WrongEntry")!!.contains("Üç"))
        assertEquals("notum", str("SELECT note FROM WrongEntry"))
        assertEquals(2, int("SELECT wrongCount FROM WrongEntry"))
        assertEquals(1, int("SELECT status FROM TopicProgress"))
        assertEquals(1, int("SELECT COUNT(*) FROM Bookmark"))
        assertEquals("FORMULA", str("SELECT kind FROM Bookmark"))
        assertEquals("Not", str("SELECT title FROM UserNote"))
        assertEquals(28, int("SELECT net FROM MockExamResult WHERE mockExamId='m1'"))
        assertEquals("DONE", str("SELECT status FROM MockExam WHERE id='m2'"))
        assertEquals(1, int("SELECT correct FROM MockExamResult WHERE mockExamId='m2'"))
        assertEquals(1, int("SELECT wrong FROM MockExamResult WHERE mockExamId='m2'"))
        assertEquals(30, int("SELECT minutes FROM StudySession"))
        assertEquals(2, int("SELECT priority FROM StudyTask"))
        assertEquals(1, int("SELECT COUNT(*) FROM LibraryFile"))
        assertEquals(0, int("SELECT COUNT(*) FROM sqlite_master WHERE name IN ('Question','AnswerOption','QuestionAttempt','AnswerRecord','WrongQuestion')"))
        assertEquals(0, db.query("PRAGMA foreign_key_check").use { it.count })
        db.close()
    }
}
