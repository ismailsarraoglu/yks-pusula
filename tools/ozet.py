#!/usr/bin/env python3
"""CI özeti ve son kalite kapısı.

Kullanım: ozet.py <sonuç-klasörü> '<planlanan cihazlar JSON>' <çıktı.md>

Her iş (derle + planlanan her cihaz) için durum-<iş>.tsv ve cikis-<iş>.txt okunur.
- Hiç kaydı olmayan planlı iş: NOT RUN (derleme durduysa veya iş başlamadıysa).
- Bütün aşamaları PASS ve çıkış kodu 0 olan iş: PASS. Başka her durum: FAIL.
Bütün işler PASS değilse çıkış kodu 1 olur; böylece GitHub Actions sonucu da başarısız görünür.
"""
import json
import sys
from pathlib import Path

root = Path(sys.argv[1])
planned = [c["ad"] for c in json.loads(sys.argv[2] or "[]")]
out = Path(sys.argv[3])
jobs = ["derle"] + planned


def find(name):
    hits = sorted(root.rglob(name))
    return hits[0] if hits else None


def stages(job):
    f = find(f"durum-{job}.tsv")
    if not f:
        return None
    rows = []
    for line in f.read_text(encoding="utf-8", errors="replace").splitlines():
        parts = line.split("\t")
        if len(parts) >= 2 and parts[0]:
            rows.append((parts[0], parts[1], parts[2] if len(parts) > 2 else ""))
    return rows


def exit_code(job):
    f = find(f"cikis-{job}.txt")
    if not f:
        return None
    t = f.read_text().strip()
    return int(t) if t.lstrip("-").isdigit() else None


label = {"PASS": "PASS", "FAIL": "FAIL", "NOT_RUN": "NOT RUN"}
summary = []
detail = []
for job in jobs:
    rows = stages(job)
    code = exit_code(job)
    notes = find(f"notlar-{job}.txt")
    if rows is None and code is None:
        status, why = "NOT_RUN", "iş çalışmadı (derleme kapısı geçilmedi, iptal edildi veya planlanmadı)"
        rows = []
    else:
        rows = rows or []
        bad = [r for r in rows if r[1] != "PASS"]
        if not rows:
            status, why = "FAIL", f"aşama kaydı yok (çıkış kodu {code})"
        elif code is None:
            status, why = "FAIL", "çıkış kodu kaydedilmedi (iş yarıda kesildi)"
        elif bad:
            status, why = "FAIL", f"{len(bad)} aşama PASS değil: " + ", ".join(f"{r[0]}={label.get(r[1], r[1])}" for r in bad[:8])
        elif code != 0:
            status, why = "FAIL", f"aşamalar PASS ama çıkış kodu {code}"
        else:
            status, why = "PASS", f"{len(rows)} aşama geçti"
    summary.append((job, status, why))
    detail.append(f"\n### {job}: {label[status]}\n")
    if notes:
        detail.append("> " + notes.read_text(encoding="utf-8").strip().replace("\n", "\n> ") + "\n")
    if rows:
        detail.append("| Aşama | Sonuç | Açıklama |\n|---|---|---|")
        for a, s, d in rows:
            detail.append(f"| {a} | {label.get(s, s)} | {d.replace('|', '/')} |")

overall = all(s == "PASS" for _, s, _ in summary)
lines = [f"# CI kalite kapısı: {'PASS' if overall else 'FAIL'}\n",
         "| İş | Sonuç | Neden |", "|---|---|---|"]
lines += [f"| {j} | {label[s]} | {w} |" for j, s, w in summary]
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text("\n".join(lines + detail) + "\n", encoding="utf-8")
print("\n".join(lines))
sys.exit(0 if overall else 1)
