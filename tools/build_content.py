#!/usr/bin/env python3
"""İçerik kaynaklarını doğrular ve app/src/main/assets/content/base.json dosyasını üretir."""
import hashlib
import json
import random
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "content" / "src"))
import catalog_tyt, catalog_ayt, gen_computed, q_verbal1, q_verbal2, q_verbal3, vocab  # noqa: E402

VERSION = "1.0.0"
CURR = "2027-YKS-cerceve"
SOURCE = "YKS Pusula özgün içerik"
LICENSE = "Özgün içerik; uygulama ile birlikte dağıtılır"
LETTERS = "ABCDE"


def slug(s):
    s = s.replace("İ", "i").replace("I", "ı").lower()
    for a, b in zip("çğıöşüâîû'’", "cgiosuaiu  "):
        s = s.replace(a, b)
    return re.sub(r"[^a-z0-9]+", "-", s).strip("-")


subjects, topics, topic_index, order = [], [], {}, 0


def add_subject(sid, exam, name, group, fields, items, language=None):
    global order
    order += 1
    subjects.append(dict(id=sid, exam=exam, name=name, groupName=group, fields=fields, sortOrder=order, language=language))
    for i, item in enumerate(items):
        name_t, summary, lesson, keys, example = item[:5]
        meta = item[5] if len(item) > 5 else {}
        tid = f"{sid}-{slug(name_t)}"
        assert tid not in topic_index, tid
        topic_index[(sid, name_t)] = tid
        parent = f"{sid}-{slug(meta['parent'])}" if meta.get("parent") else None
        scope = {"REVIEW": "REVIEW", "EXCLUDED": "EXCLUDED"}.get(meta.get("scope"), "IN")
        topics.append(dict(id=tid, subjectId=sid, parentId=parent, name=name_t, summary=summary, lesson=lesson,
                           keyPoints=keys.replace("|", "\n"), example=example, curriculum=CURR, validFromYear=2027, validToYear=2027,
                           scope=scope, scopeNote=meta.get("note", ""), estimatedMinutes=150, sortOrder=i))


for sid, (name, group, fields, items) in catalog_tyt.TYT.items():
    add_subject(sid, "TYT", name, group, fields, items)
for sid, (name, group, fields, items) in catalog_ayt.AYT.items():
    add_subject(sid, "AYT", name, group, fields, items)
add_subject("ydt-ing", "YDT", "İngilizce", "Genel", "DIL", catalog_ayt.ENG_TOPICS, "İngilizce")
for code, lang in [("alm", "Almanca"), ("fra", "Fransızca"), ("rus", "Rusça"), ("ara", "Arapça")]:
    add_subject(f"ydt-{code}", "YDT", lang, "Genel", "DIL", catalog_ayt.GENERIC_LANG_TOPICS, lang)

raw = list(gen_computed.OUT)
for bank in (q_verbal1.Q, q_verbal2.Q, q_verbal3.Q):
    for sid, rows in bank.items():
        for topic, diff, stem, correct, wrong, expl in rows:
            raw.append(dict(subject=sid, topic=topic, diff=diff, stem=stem, correct=correct, wrong=wrong, expl=expl, steps=[], figure=None, generated=False))

exam_of = {s["id"]: s["exam"] for s in subjects}
errors, questions, seen = [], [], set()
for q in raw:
    key = (q["subject"], q["topic"])
    if key not in topic_index:
        errors.append(f"Konu yok: {key}")
        continue
    norm = re.sub(r"\s+", " ", q["stem"].lower()).strip()
    if norm in seen:
        continue  # aynı soru iki kez üretilmişse biri alınır
    seen.add(norm)
    if q["correct"] in q["wrong"] or len(set(q["wrong"])) != 4:
        errors.append(f"Çeldirici hatası: {q['stem']}")
        continue
    h = hashlib.sha1(q["stem"].encode()).hexdigest()
    rnd = random.Random(h)
    opts = [q["correct"]] + list(q["wrong"])
    rnd.shuffle(opts)
    label = LETTERS[opts.index(q["correct"])]
    tid = topic_index[key]
    questions.append(dict(id=f"q-{h[:12]}", exam=exam_of[q["subject"]], subjectId=q["subject"], topicId=tid, difficulty=q["diff"], stem=q["stem"],
                          figure=q["figure"], options=[dict(label=LETTERS[i], text=o) for i, o in enumerate(opts)], correct=label,
                          explanation=q["expl"], steps=q["steps"], examYear=2027, curriculum=CURR, source=SOURCE, license=LICENSE, generated=q["generated"]))

# Formül ve bilgi kütüphanesi: konu anahtar bilgilerinden üretilir
CATEGORY = {"tyt-mat": "Matematik", "ayt-mat": "Matematik", "tyt-geo": "Geometri", "tyt-fiz": "Fizik", "ayt-fiz": "Fizik", "tyt-kim": "Kimya", "ayt-kim": "Kimya",
            "tyt-biy": "Biyoloji kavramları", "ayt-biy": "Biyoloji kavramları", "tyt-tar": "Tarih kronolojisi", "ayt-tar1": "Tarih kronolojisi", "ayt-tar2": "Tarih kronolojisi",
            "tyt-cog": "Coğrafya temel bilgiler", "ayt-cog1": "Coğrafya temel bilgiler", "ayt-cog2": "Coğrafya temel bilgiler", "ayt-edb": "Edebiyat yazar-eser",
            "tyt-tur": "Türkçe", "tyt-fel": "Felsefe", "ayt-fel": "Felsefe grubu", "tyt-din": "Din Kültürü", "ayt-din": "Din Kültürü", "ydt-ing": "İngilizce"}
sub_by_id = {s["id"]: s for s in subjects}
formulas, flashcards = [], []
for t in topics:
    if not t["keyPoints"] or t["subjectId"] not in CATEGORY:
        continue
    s = sub_by_id[t["subjectId"]]
    formulas.append(dict(id=f"f-{t['id']}", subjectId=s["id"], subjectName=CATEGORY[s["id"]], category=f"{s['exam']} {s['name']}", title=t["name"], body=t["keyPoints"], note=""))
DECKS = {"tyt-tar": "Tarih", "ayt-tar1": "Tarih", "ayt-tar2": "Tarih", "tyt-cog": "Coğrafya", "ayt-cog1": "Coğrafya", "ayt-cog2": "Coğrafya", "tyt-biy": "Biyoloji", "ayt-biy": "Biyoloji",
         "ayt-edb": "Edebiyat", "tyt-fel": "Felsefe", "ayt-fel": "Felsefe", "tyt-mat": "Formüller", "ayt-mat": "Formüller", "tyt-geo": "Formüller", "tyt-fiz": "Formüller", "ayt-fiz": "Formüller", "tyt-kim": "Tanımlar", "ayt-kim": "Tanımlar"}
for t in topics:
    deck = DECKS.get(t["subjectId"])
    if not deck:
        continue
    for line in t["keyPoints"].split("\n"):
        if ": " in line:
            f, b = line.split(": ", 1)
            flashcards.append(dict(id=f"c-{hashlib.sha1((t['id']+line).encode()).hexdigest()[:12]}", deck=deck, subjectName=sub_by_id[t["subjectId"]]["name"], front=f, back=b))
for line in vocab.WORDS.strip().split("\n"):
    w, tr, pos, ex = line.split("|")
    flashcards.append(dict(id=f"v-{slug(w)}", deck="İngilizce Kelimeler", subjectName="İngilizce", front=w, back=tr, partOfSpeech=pos, example=ex))
ids = Counter(c["id"] for c in flashcards)
flashcards = list({c["id"]: c for c in flashcards}.values())

P = lambda *a: [dict(subjectId=s, count=c) for s, c in a]
NOTE = "2026 YKS yapısı başlangıç referansıdır. Ders içi dağılım uygulamanın deneme oluşturmak için kullandığı değerdir; sınav yılı kılavuzuyla karşılaştırın."
configs = [
    dict(code="TYT", title="TYT", examDate="2027-06-19", examTime="10:15", questionCount=120, durationMin=165, wrongDivisor=4.0,
         sectionsJson=json.dumps([dict(name="Türkçe", parts=P(("tyt-tur", 40))), dict(name="Sosyal Bilimler", parts=P(("tyt-tar", 5), ("tyt-cog", 5), ("tyt-fel", 5), ("tyt-din", 5))),
                                  dict(name="Temel Matematik", parts=P(("tyt-mat", 32), ("tyt-geo", 8))), dict(name="Fen Bilimleri", parts=P(("tyt-fiz", 7), ("tyt-kim", 7), ("tyt-biy", 6)))], ensure_ascii=False),
         applicationStart="2027-02-04", applicationEnd="2027-03-02", resultDate="2027-07-21", sourceNote=NOTE),
    dict(code="AYT", title="AYT", examDate="2027-06-20", examTime="10:15", questionCount=160, durationMin=180, wrongDivisor=4.0,
         sectionsJson=json.dumps([dict(name="Türk Dili ve Edebiyatı-Sosyal Bilimler-1", parts=P(("ayt-edb", 24), ("ayt-tar1", 10), ("ayt-cog1", 6))),
                                  dict(name="Sosyal Bilimler-2", parts=P(("ayt-tar2", 11), ("ayt-cog2", 11), ("ayt-fel", 12), ("ayt-din", 6))),
                                  dict(name="Matematik", parts=P(("ayt-mat", 40))), dict(name="Fen Bilimleri", parts=P(("ayt-fiz", 14), ("ayt-kim", 13), ("ayt-biy", 13)))], ensure_ascii=False),
         applicationStart="2027-02-04", applicationEnd="2027-03-02", resultDate="2027-07-21", sourceNote=NOTE),
    dict(code="YDT", title="YDT", examDate="2027-06-20", examTime="15:45", questionCount=80, durationMin=120, wrongDivisor=4.0,
         sectionsJson=json.dumps([dict(name="Yabancı Dil", parts=P(("ydt-ing", 80)))], ensure_ascii=False),
         applicationStart="2027-02-04", applicationEnd="2027-03-02", resultDate="2027-07-21", sourceNote=NOTE),
]

pack = dict(pack=dict(id="base", title="YKS Pusula başlangıç içeriği", version=VERSION, kind="CONTENT", source=SOURCE, license=LICENSE),
            examConfigs=configs, subjects=subjects, topics=topics, questions=questions, formulas=formulas, flashcards=flashcards)

# Son denetimler
hashes = Counter()
for q in questions:
    assert sum(o["label"] == q["correct"] for o in q["options"]) == 1
    texts = [o["text"] for o in q["options"]]
    assert len(set(t.lower() for t in texts)) == 5, q["stem"]
    assert q["explanation"]
    hashes[q["id"]] += 1
assert max(hashes.values()) == 1
if errors:
    print("\n".join(errors))
    sys.exit(1)

out = ROOT / "app/src/main/assets/content/base.json"
out.write_text(json.dumps(pack, ensure_ascii=False, separators=(",", ":")), encoding="utf8")
report = dict(version=VERSION, questions=len(questions), byExam=Counter(q["exam"] for q in questions), bySubject=Counter(q["subjectId"] for q in questions),
              generated=sum(q["generated"] for q in questions), handWritten=sum(not q["generated"] for q in questions), byDifficulty=Counter(q["difficulty"] for q in questions),
              topics=len(topics), topicsWithLesson=sum(1 for t in topics if t["lesson"]), topicsWithExample=sum(1 for t in topics if t["example"]),
              topicsWithoutQuestions=[t["id"] for t in topics if t["scope"] != "EXCLUDED" and t["id"] not in {q["topicId"] for q in questions}],
              scopeReview=[t["name"] for t in topics if t["scope"] != "IN"], formulas=len(formulas), flashcards=len(flashcards),
              vocabulary=sum(1 for c in flashcards if c["deck"] == "İngilizce Kelimeler"), sizeBytes=out.stat().st_size)
(ROOT / "content/report.json").write_text(json.dumps(report, ensure_ascii=False, indent=1), encoding="utf8")
print({k: v for k, v in report.items() if k != "topicsWithoutQuestions"})
print("Sorusuz konu:", len(report["topicsWithoutQuestions"]))
