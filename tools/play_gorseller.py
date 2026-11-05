#!/usr/bin/env python3
"""Gerçek uygulama ekranlarından Google Play görsellerini üretir. Kullanım: play_gorseller.py ekran_klasoru"""
import base64, json, subprocess, sys, tempfile
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
SRC = Path(sys.argv[1])
OUT = ROOT / "play/gorseller"
SHOTS = [
    ("1-ana-sayfa", "s-dark-00-home", "Sınava hazırlığını", "tek yerden yönet"),
    ("2-bugunku-rotan", "s-dark-10-coach", "Bugün ne çalışacağını", "gerekçesiyle gör"),
    ("3-akilli-plan", "s-dark-33-plan", "Program bozulsa da", "yeniden düzenle"),
    ("4-kaynak-yonetimi", "s-dark-12-sources", "Kaldığın sayfayı", "her kaynakta takip et"),
    ("5-konu-ustaligi", "s-dark-09-mastery", "Konu haritan", "gerçek verilerle dolsun"),
    ("6-deneme-analizi", "s-dark-49-examAnalysis", "Net gelişimini", "ders ders analiz et"),
    ("7-yanlis-defteri", "s-dark-18-wrongs", "Yanlışlarını", "aralıklı tekrarla kapat"),
    ("8-haftalik-rapor", "s-dark-54-reports", "Haftanı özetle", "rapor olarak paylaş"),
]
def data(p): return "data:image/png;base64," + base64.b64encode(Path(p).read_bytes()).decode()
def find(key): return sorted(SRC.glob(f"411dp-{key}*.png"))[0]
PHONE = "border-radius:{r}px;border:{b}px solid #2b3750;box-shadow:0 30px 80px rgba(49,139,255,.28);overflow:hidden;background:#0b0f18"
def shot_html(img, a, b):
    return f"""<html><body style="margin:0"><div style="width:1080px;height:1920px;position:relative;overflow:hidden;background:radial-gradient(circle at 85% 8%,#1e3a6b 0,#111827 40%,#0b0f18 75%);font-family:Roboto,Arial,sans-serif;color:#f7f8fc">
<div style="padding:90px 80px 0"><div style="font-size:74px;font-weight:800;line-height:1.1">{a}</div><div style="font-size:74px;font-weight:800;line-height:1.1;background:linear-gradient(90deg,#58b5ff,#a47aff);-webkit-background-clip:text;color:transparent">{b}</div></div>
<div style="position:absolute;left:130px;top:360px;width:820px;height:1760px;{PHONE.format(r=56,b=10)}"><img src="{data(img)}" style="width:100%;display:block"></div></div></body></html>"""
def feature(img, title, sub):
    return f"""<html><body style="margin:0"><div style="width:1024px;height:500px;position:relative;overflow:hidden;background:radial-gradient(circle at 80% 20%,#1e3a6b 0,#111827 45%,#0b0f18 80%);font-family:Roboto,Arial,sans-serif;color:#f7f8fc">
<div style="position:absolute;left:64px;top:120px;width:560px"><div style="font-size:30px;color:#58b5ff;font-weight:700;margin-bottom:14px">YKS Pusula</div><div style="font-size:54px;font-weight:800;line-height:1.12">{title}</div><div style="font-size:26px;color:#a7b1c5;margin-top:18px">{sub}</div></div>
<div style="position:absolute;left:690px;top:40px;width:270px;height:600px;transform:rotate(4deg);{PHONE.format(r=30,b=6)}"><img src="{data(img)}" style="width:100%;display:block"></div></div></body></html>"""
jobs = []
tmp = Path(tempfile.mkdtemp())
for name, key, a, b in SHOTS:
    f = tmp / f"{name}.html"; f.write_text(shot_html(find(key), a, b), encoding="utf8"); jobs.append([str(f), str(OUT / f"telefon-{name}.png"), 1080, 1920])
f = tmp / "t.html"; f.write_text(feature(find("s-dark-02-exams"), "Sınava hazırlığını tek yerden yönet.", "Planla. Çalış. Takip et. Hedefine yaklaş."), encoding="utf8"); jobs.append([str(f), str(OUT / "tanitim-1024x500.png"), 1024, 500])
f = tmp / "t2.html"; f.write_text(feature(find("s-dark-00-home"), "Ne çalışacağını sana söyleyen plan.", "Kaynağın ne olursa olsun, rotan hazır."), encoding="utf8"); jobs.append([str(f), str(OUT / "tanitim-alternatif.png"), 1024, 500])
js = Path(tmp / "s.js"); js.write_text("""const { chromium } = require('playwright');
(async()=>{const b=await chromium.launch();for(const [f,o,w,h] of JSON.parse(process.argv[2])){const p=await b.newPage({viewport:{width:w,height:h}});await p.goto('file://'+f);await p.waitForTimeout(300);await p.screenshot({path:o,clip:{x:0,y:0,width:w,height:h}});await p.close();}await b.close();})();""")
subprocess.run(["node", str(js), json.dumps(jobs)], check=True, cwd=tmp)
print(len(jobs), "görsel")
