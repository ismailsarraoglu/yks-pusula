"""Hesaplanarak üretilen özgün sorular.
Doğru cevap her zaman bu kodla hesaplanır, elle yazılmaz. Sayılar değiştirilerek aynı kalıptan
birden çok soru türetildiği için bu sorular veride generated=true ile işaretlenir."""
import itertools
import json
import math
import random
from fractions import Fraction as Fr
from statistics import median

OUT = []


def num(v):
    if isinstance(v, Fr):
        if v.denominator == 1:
            return str(v.numerator).replace("-", "−")
        sign = "−" if v < 0 else ""
        return f"{sign}\\frac{{{abs(v.numerator)}}}{{{v.denominator}}}"
    if isinstance(v, float):
        if abs(v - round(v)) < 1e-9:
            return str(int(round(v))).replace("-", "−")
        return f"{v:.2f}".rstrip("0").rstrip(".").replace(".", ",").replace("-", "−")
    return str(v).replace("-", "−")


def add(subject, topic, diff, stem, ans, wrong, expl, steps=(), unit="", figure=None, key=None):
    """ans ve wrong sayısal (veya metin) olabilir; 4 farklı çeldirici zorunludur."""
    fmt = (lambda x: num(x) + unit) if not isinstance(ans, str) else (lambda x: x)
    a = fmt(ans)
    seen = {a}
    opts = []
    for w in wrong:
        t = fmt(w)
        if t not in seen:
            seen.add(t)
            opts.append(t)
    if not isinstance(ans, str):
        step = 1
        while len(opts) < 4:
            for s in (step, -step):
                cand = ans + s
                t = fmt(cand)
                if t not in seen and len(opts) < 4 and (cand >= 0 or ans < 0):
                    seen.add(t)
                    opts.append(t)
            step += 1
    assert len(opts) >= 4, (stem, opts)
    OUT.append(dict(subject=subject, topic=topic, diff=diff, stem=stem, correct=a, wrong=opts[:4],
                    expl=expl, steps=list(steps), figure=figure, generated=True))


TM, GE, FZ, KM = "tyt-mat", "tyt-geo", "tyt-fiz", "tyt-kim"
AM, AF, AK = "ayt-mat", "ayt-fiz", "ayt-kim"


def triangle_fig(alt, a="A", b="B", c="C", labels=()):
    return json.dumps({"w": 100, "h": 62, "alt": alt,
        "lines": [{"x1": 12, "y1": 52, "x2": 88, "y2": 52}, {"x1": 88, "y1": 52, "x2": 40, "y2": 10}, {"x1": 40, "y1": 10, "x2": 12, "y2": 52}],
        "labels": [{"x": 7, "y": 56, "t": a}, {"x": 93, "y": 56, "t": b}, {"x": 40, "y": 5, "t": c}] + list(labels)}, ensure_ascii=False)


def right_fig(alt, b, c, h):
    return json.dumps({"w": 100, "h": 62, "alt": alt,
        "lines": [{"x1": 16, "y1": 52, "x2": 84, "y2": 52}, {"x1": 16, "y1": 52, "x2": 16, "y2": 10}, {"x1": 16, "y1": 10, "x2": 84, "y2": 52},
                  {"x1": 16, "y1": 46, "x2": 22, "y2": 46}, {"x1": 22, "y1": 46, "x2": 22, "y2": 52}],
        "labels": [{"x": 50, "y": 58, "t": b}, {"x": 9, "y": 31, "t": c}, {"x": 56, "y": 26, "t": h}]}, ensure_ascii=False)


# ---------- TYT MATEMATİK ----------
for a, b, c in [(3, 4, 5), (7, 2, 6), (12, 3, 2), (9, 5, 3)]:
    r = a + b * c
    add(TM, "Temel Kavramlar", 1, f"{a} + {b} · {c} işleminin sonucu kaçtır?", r, [(a + b) * c, a * b + c, r + 2, r - 3],
        "Çarpma işlemi toplamadan önce yapılır.", [f"{b} · {c} = {b*c}", f"{a} + {b*c} = {r}"])
for s in (11, 9, 13, 7):
    add(TM, "Sayı Basamakları", 1, f"ab iki basamaklı bir sayıdır. ab + ba = {11*s} olduğuna göre a + b kaçtır?", s, [s - 2, s + 2, 11, 2 * s],
        "ab + ba = 11(a + b) olur.", [f"11(a + b) = {11*s}", f"a + b = {s}"])
for d in (3, 4, 5, 6):
    add(TM, "Sayı Basamakları", 2, f"ab iki basamaklı bir sayıdır. ab − ba = {9*d} olduğuna göre a − b kaçtır?", d, [d + 1, d - 1, 9, d + 3],
        "ab − ba = 9(a − b) olur.", [f"9(a − b) = {9*d}", f"a − b = {d}"])
for n, m in [(127, 6), (385, 7), (214, 9), (1003, 8)]:
    add(TM, "Bölme ve Bölünebilme", 1, f"{n} sayısının {m} ile bölümünden kalan kaçtır?", n % m, [(n % m + 1) % m, (n % m + 2) % m, (n % m + 3) % m, (n % m + 4) % m],
        f"{n} = {m} · {n//m} + {n%m} yazılır; kalan bölenden küçüktür.", [f"{m} · {n//m} = {m*(n//m)}", f"{n} − {m*(n//m)} = {n%m}"])
for base in (413, 5721, 262):
    digits = [int(x) for x in str(base)]
    s = sum(digits)
    x = (9 - s % 9) % 9
    if x == 0:
        x = 9
    add(TM, "Bölme ve Bölünebilme", 2, f"Son basamağı x olan {base}x sayısı 9 ile tam bölünebildiğine göre x rakamı kaçtır? (x ≠ 0)", x,
        [(x + 1) % 9 + 1, (x + 3) % 9 + 1, (x + 5) % 9 + 1, (x + 6) % 9 + 1],
        "9 ile bölünebilme için rakamlar toplamı 9'un katı olmalıdır.", [f"Rakamlar toplamı {s} + x", f"{s} + {x} = {s+x}, 9'un katıdır."])
for a, b in [(24, 36), (18, 30), (42, 56), (60, 84)]:
    g = math.gcd(a, b)
    add(TM, "EBOB-EKOK", 1, f"{a} ve {b} sayılarının en büyük ortak böleni kaçtır?", g, [g * 2, g // 2, g + 1, a * b // g],
        "Ortak bölenlerin en büyüğü alınır.", [f"EBOB({a}, {b}) = {g}"])
    l = a * b // g
    add(TM, "EBOB-EKOK", 1, f"{a} ve {b} sayılarının en küçük ortak katı kaçtır?", l, [g, l * 2, a * b, l // 2],
        "EKOK = çarpım / EBOB.", [f"EBOB = {g}", f"EKOK = {a} · {b} / {g} = {l}"])
for a, b, days in [(4, 6, 12), (6, 8, 24), (10, 15, 30)]:
    add(TM, "EBOB-EKOK", 2, f"Bir kişi her {a} günde bir, diğeri her {b} günde bir aynı yeri ziyaret ediyor. Bugün birlikte ziyaret ettiklerine göre en az kaç gün sonra yine birlikte ziyaret ederler?", days,
        [a + b, a * b + 1, days * 2, math.gcd(a, b)], "Birlikte tekrar eden olaylarda EKOK aranır.", [f"EKOK({a}, {b}) = {days}"], " gün")
for p, q, r, s in [(1, 2, 1, 3), (2, 3, 1, 4), (3, 4, 2, 5), (1, 6, 3, 8)]:
    v = Fr(p, q) + Fr(r, s)
    add(TM, "Rasyonel Sayılar", 1, f"\\frac{{{p}}}{{{q}}} + \\frac{{{r}}}{{{s}}} işleminin sonucu kaçtır?", v, [Fr(p + r, q + s), Fr(p * r, q * s), v + Fr(1, 2), v - Fr(1, q * s)],
        "Paydalar eşitlenir, sonra paylar toplanır.", [f"Ortak payda: {v.denominator if v.denominator % q == 0 else q*s}", f"Sonuç {num(v)}"])
for a, b in [(3, 2), (12, 5), (25, 4)]:
    v = Fr(a, 10) * Fr(b, 10)
    add(TM, "Ondalık Sayılar", 1, f"{num(a/10)} · {num(b/10)} işleminin sonucu kaçtır?", float(v), [float(v) * 10, float(v) / 10, float(Fr(a, 10) + Fr(b, 10)), float(v) + 0.5],
        "Sayılar virgülsüz çarpılır, toplam ondalık basamak korunur.", [f"{a} · {b} = {a*b}", f"İki ondalık basamak: {num(float(v))}"])
for a, b in [(2, 8), (3, 12), (5, 20), (4, 28)]:
    add(TM, "Basit Eşitsizlikler", 2, f"−{a}x < {b} eşitsizliğini sağlayan en küçük tam sayı kaçtır?", -(b // a) + 1, [-(b // a), -(b // a) - 1, b // a, -(b // a) + 2],
        "Negatif sayıya bölünce eşitsizlik yön değiştirir.", [f"x > −{b//a}", f"En küçük tam sayı {-(b//a)+1}"])
for k, v in [(2, 3), (5, 4), (1, 6), (3, 7)]:
    add(TM, "Mutlak Değer", 2, f"|x − {k}| = {v} denkleminin kökleri toplamı kaçtır?", 2 * k, [v, k + v, 2 * v, 0],
        "|x − k| = v ise x = k + v veya x = k − v.", [f"x = {k+v} veya x = {k-v}", f"Toplam {2*k}"])
for a, b, c in [(3, 4, 2), (5, 2, 3), (6, 3, 4)]:
    add(TM, "Üslü Sayılar", 1, f"\\frac{{2^{{{a}}} · 2^{{{b}}}}}{{2^{{{c}}}}} işleminin sonucu kaçtır?", 2 ** (a + b - c), [2 ** (a + b), 2 ** (a * b - c) if a * b - c < 12 else 3000, 2 ** (a + b - c + 1), 2 ** (a + b - c - 1)],
        "Tabanlar aynıyken çarpmada üsler toplanır, bölmede çıkarılır.", [f"{a} + {b} − {c} = {a+b-c}", f"2^{{{a+b-c}}} = {2**(a+b-c)}"])
for n, o in [(3, 4), (5, 3)]:
    add(TM, "Üslü Sayılar", 1, f"(−{n})^{{{2}}} + {n}^{{0}} işleminin sonucu kaçtır?", n * n + 1, [n * n, -n * n + 1, n * n - 1, 2 * n + 1],
        "Çift üste negatif taban pozitif olur; sıfırıncı kuvvet 1 dir.", [f"(−{n})^{{2}} = {n*n}", f"{n}^{{0}} = 1", f"Toplam {n*n+1}"])
for a, b in [(2, 3), (3, 5), (5, 2), (7, 2)]:
    add(TM, "Köklü Sayılar", 2, f"\\sqrt{{{a*a*b}}} sayısı aşağıdakilerden hangisine eşittir?", f"{a}\\sqrt{{{b}}}", [f"{b}\\sqrt{{{a}}}", f"{a*b}\\sqrt{{{a}}}", f"{a+1}\\sqrt{{{b}}}", f"{a}\\sqrt{{{b+1}}}"],
        "Kök içindeki tam kare dışarı çıkarılır.", [f"{a*a*b} = {a*a} · {b}", f"\\sqrt{{{a*a}}} = {a}"])
for a, b in [(7, 3), (9, 4), (12, 5), (15, 2)]:
    add(TM, "Çarpanlara Ayırma", 1, f"a = {a}, b = {b} için a^{{2}} − b^{{2}} ifadesinin değeri kaçtır?", a * a - b * b, [(a - b) ** 2, a * a + b * b, a - b, (a + b) ** 2],
        "a^{2} − b^{2} = (a − b)(a + b)", [f"({a} − {b})({a} + {b}) = {a-b} · {a+b}", f"= {a*a-b*b}"])
for a, b, c in [(2, 3, 5), (1, 4, 2), (3, 1, 6)]:
    add(TM, "Polinomlar", 1, f"P(x) = {a}x^{{2}} + {b}x + {c} polinomunun katsayılar toplamı kaçtır?", a + b + c, [c, a + b, a * b * c, a + c],
        "Katsayılar toplamı P(1) dir.", [f"P(1) = {a} + {b} + {c} = {a+b+c}"])
for a, b, c in [(3, 5, 12), (2, 7, 8), (4, 9, 20)]:
    x = Fr(b * c, a)
    add(TM, "Oran-Orantı", 1, f"\\frac{{{a}}}{{{b}}} = \\frac{{{c}}}{{x}} olduğuna göre x kaçtır?", x, [Fr(a * c, b), x + 1, x - 1, Fr(a * b, c)],
        "İçler dışlar çarpımı yapılır.", [f"{a}x = {b} · {c}", f"x = {num(x)}"])
for w, d in [(4, 15), (6, 10), (5, 12)]:
    t = 2 * w
    add(TM, "Oran-Orantı", 2, f"{w} işçi bir işi {d} günde yapıyor. Aynı güçte {t} işçi bu işi kaç günde yapar?", Fr(w * d, t), [2 * d, d - 2, d + 1, w * d],
        "İşçi sayısı ile süre ters orantılıdır.", [f"{w} · {d} = {t} · x", f"x = {num(Fr(w*d,t))}"], " gün")
for a, b, c in [(2, 3, 11), (3, 4, 19), (5, 7, 42), (4, 9, 29)]:
    add(TM, "Denklem Çözme", 1, f"{a}x + {b} = {c} denkleminin çözümü kaçtır?", Fr(c - b, a), [Fr(c + b, a), Fr(c - b, a) + 1, c - b, Fr(c - b, a) - 1],
        "Sabit terim karşıya atılır, sonra katsayıya bölünür.", [f"{a}x = {c-b}", f"x = {num(Fr(c-b,a))}"])
for k, m, r in [(3, 5, 20), (4, 7, 31), (5, 3, 38)]:
    add(TM, "Problemler: Sayı ve Kesir", 1, f"Bir sayının {k} katının {m} fazlası {r} ise bu sayı kaçtır?", Fr(r - m, k), [Fr(r + m, k), r - m, Fr(r - m, k) + 2, Fr(r - m, k) - 1],
        "Sayıya x denir ve denklem kurulur.", [f"{k}x + {m} = {r}", f"x = {num(Fr(r-m,k))}"])
for tot, p, q in [(120, 3, 4), (240, 5, 8), (90, 2, 3)]:
    add(TM, "Problemler: Sayı ve Kesir", 2, f"Ali parasının \\frac{{{p}}}{{{q}}} ini harcıyor. Başlangıçta {tot} lirası olduğuna göre kalan parası kaç liradır?", Fr(tot) * (1 - Fr(p, q)), [Fr(tot) * Fr(p, q), tot - p, Fr(tot, q), tot],
        "Kalan kesir 1 − harcanan kesirdir.", [f"Kalan oran: {num(1-Fr(p,q))}", f"{tot} · {num(1-Fr(p,q))} = {num(Fr(tot)*(1-Fr(p,q)))}"], " TL")
for s, f, y in [(12, 40, 6), (8, 32, 4), (15, 45, 5)]:
    r = f + y - 2 * (s + y) if False else None
    add(TM, "Problemler: Yaş", 2, f"Bir babanın yaşı {f}, oğlunun yaşı {s} tir. {y} yıl sonra yaşlarının toplamı kaç olur?", f + s + 2 * y, [f + s + y, f + s, f - s, f + s + 3 * y],
        "Her yıl iki kişinin yaşı da artar; toplam 2y artar.", [f"{f} + {s} = {f+s}", f"{f+s} + 2 · {y} = {f+s+2*y}"])
for s, f in [(10, 34), (12, 36), (7, 29)]:
    d = f - s
    add(TM, "Problemler: Yaş", 3, f"Baba {f}, oğlu {s} yaşındadır. Kaç yıl sonra babanın yaşı oğlunun yaşının 2 katı olur?", d - s, [d, d - s + 2, s, d + s],
        "Yaş farkı sabittir. Baba oğlunun iki katı olduğunda oğul yaş farkı kadar yaştadır.", [f"Yaş farkı {d}", f"Oğul {d} yaşında olmalı", f"{d} − {s} = {d-s}"], " yıl")
for a, p in [(200, 15), (360, 25), (480, 35), (150, 40)]:
    add(TM, "Problemler: Yüzde ve Kâr-Zarar", 1, f"{a} sayısının %{p} i kaçtır?", Fr(a * p, 100), [Fr(a * p, 100) * 2, a - p, Fr(a * p, 10), a + p],
        "Yüzde bulmak için sayı p ile çarpılıp 100'e bölünür.", [f"{a} · {p} / 100 = {num(Fr(a*p,100))}"])
for m, k in [(120, 25), (80, 50), (250, 20), (400, 15)]:
    s = Fr(m * (100 + k), 100)
    add(TM, "Problemler: Yüzde ve Kâr-Zarar", 2, f"Maliyeti {m} TL olan bir ürün %{k} kârla satılıyor. Satış fiyatı kaç TL dir?", s, [m + k, s + 10, Fr(m * k, 100), Fr(m * (100 - k), 100)],
        "Satış = maliyet × (100 + kâr) / 100.", [f"Kâr = {num(Fr(m*k,100))} TL", f"Satış = {num(s)} TL"], " TL")
for a, p, b, q in [(40, 20, 60, 30), (20, 10, 30, 20), (50, 30, 50, 10)]:
    r = Fr(a * p + b * q, a + b)
    add(TM, "Problemler: Karışım", 2, f"Tuz oranı %{p} olan {a} kg tuzlu su ile tuz oranı %{q} olan {b} kg tuzlu su karıştırılıyor. Karışımın tuz oranı yüzde kaçtır?", r, [Fr(p + q, 2) + 1, p + q, r + 2, r - 2],
        "Toplam tuz toplam kütleye bölünür.", [f"Toplam tuz: {num(Fr(a*p+b*q,100))} kg", f"Oran: %{num(r)}"])
for v1, v2, x in [(60, 40, 300), (70, 50, 240), (90, 30, 600)]:
    add(TM, "Problemler: Hareket", 2, f"Aralarında {x} km olan iki araç birbirine doğru {v1} km/sa ve {v2} km/sa hızlarla aynı anda hareket ediyor. Kaç saat sonra karşılaşırlar?", Fr(x, v1 + v2), [Fr(x, v1), Fr(x, v2), Fr(x, v1 - v2), Fr(x, v1 + v2) + 1],
        "Karşılaşma süresi yolun hızlar toplamına bölümüdür.", [f"v_{{1}} + v_{{2}} = {v1+v2}", f"t = {x} / {v1+v2} = {num(Fr(x,v1+v2))}"], " saat")
for v1, v2, x in [(80, 60, 100), (90, 50, 200), (75, 45, 60)]:
    add(TM, "Problemler: Hareket", 3, f"Aralarında {x} km olan iki araç aynı yönde gidiyor. Arkadaki aracın hızı {v1} km/sa, öndekinin {v2} km/sa olduğuna göre arkadaki kaç saatte yetişir?", Fr(x, v1 - v2), [Fr(x, v1 + v2), Fr(x, v2), Fr(x, v1), Fr(x, v1 - v2) + 1],
        "Yetişme süresi yolun hız farkına bölümüdür.", [f"v_{{1}} − v_{{2}} = {v1-v2}", f"t = {num(Fr(x,v1-v2))}"], " saat")
for a, b in [(6, 12), (10, 15), (12, 24), (20, 30)]:
    add(TM, "Problemler: İşçi ve Havuz", 2, f"Bir işi Ayşe tek başına {a} günde, Veli {b} günde bitiriyor. İkisi birlikte bu işi kaç günde bitirir?", 1 / (Fr(1, a) + Fr(1, b)), [a + b, Fr(a + b, 2), abs(b - a), 1 / (Fr(1, a) + Fr(1, b)) + 2],
        "Günlük iş hızları toplanır.", [f"\\frac{{1}}{{{a}}} + \\frac{{1}}{{{b}}} = {num(Fr(1,a)+Fr(1,b))}", f"Süre: {num(1/(Fr(1,a)+Fr(1,b)))}"], " gün")
for a, b, c in [(8, 6, 3), (12, 9, 4), (15, 10, 5), (20, 7, 2)]:
    add(TM, "Kümeler", 1, f"s(A) = {a}, s(B) = {b}, s(A ∩ B) = {c} ise s(A ∪ B) kaçtır?", a + b - c, [a + b, a + b + c, a - c, b - c],
        "s(A ∪ B) = s(A) + s(B) − s(A ∩ B)", [f"{a} + {b} − {c} = {a+b-c}"])
for n in (4, 5, 6):
    add(TM, "Kümeler", 1, f"{n} elemanlı bir kümenin alt küme sayısı kaçtır?", 2 ** n, [2 * n, n * n, 2 ** n - 1, 2 ** (n - 1)],
        "n elemanlı kümenin 2^{n} alt kümesi vardır.", [f"2^{{{n}}} = {2**n}"])
for a, b, x in [(2, 1, 3), (3, -2, 4), (5, 2, 2)]:
    add(TM, "Fonksiyonlar", 1, f"f(x) = {a}x {'+' if b>=0 else '−'} {abs(b)} ise f({x}) kaçtır?", a * x + b, [a * x - b, a + x + b, a * x, a * x + b + 1],
        "x yerine verilen değer yazılır.", [f"f({x}) = {a} · {x} {'+' if b>=0 else '−'} {abs(b)} = {a*x+b}"])
for n, r in [(5, 2), (6, 3), (7, 2), (8, 2)]:
    p = len(list(itertools.permutations(range(n), r)))
    add(TM, "Permütasyon", 2, f"{n} kişi arasından seçilen {r} kişi, {r} farklı göreve kaç farklı şekilde atanabilir?", p, [math.comb(n, r), n * r, math.factorial(n), p // 2 + 1],
        "Sıra önemli olduğu için permütasyon kullanılır.", [f"P({n}, {r}) = {p}"])
for n in (4, 5, 6):
    add(TM, "Permütasyon", 1, f"{n} kişi düz bir sıraya kaç farklı şekilde dizilebilir?", math.factorial(n), [n * n, n, 2 ** n, math.factorial(n - 1)],
        "n farklı nesne n! biçimde sıralanır.", [f"{n}! = {math.factorial(n)}"])
for n, r in [(6, 2), (7, 3), (8, 3), (10, 2)]:
    c = len(list(itertools.combinations(range(n), r)))
    add(TM, "Kombinasyon", 2, f"{n} kişilik bir gruptan {r} kişilik bir ekip kaç farklı şekilde seçilebilir?", c, [c * r, n * r, math.perm(n, r), n + r],
        "Seçimde sıra önemli değildir.", [f"C({n}, {r}) = {c}"])
for r, b, g in [(3, 5, 2), (4, 2, 6), (5, 3, 4)]:
    t = r + b + g
    add(TM, "Olasılık", 1, f"Bir torbada {r} kırmızı, {b} mavi, {g} yeşil top vardır. Rastgele çekilen bir topun kırmızı olma olasılığı kaçtır?", Fr(r, t), [Fr(b, t), Fr(g, t), Fr(r, b + g), Fr(1, 3) if Fr(r, t) != Fr(1, 3) else Fr(1, 4)],
        "İstenen durum sayısı toplam duruma bölünür.", [f"Toplam top {t}", f"P = {num(Fr(r,t))}"])
for tot in (7, 9, 5):
    cnt = sum(1 for a, b in itertools.product(range(1, 7), repeat=2) if a + b == tot)
    add(TM, "Olasılık", 2, f"İki zar birlikte atılıyor. Üst yüzeylerin toplamının {tot} olma olasılığı kaçtır?", Fr(cnt, 36), [Fr(cnt + 1, 36), Fr(cnt, 12), Fr(1, 2), Fr(cnt - 1, 36)],
        "36 durum içinde istenenler sayılır.", [f"İstenen durum sayısı {cnt}", f"P = {num(Fr(cnt,36))}"])
for data in [(4, 8, 6, 10, 12), (3, 7, 9, 5, 11), (10, 20, 30, 40, 50)]:
    m = Fr(sum(data), len(data))
    add(TM, "Veri ve İstatistik", 1, f"{', '.join(map(str, data))} verisinin aritmetik ortalaması kaçtır?", m, [m + 1, m - 1, max(data), min(data) + 1],
        "Toplam eleman sayısına bölünür.", [f"Toplam {sum(data)}", f"{sum(data)} / {len(data)} = {num(m)}"])
for data in [(9, 2, 7, 4, 12), (15, 3, 8, 21, 6), (5, 1, 14, 9, 3)]:
    add(TM, "Veri ve İstatistik", 1, f"{', '.join(map(str, data))} verisinin ortancası kaçtır?", median(data), [data[2] + 2 if data[2] != median(data) else data[0], max(data), min(data), median(data) + 3],
        "Veri sıralanır, ortadaki değer alınır.", [f"Sıralı: {', '.join(map(str, sorted(data)))}", f"Ortanca {median(data)}"])

# ---------- TYT GEOMETRİ ----------
for a in (35, 52, 68):
    add(GE, "Doğruda Açılar", 1, f"Bir açının ölçüsü {a}° ise bütünlerinin ölçüsü kaç derecedir?", 180 - a, [90 - a, 360 - a, a, 180 + a],
        "Bütünler açıların toplamı 180° dir.", [f"180 − {a} = {180-a}"], "°")
    add(GE, "Doğruda Açılar", 1, f"Bir açının ölçüsü {a}° ise tümlerinin ölçüsü kaç derecedir?", 90 - a, [180 - a, 360 - a, a, 90 + a],
        "Tümler açıların toplamı 90° dir.", [f"90 − {a} = {90-a}"], "°")
for a, b in [(50, 60), (45, 75), (30, 110), (80, 40)]:
    c = 180 - a - b
    add(GE, "Üçgende Açılar", 1, f"Bir ABC üçgeninde m(A) = {a}°, m(B) = {b}° ise m(C) kaç derecedir?", c, [180 - a, 180 - b, a + b, c + 10],
        "Üçgenin iç açıları toplamı 180° dir.", [f"{a} + {b} = {a+b}", f"180 − {a+b} = {c}"], "°", figure=triangle_fig("ABC üçgeni", "A", "B", "C"))
    add(GE, "Üçgende Açılar", 2, f"Bir üçgende iki iç açı {a}° ve {b}° dir. Üçüncü köşedeki dış açının ölçüsü kaçtır?", a + b, [180 - a - b, 180 - a, a + b + 10, 180 - b],
        "Dış açı, komşu olmayan iki iç açının toplamıdır.", [f"{a} + {b} = {a+b}"], "°")
for b, c in [(3, 7), (5, 9), (6, 10), (8, 13)]:
    vals = [x for x in range(1, b + c + 1) if abs(b - c) < x < b + c]
    add(GE, "Üçgenler", 2, f"Kenar uzunlukları {b} cm, {c} cm ve x cm olan bir üçgende x in alabileceği kaç farklı tam sayı değeri vardır?", len(vals), [len(vals) + 1, len(vals) - 1, b + c, len(vals) + 2],
        "Üçgen eşitsizliğine göre |b − c| < x < b + c.", [f"{abs(b-c)} < x < {b+c}", f"{vals[0]} ile {vals[-1]} arası {len(vals)} değer"])
for a, h in [(8, 5), (12, 7), (10, 9), (14, 6)]:
    add(GE, "Üçgende Alan", 1, f"Taban uzunluğu {a} cm, bu tabana ait yüksekliği {h} cm olan üçgenin alanı kaç cm^{{2}} dir?", Fr(a * h, 2), [a * h, a + h, Fr(a * h, 4), Fr(a * h, 2) + h],
        "Alan = taban × yükseklik / 2.", [f"{a} · {h} / 2 = {num(Fr(a*h,2))}"], " cm^{2}", figure=triangle_fig("Taban ve yüksekliği verilen üçgen", "B", "C", "A"))
for a, b, c in [(3, 4, 5), (5, 12, 13), (8, 15, 17), (6, 8, 10), (7, 24, 25)]:
    add(GE, "Özel Üçgenler", 1, f"Dik kenarları {a} cm ve {b} cm olan dik üçgenin hipotenüsü kaç cm dir?", c, [a + b, c + 1, c - 1, b + 1],
        "Pisagor bağıntısı uygulanır.", [f"{a}^{{2}} + {b}^{{2}} = {a*a+b*b}", f"\\sqrt{{{a*a+b*b}}} = {c}"], " cm", figure=right_fig("Dik üçgen", f"{a}", f"{b}", "x"))
for h in (6, 10, 14, 18):
    add(GE, "Özel Üçgenler", 2, f"30-60-90 üçgeninde hipotenüs {h} cm ise 30° lik açının karşısındaki kenar kaç cm dir?", Fr(h, 2), [h, 2 * h, Fr(h, 3), Fr(h, 2) + 1],
        "30° karşısındaki kenar hipotenüsün yarısıdır.", [f"{h} / 2 = {num(Fr(h,2))}"], " cm")
for a, b in [(6, 4), (9, 5), (12, 7)]:
    add(GE, "Dörtgenler", 1, f"Kenarları {a} cm ve {b} cm olan dikdörtgenin alanı kaç cm^{{2}} dir?", a * b, [2 * (a + b), a + b, a * b + a, a * a],
        "Dikdörtgenin alanı kenarlarının çarpımıdır.", [f"{a} · {b} = {a*b}"], " cm^{2}")
for a, c, h in [(4, 8, 5), (6, 10, 4), (7, 13, 6)]:
    add(GE, "Dörtgenler", 2, f"Tabanları {a} cm ve {c} cm, yüksekliği {h} cm olan yamuğun alanı kaç cm^{{2}} dir?", Fr((a + c) * h, 2), [(a + c) * h, a * c, Fr(a * c * h, 2), a + c + h],
        "Yamuk alanı = (taban toplamı × yükseklik) / 2.", [f"({a} + {c}) · {h} / 2 = {num(Fr((a+c)*h,2))}"], " cm^{2}")
for n in (5, 6, 8, 10):
    add(GE, "Çokgenler", 1, f"{n} kenarlı bir çokgenin iç açıları toplamı kaç derecedir?", (n - 2) * 180, [n * 180, 360, (n - 1) * 180, (n - 3) * 180],
        "İç açılar toplamı (n − 2) · 180°.", [f"({n} − 2) · 180 = {(n-2)*180}"], "°")
    add(GE, "Çokgenler", 2, f"{n} kenarlı bir çokgenin köşegen sayısı kaçtır?", n * (n - 3) // 2, [n * (n - 3), n * (n - 1) // 2, n, n * (n - 3) // 2 + 2],
        "Köşegen sayısı n(n − 3)/2.", [f"{n} · {n-3} / 2 = {n*(n-3)//2}"])
for r in (3, 5, 7, 9):
    add(GE, "Çember ve Daire", 1, f"Yarıçapı {r} cm olan dairenin alanı kaç π cm^{{2}} dir?", r * r, [2 * r, r * r + r, 4 * r, r * r * 2],
        "Alan = πr^{2}", [f"π · {r}^{{2}} = {r*r}π"], "π")
    add(GE, "Çember ve Daire", 1, f"Yarıçapı {r} cm olan çemberin çevresi kaç π cm dir?", 2 * r, [r * r, r, 4 * r, 2 * r + 2],
        "Çevre = 2πr", [f"2 · {r} = {2*r}"], "π")
for x1, y1, x2, y2 in [(1, 2, 4, 6), (0, 0, 6, 8), (2, 1, 7, 13), (-1, 2, 2, 6)]:
    d = math.isqrt((x2 - x1) ** 2 + (y2 - y1) ** 2)
    add(GE, "Analitik Geometri", 2, f"A({x1}, {y1}) ve B({x2}, {y2}) noktaları arasındaki uzaklık kaç birimdir?", d, [abs(x2 - x1) + abs(y2 - y1), d + 1, d - 1, d + 2],
        "İki nokta arası uzaklık formülü uygulanır.", [f"Δx = {x2-x1}, Δy = {y2-y1}", f"\\sqrt{{{(x2-x1)**2+(y2-y1)**2}}} = {d}"])
    add(GE, "Analitik Geometri", 1, f"A({x1}, {y1}) ve B({x2}, {y2}) noktalarından geçen doğrunun eğimi kaçtır?", Fr(y2 - y1, x2 - x1), [Fr(x2 - x1, y2 - y1), Fr(y2 - y1, x2 - x1) + 1, -Fr(y2 - y1, x2 - x1), y2 - y1],
        "Eğim = Δy / Δx", [f"{y2-y1} / {x2-x1} = {num(Fr(y2-y1,x2-x1))}"])
for a in (3, 4, 5):
    add(GE, "Katı Cisimler", 1, f"Bir ayrıtı {a} cm olan küpün hacmi kaç cm^{{3}} tür?", a ** 3, [6 * a * a, a * a, 3 * a, a ** 3 + a],
        "Küpün hacmi a^{3}.", [f"{a}^{{3}} = {a**3}"], " cm^{3}")
    add(GE, "Katı Cisimler", 2, f"Taban yarıçapı {a} cm, yüksekliği {a+2} cm olan silindirin hacmi kaç π cm^{{3}} tür?", a * a * (a + 2), [2 * a * (a + 2), a * (a + 2), a * a * a, a * a * (a + 2) + a],
        "Silindir hacmi πr^{2}h.", [f"{a}^{{2}} · {a+2} = {a*a*(a+2)}"], "π cm^{3}")

# ---------- TYT FİZİK ----------
for m, v in [(60, 20), (120, 40), (45, 15), (100, 25)]:
    add(FZ, "Madde ve Özellikleri", 1, f"Kütlesi {m} g, hacmi {v} cm^{{3}} olan cismin özkütlesi kaç g/cm^{{3}} tür?", Fr(m, v), [m * v, Fr(v, m), m - v, Fr(m, v) + 1],
        "d = m / V", [f"{m} / {v} = {num(Fr(m,v))}"])
for m1, d1, m2, d2 in [(30, 3, 20, 2), (40, 2, 60, 3), (60, 4, 40, 2)]:
    v = Fr(m1, d1) + Fr(m2, d2)
    add(FZ, "Madde ve Özellikleri", 3, f"Özkütlesi {d1} g/cm^{{3}} olan {m1} g sıvı ile özkütlesi {d2} g/cm^{{3}} olan {m2} g sıvı karıştırılıyor. Karışımın özkütlesi kaç g/cm^{{3}} tür? (Hacim değişimi yoktur.)", Fr(m1 + m2) / v, [Fr(d1 + d2, 2), d1 + d2, Fr(m1 + m2) / v + 1, Fr(d1 * d2, d1 + d2)],
        "Toplam kütle toplam hacme bölünür.", [f"V = {num(Fr(m1,d1))} + {num(Fr(m2,d2))} = {num(v)}", f"d = {m1+m2} / {num(v)}"])
for m, a in [(5, 4), (3, 6), (8, 2), (10, 3)]:
    add(FZ, "Hareket ve Kuvvet", 1, f"{m} kg kütleli cisme etkiyen net kuvvet cisme {a} m/s^{{2}} ivme kazandırıyor. Net kuvvet kaç N dur?", m * a, [m + a, Fr(m, a), m * a * 2, m * a + m],
        "F = m · a", [f"{m} · {a} = {m*a}"], " N")
for v, t in [(20, 5), (15, 4), (30, 3), (12, 6)]:
    add(FZ, "Hareket ve Kuvvet", 1, f"Sabit {v} m/s hızla giden araç {t} s de kaç m yol alır?", v * t, [v + t, Fr(v, t), v * t * 2, v * t + v],
        "x = v · t", [f"{v} · {t} = {v*t}"], " m")
for f, x in [(10, 3), (25, 4), (40, 2), (15, 6)]:
    add(FZ, "İş, Güç ve Enerji", 1, f"{f} N luk kuvvet bir cismi kuvvet doğrultusunda {x} m hareket ettiriyor. Yapılan iş kaç J dür?", f * x, [f + x, Fr(f, x), f * x * 2, f * x - f],
        "W = F · x", [f"{f} · {x} = {f*x}"], " J")
for w, t in [(600, 30), (1200, 60), (450, 15)]:
    add(FZ, "İş, Güç ve Enerji", 1, f"{t} saniyede {w} J iş yapan makinenin gücü kaç W tır?", Fr(w, t), [w * t, w - t, Fr(t, w) + 1, Fr(w, t) * 2],
        "P = W / t", [f"{w} / {t} = {num(Fr(w,t))}"], " W")
for m, v in [(2, 3), (4, 5), (6, 2), (10, 4)]:
    add(FZ, "İş, Güç ve Enerji", 2, f"{m} kg kütleli cisim {v} m/s hızla gidiyor. Kinetik enerjisi kaç J dür?", Fr(m * v * v, 2), [m * v, m * v * v, Fr(m * v, 2), Fr(m * v * v, 2) + m],
        "E_{k} = \\frac{1}{2} mv^{2}", [f"\\frac{{1}}{{2}} · {m} · {v*v} = {num(Fr(m*v*v,2))}"], " J")
for m, h in [(2, 5), (3, 10), (5, 4)]:
    add(FZ, "İş, Güç ve Enerji", 2, f"{m} kg kütleli cisim yerden {h} m yükseklikte duruyor. Yere göre potansiyel enerjisi kaç J dür? (g = 10 m/s^{{2}})", m * 10 * h, [m * h, m * 10, m * 10 * h * 2, m + h],
        "E_{p} = mgh", [f"{m} · 10 · {h} = {m*10*h}"], " J")
for m, c, dt in [(200, 1, 30), (500, 1, 20), (100, 2, 15)]:
    add(FZ, "Isı ve Sıcaklık", 2, f"Öz ısısı {c} cal/g°C olan {m} g maddenin sıcaklığını {dt} °C artırmak için kaç cal ısı gerekir?", m * c * dt, [m * dt, m + c + dt, m * c, m * c * dt * 2],
        "Q = m · c · ΔT", [f"{m} · {c} · {dt} = {m*c*dt}"], " cal")
for v, r in [(12, 4), (24, 6), (30, 5), (9, 3)]:
    add(FZ, "Elektrik ve Manyetizma", 1, f"{r} Ω luk dirençli bir iletkenin uçları arasına {v} V gerilim uygulanıyor. Akım kaç A dir?", Fr(v, r), [v * r, v - r, Fr(r, v), Fr(v, r) + 1],
        "V = I · R", [f"I = {v} / {r} = {num(Fr(v,r))}"], " A")
for a, b in [(3, 6), (4, 4), (2, 6), (6, 12)]:
    add(FZ, "Elektrik ve Manyetizma", 2, f"{a} Ω ve {b} Ω luk iki direnç paralel bağlanıyor. Eşdeğer direnç kaç Ω dur?", 1 / (Fr(1, a) + Fr(1, b)), [a + b, abs(a - b) + 1, Fr(a + b, 2), 1 / (Fr(1, a) + Fr(1, b)) + 1],
        "Paralelde direncin tersleri toplanır.", [f"\\frac{{1}}{{R}} = \\frac{{1}}{{{a}}} + \\frac{{1}}{{{b}}}", f"R = {num(1/(Fr(1,a)+Fr(1,b)))}"], " Ω")
    add(FZ, "Elektrik ve Manyetizma", 1, f"{a} Ω ve {b} Ω luk iki direnç seri bağlanıyor. Eşdeğer direnç kaç Ω dur?", a + b, [a * b, abs(a - b) + 1, a + b + 2, 1 / (Fr(1, a) + Fr(1, b))],
        "Seri bağlamada dirençler toplanır.", [f"{a} + {b} = {a+b}"], " Ω")
for f, a in [(100, 2), (300, 5), (80, 4)]:
    add(FZ, "Basınç ve Kaldırma Kuvveti", 1, f"{f} N ağırlığındaki cisim {a} m^{{2}} lik yüzeye oturuyor. Yüzeye yaptığı basınç kaç Pa dır?", Fr(f, a), [f * a, f - a, Fr(a, f), Fr(f, a) * 2],
        "P = F / A", [f"{f} / {a} = {num(Fr(f,a))}"], " Pa")
for h, d in [(2, 1000), (5, 1000), (3, 800)]:
    add(FZ, "Basınç ve Kaldırma Kuvveti", 2, f"Özkütlesi {d} kg/m^{{3}} olan sıvının {h} m derinliğindeki sıvı basıncı kaç Pa dır? (g = 10 m/s^{{2}})", h * d * 10, [h * d, d * 10, h * 10, h * d * 100],
        "P = h · d · g", [f"{h} · {d} · 10 = {h*d*10}"], " Pa")
for v, d in [(2, 1000), (0.5, 1000), (3, 800)]:
    add(FZ, "Basınç ve Kaldırma Kuvveti", 2, f"Hacmi {num(float(v))} m^{{3}} olan cisim özkütlesi {d} kg/m^{{3}} sıvıya tamamen batıyor. Kaldırma kuvveti kaç N dur? (g = 10 m/s^{{2}})", int(v * d * 10), [int(v * d), d * 10, int(v * d * 100), int(v * d * 10) + 100],
        "F_{k} = V · d · g", [f"{num(float(v))} · {d} · 10 = {int(v*d*10)}"], " N")
for f, l in [(5, 2), (10, 3), (4, 6), (25, 2)]:
    add(FZ, "Dalgalar", 1, f"Frekansı {f} Hz, dalga boyu {l} m olan dalganın hızı kaç m/s dir?", f * l, [f + l, Fr(f, l), Fr(l, f), f * l + 2],
        "v = λ · f", [f"{l} · {f} = {f*l}"], " m/s")
for a in (20, 35, 50):
    add(FZ, "Optik", 1, f"Düzlem aynaya gelen ışının gelme açısı {a}° ise yansıyan ışın ile gelen ışın arasındaki açı kaç derecedir?", 2 * a, [a, 90 - a, 180 - a, 180 - 2 * a],
        "Gelme açısı yansıma açısına eşittir; aradaki açı ikisinin toplamıdır.", [f"{a} + {a} = {2*a}"], "°")

# ---------- TYT KİMYA ----------
for z, a, name in [(11, 23, "Na"), (17, 35, "Cl"), (8, 16, "O"), (20, 40, "Ca"), (13, 27, "Al")]:
    add(KM, "Atom ve Periyodik Sistem", 1, f"Atom numarası {z}, kütle numarası {a} olan {name} atomunun nötron sayısı kaçtır?", a - z, [a + z, z, a, a - z + 1],
        "Nötron sayısı = kütle numarası − proton sayısı.", [f"{a} − {z} = {a-z}"])
for m, ma, label in [(36, 18, "H_{2}O"), (88, 44, "CO_{2}"), (80, 40, "NaOH"), (34, 17, "NH_{3}")]:
    add(KM, "Kimyasal Hesaplamalar", 1, f"{m} gram {label} kaç moldür? (Mol kütlesi {ma} g/mol)", Fr(m, ma), [m * ma, Fr(ma, m), m - ma, Fr(m, ma) + 1],
        "n = m / M_{A}", [f"{m} / {ma} = {num(Fr(m,ma))}"], " mol")
for n, ma, label in [(3, 18, "H_{2}O"), (2, 44, "CO_{2}"), (5, 2, "H_{2}")]:
    add(KM, "Kimyasal Hesaplamalar", 1, f"{n} mol {label} kaç gramdır? (Mol kütlesi {ma} g/mol)", n * ma, [n + ma, Fr(ma, n), n * ma * 2, n * ma + 10],
        "m = n · M_{A}", [f"{n} · {ma} = {n*ma}"], " g")
for n in (2, 3, 4):
    add(KM, "Kimyasal Hesaplamalar", 2, f"Normal koşullarda {n} mol ideal gaz kaç litre hacim kaplar?", float(Fr(224, 10) * n), [float(Fr(224, 10) * (n + 1)), 22.4, n, float(Fr(112, 10) * n)],
        "Normal koşullarda 1 mol gaz 22,4 L hacim kaplar.", [f"{n} · 22,4"], " L")

# ---------- AYT MATEMATİK ----------
for a, b, y in [(2, 1, 7), (3, 2, 14), (5, 1, 16), (4, 3, 15)]:
    add(AM, "Fonksiyonlar", 2, f"f(x) = {a}x + {b} ise f^{{−1}}({y}) kaçtır?", Fr(y - b, a), [a * y + b, Fr(y + b, a), Fr(y - b, a) + 1, y - b],
        "f(x) = y denklemi çözülür.", [f"{a}x + {b} = {y}", f"x = {num(Fr(y-b,a))}"])
for a, b, c, d, x in [(2, 1, 3, 2, 1), (1, 3, 2, 1, 2), (3, 2, 1, 4, 1)]:
    add(AM, "Fonksiyonlar", 2, f"f(x) = {a}x + {b} ve g(x) = {c}x + {d} ise (f ∘ g)({x}) kaçtır?", a * (c * x + d) + b, [c * (a * x + b) + d, (a * x + b) * (c * x + d), a * x + b + c * x + d, a * (c * x + d)],
        "Önce g sonra f uygulanır.", [f"g({x}) = {c*x+d}", f"f({c*x+d}) = {a*(c*x+d)+b}"])
for a, b, c, k in [(1, 2, 3, 2), (2, -1, 4, 1), (1, -3, 5, 3)]:
    bs = ("+ " + str(b)) if b >= 0 else ("− " + str(-b))
    add(AM, "Polinomlar", 2, f"P(x) = {'' if a==1 else a}x^{{2}} {bs}x + {c} polinomunun x − {k} ile bölümünden kalan kaçtır?", a * k * k + b * k + c, [c, a + b + c, a * k * k + b * k, k + c],
        "Kalan P(k) dır.", [f"P({k}) = {a*k*k+b*k+c}"])
for r1, r2 in [(2, 3), (1, 5), (3, 4), (2, 7)]:
    s, p = r1 + r2, r1 * r2
    add(AM, "İkinci Dereceden Denklemler", 1, f"x^{{2}} − {s}x + {p} = 0 denkleminin kökler toplamı kaçtır?", s, [p, -s, r2 - r1, s + p],
        "Kökler toplamı −b/a dır.", [f"−(−{s}) / 1 = {s}"])
    add(AM, "İkinci Dereceden Denklemler", 1, f"x^{{2}} − {s}x + {p} = 0 denkleminin kökler çarpımı kaçtır?", p, [s, -p, r2 - r1, s + p],
        "Kökler çarpımı c/a dır.", [f"{p} / 1 = {p}"])
    add(AM, "İkinci Dereceden Denklemler", 2, f"x^{{2}} − {s}x + {p} = 0 denkleminin diskriminantı kaçtır?", s * s - 4 * p, [s * s + 4 * p, s * s, 4 * p, s * s - 2 * p],
        "Δ = b^{2} − 4ac", [f"{s}^{{2}} − 4 · {p} = {s*s-4*p}"])
for r, k in [(2, 1), (3, -2), (1, 4), (4, 3)]:
    b, c = -2 * r, r * r + k
    ks = f"{num(k)}"
    add(AM, "Parabol", 2, f"y = x^{{2}} − {2*r}x + {c} parabolünün tepe noktasının ordinatı kaçtır?", k, [r, c, -k, k + r],
        "r = −b/2a ile apsis bulunur, sonra yerine yazılır.", [f"r = {r}", f"k = {r*r} − {2*r*r} + {c} = {ks}"])
for a, b, m in [(3, 4, 5), (6, 8, 10), (5, 12, 13), (8, 15, 17)]:
    add(AM, "Karmaşık Sayılar", 1, f"z = {a} + {b}i karmaşık sayısının modülü kaçtır?", m, [a + b, a * b, m + 1, m - 1],
        "|z| = \\sqrt{a^{2} + b^{2}}", [f"\\sqrt{{{a*a+b*b}}} = {m}"])
for n in (6, 7, 9, 11, 14, 25):
    v = {0: "1", 1: "i", 2: "−1", 3: "−i"}[n % 4]
    add(AM, "Karmaşık Sayılar", 2, f"i^{{{n}}} ifadesinin değeri nedir?", v, [x for x in ["1", "i", "−1", "−i", "0"] if x != v],
        "i nin kuvvetleri 4'lük periyotla tekrar eder.", [f"{n} in 4 ile bölümünden kalan {n%4}", f"Sonuç {v}"])
for n in (5, 6, 7):
    add(AM, "Permütasyon", 2, f"{n} kişi yuvarlak bir masa çevresine kaç farklı şekilde oturabilir?", math.factorial(n - 1), [math.factorial(n), n * n, math.factorial(n - 2), math.factorial(n - 1) * 2],
        "Dairesel sıralamada (n − 1)! kullanılır.", [f"({n} − 1)! = {math.factorial(n-1)}"])
for w, cnt in [("ANNE", 12), ("KALEM", 120), ("ARABA", 20), ("TEKNE", 60)]:
    assert cnt == len(set(itertools.permutations(w)))
    add(AM, "Permütasyon", 2, f"{w} sözcüğünün harfleriyle anlamlı veya anlamsız kaç farklı sözcük yazılabilir?", cnt, [math.factorial(len(w)), cnt // 2, cnt * 2, cnt + len(w)],
        "Tekrar eden harfler için bölme yapılır.", [f"Farklı dizilim sayısı {cnt}"])
for k, m, a, b in [(4, 3, 2, 1), (5, 4, 2, 2), (6, 2, 3, 1)]:
    r = math.comb(k, a) * math.comb(m, b)
    add(AM, "Kombinasyon", 2, f"{k} kız ve {m} erkek öğrenci arasından {a} kız ve {b} erkek kaç farklı şekilde seçilebilir?", r, [math.comb(k + m, a + b), math.comb(k, a) + math.comb(m, b), r * 2, k * m],
        "Kız ve erkek seçimleri ayrı yapılıp çarpılır.", [f"C({k},{a}) · C({m},{b}) = {r}"])
for n, r in [(5, 2), (6, 3), (7, 2), (4, 1)]:
    c = math.comb(n, r)
    add(AM, "Binom", 2, f"(x + 1)^{{{n}}} açılımında x^{{{n-r}}} li terimin katsayısı kaçtır?", c, [n, c + 1, n * r, math.comb(n, r + 1) if math.comb(n, r + 1) != c else c * 2],
        "Katsayı C(n, r) ile bulunur.", [f"C({n}, {r}) = {c}"])
    add(AM, "Binom", 1, f"(x + 2)^{{{n}}} açılımında kaç terim vardır?", n + 1, [n, n + 2, 2 * n, n - 1],
        "n. kuvvet açılımında n + 1 terim vardır.", [f"{n} + 1 = {n+1}"])
for s in (2, 3, 7):
    add(AM, "Olasılık", 2, f"Hilesiz bir para {s} kez atılıyor. Tümünün yazı gelme olasılığı kaçtır?", Fr(1, 2 ** s), [Fr(1, s), Fr(1, 2), Fr(1, 2 * s), Fr(s, 2 ** s) if s != 2 else Fr(1, 3)],
        "Bağımsız olaylarda olasılıklar çarpılır.", [f"(1/2)^{{{s}}} = {num(Fr(1,2**s))}"])
for a, s, c in [(30, "\\frac{1}{2}", "sin"), (60, "\\frac{1}{2}", "cos"), (45, "1", "tan"), (0, "0", "sin"), (90, "1", "sin"), (0, "1", "cos")]:
    add(AM, "Trigonometri", 1, f"{c} {a}° değeri kaçtır?", s, [x for x in ["\\frac{1}{2}", "\\frac{\\sqrt{3}}{2}", "1", "0", "\\frac{\\sqrt{2}}{2}", "−1"] if x != s],
        "Temel açıların trigonometrik değerleri kullanılır.", [f"{c} {a}° = {s}"])
for b, e in [(2, 5), (3, 4), (5, 3), (10, 3), (2, 8)]:
    add(AM, "Logaritma", 1, f"log_{{{b}}}{b**e} değeri kaçtır?", e, [b, b ** e // b, e + 1, e - 1],
        "log_{a}a^{n} = n", [f"{b}^{{{e}}} = {b**e}"])
for a, b in [(2, 3), (4, 5), (20, 5), (25, 4)]:
    v = a * b
    if v in (10, 100, 1000):
        add(AM, "Logaritma", 2, f"log {a} + log {b} ifadesinin değeri kaçtır?", round(math.log10(v)), [a + b, 0, round(math.log10(v)) + 1, v],
            "log a + log b = log(ab)", [f"log {v} = {round(math.log10(v))}"])
for a1, d, n in [(2, 3, 10), (5, 2, 8), (1, 4, 6), (7, 5, 5)]:
    an = a1 + (n - 1) * d
    add(AM, "Diziler", 1, f"İlk terimi {a1}, ortak farkı {d} olan aritmetik dizinin {n}. terimi kaçtır?", an, [a1 + n * d, a1 * d, an + d, an - 1],
        "a_{n} = a_{1} + (n − 1)d", [f"{a1} + {n-1} · {d} = {an}"])
    add(AM, "Diziler", 2, f"İlk terimi {a1}, ortak farkı {d} olan aritmetik dizinin ilk {n} teriminin toplamı kaçtır?", Fr(n * (a1 + an), 2), [n * an, Fr(n * (a1 + an), 2) + d, a1 + an, n * a1],
        "S_{n} = n(a_{1} + a_{n})/2", [f"a_{{{n}}} = {an}", f"{n}({a1} + {an}) / 2 = {num(Fr(n*(a1+an),2))}"])
for a1, r, n in [(2, 3, 4), (1, 2, 6), (3, 2, 5)]:
    add(AM, "Diziler", 2, f"İlk terimi {a1}, ortak çarpanı {r} olan geometrik dizinin {n}. terimi kaçtır?", a1 * r ** (n - 1), [a1 * r ** n, a1 + r * (n - 1), a1 * r * n, a1 * r ** (n - 2)],
        "a_{n} = a_{1} r^{n−1}", [f"{a1} · {r}^{{{n-1}}} = {a1*r**(n-1)}"])
for a in (2, 3, 5, 7):
    add(AM, "Limit", 2, f"x → {a} için \\frac{{x^{{2}} − {a*a}}}{{x − {a}}} ifadesinin limiti kaçtır?", 2 * a, [a, 0, a * a, 2 * a + 1],
        "Pay çarpanlarına ayrılır ve sadeleştirilir.", [f"(x − {a})(x + {a}) / (x − {a}) = x + {a}", f"{a} + {a} = {2*a}"])
for n, x in [(3, 2), (4, 1), (2, 5), (5, 1)]:
    add(AM, "Türev", 1, f"f(x) = x^{{{n}}} ise f'({x}) kaçtır?", n * x ** (n - 1), [x ** n, n * x ** n, n * x, (n - 1) * x ** (n - 1)],
        "(x^{n})' = n x^{n−1}", [f"f'(x) = {n}x^{{{n-1}}}", f"f'({x}) = {n*x**(n-1)}"])
for a, b, x in [(1, -6, 3), (2, -8, 2), (3, -12, 2)]:
    add(AM, "Türev", 2, f"f(x) = {'' if a==1 else a}x^{{2}} − {-b}x fonksiyonunun türevi hangi x değerinde sıfır olur?", x, [-x, 2 * x, x + 1, -b],
        "f'(x) = 0 çözülür.", [f"f'(x) = {2*a}x − {-b}", f"x = {x}"])
for n, b in [(1, 2), (2, 3), (3, 2), (1, 4)]:
    v = Fr(b ** (n + 1), n + 1)
    add(AM, "İntegral", 2, f"0 dan {b} ye x^{{{n}}} fonksiyonunun belirli integrali kaçtır?", v, [b ** n, v + 1, Fr(b ** n, n), v * 2],
        "∫x^{n} dx = x^{n+1}/(n + 1)", [f"{b}^{{{n+1}}} / {n+1} = {num(v)}"])
for m in (Fr(2), Fr(3), Fr(1, 2), Fr(-4)):
    add(AM, "Analitik Geometri", 2, f"Eğimi {num(m)} olan doğruya dik bir doğrunun eğimi kaçtır?", -1 / m, [m, 1 / m, -m, 1 + m if 1 + m != -1 / m else m + 2],
        "Dik doğruların eğimleri çarpımı −1 dir.", [f"m_{{2}} = −1 / ({num(m)}) = {num(-1/m)}"])
for a, b, r in [(0, 0, 5), (1, 2, 3), (-2, 3, 4), (2, -1, 6)]:
    A = f"(x − {a})" if a >= 0 else f"(x + {-a})"
    B = f"(y − {b})" if b >= 0 else f"(y + {-b})"
    add(AM, "Çemberin Analitiği", 1, f"{A}^{{2}} + {B}^{{2}} = {r*r} çemberinin yarıçapı kaçtır?", r, [r * r, 2 * r, r + 1, r - 1],
        "Sağ taraf r^{2} dir.", [f"r = \\sqrt{{{r*r}}} = {r}"])
for a, b, c, d in [(3, 4, 12, 13), (2, 3, 6, 7), (1, 2, 2, 3), (2, 10, 11, 15)]:
    add(AM, "Uzay Geometri", 2, f"Ayrıtları {a}, {b}, {c} olan dikdörtgenler prizmasının cisim köşegeni kaç birimdir?", d, [a + b + c, d + 1, d - 1, c + 1],
        "Köşegen = \\sqrt{a^{2} + b^{2} + c^{2}}", [f"\\sqrt{{{a*a+b*b+c*c}}} = {d}"])

# ---------- AYT FİZİK ----------
for a, b, c in [(3, 4, 5), (6, 8, 10), (5, 12, 13)]:
    add(AF, "Vektörler", 1, f"{a} N ve {b} N luk iki kuvvet birbirine dik uygulanıyor. Bileşke kuvvet kaç N dur?", c, [a + b, abs(b - a), c + 1, c - 1],
        "Dik kuvvetlerde Pisagor uygulanır.", [f"\\sqrt{{{a*a+b*b}}} = {c}"], " N")
for a, t in [(2, 5), (3, 4), (5, 6), (4, 3)]:
    add(AF, "Hareket", 1, f"Durgun hâlden {a} m/s^{{2}} ivmeyle hızlanan aracın {t} s sonraki hızı kaç m/s dir?", a * t, [a + t, Fr(a, t), Fr(a * t * t, 2), a * t * 2],
        "v = a · t", [f"{a} · {t} = {a*t}"], " m/s")
    add(AF, "Hareket", 2, f"Durgun hâlden {a} m/s^{{2}} ivmeyle hızlanan aracın {t} s de aldığı yol kaç m dir?", Fr(a * t * t, 2), [a * t, a * t * t, Fr(a * t, 2), Fr(a * t * t, 2) + a],
        "x = \\frac{1}{2}at^{2}", [f"\\frac{{1}}{{2}} · {a} · {t*t} = {num(Fr(a*t*t,2))}"], " m")
for m, F, f in [(2, 14, 4), (4, 30, 10), (5, 40, 15)]:
    add(AF, "Newton'un Hareket Yasaları", 2, f"{m} kg kütleli cisme {F} N kuvvet uygulanıyor, sürtünme kuvveti {f} N dur. Cismin ivmesi kaç m/s^{{2}} dir?", Fr(F - f, m), [Fr(F, m), Fr(F + f, m), F - f, Fr(F - f, m) + 1],
        "F_{net} = F − f_{s} = ma", [f"F_{{net}} = {F-f}", f"a = {num(Fr(F-f,m))}"])
for t in (2, 3, 4, 5):
    add(AF, "Atışlar", 1, f"Serbest düşen bir cisim {t} s de kaç m yol alır? (g = 10 m/s^{{2}})", 5 * t * t, [10 * t, 5 * t, 10 * t * t, 5 * t * t + 10],
        "h = \\frac{1}{2}gt^{2}", [f"5 · {t*t} = {5*t*t}"], " m")
for m, v in [(3, 4), (2, 10), (5, 6)]:
    add(AF, "İtme ve Momentum", 1, f"{m} kg kütleli cisim {v} m/s hızla gidiyor. Momentumu kaç kg·m/s dir?", m * v, [m + v, Fr(m * v * v, 2), Fr(m, v), m * v * 2],
        "p = m · v", [f"{m} · {v} = {m*v}"])
for m1, v1, m2 in [(2, 6, 1), (3, 4, 3), (4, 5, 1)]:
    add(AF, "İtme ve Momentum", 3, f"{m1} kg kütleli cisim {v1} m/s hızla duran {m2} kg kütleli cisme çarpıp yapışıyor. Ortak hız kaç m/s olur?", Fr(m1 * v1, m1 + m2), [v1, Fr(v1, 2), Fr(m1 * v1, m2), Fr(m1 * v1, m1 + m2) + 1],
        "Momentum korunur.", [f"{m1} · {v1} = ({m1} + {m2})v", f"v = {num(Fr(m1*v1,m1+m2))}"], " m/s")
for c, v in [(2, 3), (4, 5), (6, 2)]:
    add(AF, "Elektrik Alan ve Potansiyel", 1, f"Sığası {c} F olan sığaç {v} V ile yükleniyor. Yükü kaç C olur?", c * v, [Fr(c, v), c + v, Fr(c * v * v, 2), c * v * 2],
        "q = C · V", [f"{c} · {v} = {c*v}"], " C")
for v, r in [(4, 2), (6, 3), (10, 5)]:
    add(AF, "Çembersel Hareket", 2, f"{r} m yarıçaplı çembersel yörüngede {v} m/s sabit süratle dolanan cismin merkezcil ivmesi kaç m/s^{{2}} dir?", Fr(v * v, r), [Fr(v, r), v * r, v * v * r, Fr(v * v, r) + 1],
        "a = v^{2} / r", [f"{v*v} / {r} = {num(Fr(v*v,r))}"])
for n1, n2, v1 in [(1000, 500, 220), (200, 800, 110), (600, 300, 120)]:
    add(AF, "Alternatif Akım", 2, f"Primer sarım sayısı {n1}, sekonder sarım sayısı {n2} olan ideal transformatörün primerine {v1} V uygulanıyor. Sekonder gerilimi kaç V tur?", Fr(v1 * n2, n1), [Fr(v1 * n1, n2), v1, Fr(v1 * n2, n1) + 10, v1 + n2],
        "V_{1}/V_{2} = N_{1}/N_{2}", [f"V_{{2}} = {v1} · {n2} / {n1} = {num(Fr(v1*n2,n1))}"], " V")

# ---------- AYT KİMYA ----------
for c in (27, 127, 0, 77):
    add(AK, "Gazlar", 1, f"{c} °C kaç kelvindir?", c + 273, [c, c + 100, 273 - c, c + 373],
        "T(K) = t(°C) + 273", [f"{c} + 273 = {c+273}"], " K")
for n, v in [(4, 2), (6, 3), (1, 4), (3, 6)]:
    add(AK, "Sıvı Çözeltiler", 1, f"{v} L çözeltide {n} mol NaOH çözünmüştür. Çözeltinin molaritesi kaçtır?", Fr(n, v), [n * v, Fr(v, n), n + v, Fr(n, v) + 1],
        "M = n / V", [f"{n} / {v} = {num(Fr(n,v))}"], " M")
for m1, v1, v2 in [(2, 1, 4), (3, 2, 6), (4, 1, 2)]:
    add(AK, "Sıvı Çözeltiler", 2, f"{m1} M, {v1} L çözelti su eklenerek {v2} L ye seyreltiliyor. Son derişim kaç M olur?", Fr(m1 * v1, v2), [m1, m1 * v2, Fr(m1, v1 + v2), Fr(m1 * v1, v2) + 1],
        "M_{1}V_{1} = M_{2}V_{2}", [f"{m1} · {v1} = M · {v2}", f"M = {num(Fr(m1*v1,v2))}"], " M")
for e in (2, 3, 4, 5):
    add(AK, "Asit-Baz Dengesi", 1, f"[H^{{+}}] = 10^{{−{e}}} M olan çözeltinin 25 °C de pH değeri kaçtır?", e, [14 - e, -e, e + 1, 7],
        "pH = −log[H^{+}]", [f"pH = {e}"])
    add(AK, "Asit-Baz Dengesi", 2, f"25 °C de pH değeri {e} olan çözeltinin pOH değeri kaçtır?", 14 - e, [e, 7, 14 + e, 14 - e + 1],
        "pH + pOH = 14", [f"14 − {e} = {14-e}"])
for n, nm in [(1, "Metan"), (2, "Etan"), (3, "Propan"), (4, "Bütan")]:
    add(AK, "Organik Kimya", 1, f"{nm} molekülünde kaç hidrojen atomu bulunur?", 2 * n + 2, [2 * n, 2 * n - 2, n, 2 * n + 4],
        "Alkanların genel formülü C_{n}H_{2n+2}.", [f"n = {n}", f"2 · {n} + 2 = {2*n+2}"])
