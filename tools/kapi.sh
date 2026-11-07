#!/bin/bash
# Kalite kapısı yardımcıları. Kullanım: source tools/kapi.sh <iş-adı>
# Her aşama ci/logs/durum-<iş>.tsv dosyasına "aşama<TAB>PASS|FAIL|NOT_RUN<TAB>açıklama" olarak yazılır.
# Betik sonunda kapi_bitir çağrılır: FAIL veya NOT_RUN varsa çıkış kodu 1 olur.
KAPI_IS=$1
PKG=app.ykspusula
mkdir -p ci/logs
DURUM=ci/logs/durum-$KAPI_IS.tsv
touch "$DURUM"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

# adb cihaz yokken sonsuza dek bekleyebilir; her çağrı süre sınırıyla çalışır.
adb() {
  local A="$ANDROID_HOME/platform-tools/adb"
  case "$1" in
    wait-for-device|emu|devices|start-server|kill-server) ;;
    *) timeout 20 "$A" get-state > /dev/null 2>&1 || { echo "adb: cihaz yok ($1)" >&2; return 1; } ;;
  esac
  timeout $(( ${TEST_SURESI:-1800} + 60 )) "$A" "$@"
}

kayit() {
  printf '%s\t%s\t%s\n' "$1" "$2" "$(echo "${3:-}" | tr '\t\r\n' '   ' | sed 's/ *$//' | cut -c1-300)" >> "$DURUM"
  echo "[$2] $KAPI_IS/$1 ${3:-}"
}

# Bir koşul doğruysa PASS, değilse FAIL yazar. kontrol <aşama> <açıklama> <komut...>
kontrol() {
  local a=$1 d=$2; shift 2
  if "$@"; then kayit "$a" PASS "$d"; return 0; else kayit "$a" FAIL "$d"; return 1; fi
}

# Aşamalar çalışamadıysa (ör. emülatör açılmadı) planlananlar NOT_RUN olarak yazılır.
calismadi() { local neden=$1; shift; for a in "$@"; do kayit "$a" NOT_RUN "$neden"; done; }

# Test çıktısı yarım kaldığında neden: uygulama çökmesi FAIL'dir. Yalnızca kanıtlı altyapı nedeninde bir kez yeniden denenir:
# - Google Play Hizmetleri ölürken veya sistem onu durdururken Android'in bağlı süreçleri kapatması
# - Sistem arayüzü gibi başka süreçler de yanıt vermezken açılışın tamamlanamaması (aşırı yüklü emülatör)
# - Android'in kendi sistem sürecinin (system_server) yerel çökmesi: emülatör sistemi yeniden başlar
altyapi_nedeni() {
  local log=$1
  if grep -q "system_server_native_crash" "$log" || grep -q "INSTRUMENTATION_ABORTED: System has crashed" "${log%.logcat}"; then
    timeout 300 adb wait-for-device; local j; for j in $(seq 1 60); do [ "$(timeout 15 adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ] && break; sleep 5; done
    sleep 20; echo "Android sistem süreci çöktü ve yeniden başladı (emülatör)"; return
  fi
  if grep -qE "Killing [0-9]+:$PKG[^ ]* .*depends on provider .* in dying proc" "$log"; then echo "Play Hizmetleri yeniden başladı (bağlı süreç kapatıldı)"; return; fi
  if grep -qE "Killing [0-9]+:$PKG[^ ]* .*stop com.google.android.gms due to" "$log"; then echo "Play Hizmetleri sistem tarafından durduruldu (bağlı süreç kapatıldı)"; return; fi
  if grep -q "failed to complete startup" "$log" && grep -E 'ANR in ' "$log" | grep -vq "$PKG"; then echo "aşırı yüklü emülatör (sistem süreçleri de yanıt vermedi)"; return; fi
}

# testi_calistir <aşama> <çıktı-dosyası> <sınıf|""> [am instrument ek argümanları...]  (sınıf boşsa filtre ek argümanlardan gelir)
testi_calistir() {
  local asama=$1 out=$2 cls=$3; shift 3
  local notu="" deneme
  for deneme in 1 2; do
    adb logcat -c 2>/dev/null
    timeout ${TEST_SURESI:-1800} adb shell am instrument -w ${cls:+-e class "$cls"} ${KAPI_DENEMESI:+-e kapi "$KAPI_DENEMESI"} "$@" $PKG.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | tr -d "\r" | tail -400 > "$out"
    if tail -6 "$out" | grep -q 'OK (' && ! grep -q 'FAILURES!!!' "$out"; then
      kayit "$asama" PASS "$(tail -6 "$out" | grep -o 'OK ([0-9]* tests\?)')${notu:+; $notu}"; return 0
    fi
    if grep -q 'FAILURES!!!' "$out"; then
      # Hata ayrıntısı iş loguna da yazılır (artifact indirilemese bile okunabilsin).
      echo "--- $asama hata ayrıntısı ---"; grep -A25 -m3 -E '^[0-9]+\) ' "$out" | cut -c1-400 | head -80
      grep -E 'Caused by|Exception:|Error:|at app\.ykspusula' "$out" | grep -v 'testThread=' | cut -c1-400 | head -25
      kayit "$asama" FAIL "$(grep -E 'Tests run:' "$out" | tail -1) $(grep -m2 -E '^[0-9]+\) ' "$out" | tr '\n' ' ')${notu:+; $notu}"; return 1
    fi
    adb logcat -d > "$out.logcat" 2>/dev/null
    if grep -A3 'FATAL EXCEPTION' "$out.logcat" | grep -q "Process: $PKG"; then kayit "$asama" FAIL "uygulama çöktü${notu:+; $notu}"; return 1; fi
    local neden; neden=$(altyapi_nedeni "$out.logcat")
    if [ -z "$neden" ] || [ "$deneme" = 2 ]; then kayit "$asama" FAIL "test tamamlanmadı ${neden:+($neden)} $(tail -2 "$out" | tr '\n' ' ')${notu:+; $notu}"; return 1; fi
    notu="1. deneme kesildi: $neden"
    cp "$out" "$out.ilk"; cp "$out.logcat" "$out.ilk.logcat"
    sleep 30
  done
}

kapi_bitir() {
  echo "--- $KAPI_IS durum ---"; cat "$DURUM"
  if [ ! -s "$DURUM" ]; then echo "durum dosyası boş"; return 1; fi
  if awk -F'\t' '$2!="PASS"{bad=1} END{exit bad?0:1}' "$DURUM"; then return 1; fi
  return 0
}

# Test ve uygulama APK'larını kurar; ikisi de kurulursa 0 döner. Sonuç test-kurulum aşamasına yazılır.
apk_kur() {
  local k1 k2
  k1=$(adb install -r -g app/build/outputs/apk/debug/app-debug.apk 2>&1 | tail -1 | tr -d '\r')
  k2=$(adb install -r -g app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk 2>&1 | tail -1 | tr -d '\r')
  kontrol test-kurulum "$k1 / $k2" [ "$k1" = "Success" -a "$k2" = "Success" ]
}

# Emülatör kurup açar. emulator_ac <ad> <imaj> <bellek-MB>; açılırsa 0 döner.
emulator_ac() {
  local NAME=$1 IMG=$2 MEM=${3:-3072}
  export ANDROID_AVD_HOME="$HOME/.android/avd"
  SDKBIN="$ANDROID_HOME/cmdline-tools/latest/bin"
  mkdir -p "$ANDROID_AVD_HOME"
  yes | "$SDKBIN/sdkmanager" --licenses > /dev/null 2>&1
  "$SDKBIN/sdkmanager" "$IMG" "emulator" "platform-tools" 2>&1 | tr '\r' '\n' | tail -3 > "ci/logs/kurulum-$NAME.txt"
  echo no | "$SDKBIN/avdmanager" create avd -n "$NAME" -k "$IMG" -d pixel_6 --force >> "ci/logs/kurulum-$NAME.txt" 2>&1
  [ -f "$ANDROID_AVD_HOME/$NAME.avd/config.ini" ] || { echo "sanal cihaz oluşmadı" >> "ci/logs/kurulum-$NAME.txt"; return 1; }
  printf 'hw.ramSize=%s\ndisk.dataPartition.size=6144M\nhw.keyboard=yes\n' "$MEM" >> "$ANDROID_AVD_HOME/$NAME.avd/config.ini"
  EMU_NAME=$NAME; EMU_MEM=$MEM
  emulator_baslat_bekle
  local B=$EMU_BOOT
  [ "$B" = "1" ] || { tail -40 "ci/logs/emulator-$NAME.txt" > "ci/logs/emulator-son-$NAME.txt"; return 1; }
  adb shell settings put global window_animation_scale 0
  adb shell settings put global transition_animation_scale 0
  adb shell settings put global animator_duration_scale 0
  adb shell svc power stayon true
  adb shell settings put global hide_error_dialogs 1
  adb shell input keyevent KEYCODE_WAKEUP; adb shell wm dismiss-keyguard
  echo "android=$(adb shell getprop ro.build.version.release | tr -d '\r') api=$(adb shell getprop ro.build.version.sdk | tr -d '\r') imaj=$IMG" >> "ci/logs/kurulum-$NAME.txt"
  return 0
}

# EMU_NAME sanal cihazını başlatır ve açılmasını bekler; EMU_BOOT=1 açıldı demektir.
emulator_baslat_bekle() {
  "$ANDROID_HOME/emulator/emulator" -avd "$EMU_NAME" -accel on -cores 3 -memory "$EMU_MEM" -no-metrics -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -no-snapshot -camera-back none -camera-front none >> "ci/logs/emulator-$EMU_NAME.txt" 2>&1 &
  EMU_PID=$!
  EMU_BOOT=""
  local i
  for i in $(seq 1 200); do
    EMU_BOOT=$(timeout 15 adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r'); [ "$EMU_BOOT" = "1" ] && break
    kill -0 $EMU_PID 2>/dev/null || { echo "emülatör kapandı (deneme $i)" >> "ci/logs/kurulum-$EMU_NAME.txt"; break; }
    sleep 5
  done
}

# Yeniden açılıştan sonra test ortamı ayarlarını tekrar uygular.
emulator_baslat() {
  emulator_baslat_bekle
  [ "$EMU_BOOT" = "1" ] || return 1
  adb shell settings put global window_animation_scale 0; adb shell settings put global transition_animation_scale 0
  adb shell settings put global animator_duration_scale 0; adb shell settings put global hide_error_dialogs 1
  return 0
}

emulator_kapat() {
  timeout 20 "$ANDROID_HOME/platform-tools/adb" emu kill > /dev/null 2>&1; sleep 5
  pkill -f qemu-system; sleep 2
}

cevrimdisi() { adb shell svc wifi disable; adb shell svc data disable; adb shell cmd connectivity airplane-mode enable 2>/dev/null; sleep 6; }
cevrimici() { adb shell cmd connectivity airplane-mode disable; adb shell svc wifi enable; adb shell svc data enable; sleep 25; }

# Cihazı yeniden başlatır. Emülatör yeniden başlatmadan geri gelmezse (bilinen emülatör sorunu) aynı sanal cihaz
# kapatılıp yeniden açılır; veri bölümü korunur, yani bu da bir yeniden başlatmadır. Durum YENIDEN_NOT değişkenine yazılır.
yeniden_baslat() {
  YENIDEN_NOT=""
  adb reboot; sleep 20; timeout 300 adb wait-for-device
  local i B=""
  for i in $(seq 1 100); do B=$(timeout 15 adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r'); [ "$B" = "1" ] && break; sleep 3; done
  if [ "$B" != "1" ]; then
    # Emülatör çoğu zaman açıktır ama adb bağlantısı "offline" kalır; önce bağlantı yenilenir.
    "$ANDROID_HOME/platform-tools/adb" kill-server; sleep 3; "$ANDROID_HOME/platform-tools/adb" start-server; timeout 300 adb wait-for-device
    for i in $(seq 1 60); do B=$(timeout 15 adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r'); [ "$B" = "1" ] && break; sleep 5; done
    [ "$B" = "1" ] && echo "yeniden başlatma: adb bağlantısı yenilendi" >> "ci/logs/olcum-$KAPI_IS.txt"
  fi
  if [ "$B" != "1" ] && [ -n "$EMU_NAME" ]; then
    # Son çare: emülatör kapatılıp açılır. Bu bir yeniden başlatma değil, ani güç kesilmesidir; kalıcılık sırası baştan tekrarlanır.
    YENIDEN_NOT="emülatör yeniden başlatmadan geri gelmedi; kapatılıp yeniden açıldı"
    echo "$YENIDEN_NOT" | tee -a "ci/logs/olcum-$KAPI_IS.txt"
    emulator_kapat
    emulator_baslat || YENIDEN_NOT="$YENIDEN_NOT; ikinci açılış da başarısız"
  fi
  sleep 60
  adb shell svc power stayon true; adb shell input keyevent KEYCODE_WAKEUP; adb shell wm dismiss-keyguard
}

# Kalıcılık: yaz, zorla durdur, oku, cihazı yeniden başlat, oku.
kalicilik() {
  local P=$PKG.PersistenceTest tur not=""
  for tur in 1 2; do
    testi_calistir kalicilik-yaz ci/logs/kalicilik-yaz-$KAPI_IS.txt "$P#writeData" || { calismadi "veri yazılamadı" kalicilik-zorla-durdurma kalicilik-yeniden-baslatma; return 1; }
    adb shell am force-stop $PKG
    testi_calistir kalicilik-zorla-durdurma ci/logs/kalicilik-durdurma-$KAPI_IS.txt "$P#readData"
    yeniden_baslat
    cevrimdisi
    # Emülatör yeniden başlatmadan dönmeyip zorla kapatıldıysa bu tur gerçek bir yeniden başlatma sayılmaz:
    # kayıtları atılır ve sıra bir kez baştan çalışır. Not sonuca yazılır.
    if [ -n "$YENIDEN_NOT" ] && [ "$tur" = 1 ]; then
      not="1. turda $YENIDEN_NOT, sıra tekrarlandı"
      sed -i '/^kalicilik-yaz\tPASS\t/d; /^kalicilik-zorla-durdurma\tPASS\t/d' "$DURUM"
      continue
    fi
    break
  done
  testi_calistir kalicilik-yeniden-baslatma ci/logs/kalicilik-yeniden-$KAPI_IS.txt "$P#readData"
  local k=$?
  [ -n "$not$YENIDEN_NOT" ] && sed -i "\$ s/\$/; ${not}${YENIDEN_NOT:+; 2. tur: $YENIDEN_NOT}/" "$DURUM"
  return $k
}

# Yan yükleme (release ayarlı) APK: kurulum, açılış, uygulama çökmesi ve ANR yok.
release_dene() {
  adb uninstall $PKG.test > /dev/null 2>&1; adb uninstall $PKG > /dev/null 2>&1
  local k; k=$(adb install -r app/build/outputs/apk/sideload/app-sideload.apk 2>&1 | tail -1 | tr -d '\r')
  kontrol release-kurulum "$k" [ "$k" = "Success" ] || return 1
  adb logcat -c
  local n t
  for n in 1 2 3; do
    adb shell am force-stop $PKG; sleep 2
    t=$(adb shell am start -W -n $PKG/.MainActivity 2>&1 | tr -d '\r' | grep -E 'TotalTime' | grep -oE '[0-9]+')
    echo "soguk-acilis-$n=${t:-yok}" >> ci/logs/olcum-$KAPI_IS.txt
    sleep 6
  done
  sleep 8
  local pid; pid=$(adb shell pidof $PKG | tr -d '\r')
  adb exec-out screencap -p > "ci/screens/release-$KAPI_IS.png" 2>/dev/null
  adb logcat -d > "ci/logs/release-logcat-$KAPI_IS.txt" 2>/dev/null
  local crash anr
  crash=$(grep -A3 'FATAL EXCEPTION' "ci/logs/release-logcat-$KAPI_IS.txt" | grep -c "Process: $PKG")
  anr=$(grep -c "ANR in $PKG" "ci/logs/release-logcat-$KAPI_IS.txt")
  kontrol release-cokme "uygulama çökmesi: $crash" [ "$crash" = "0" ]
  kontrol release-anr "ANR: $anr" [ "$anr" = "0" ]
  if [ -n "$pid" ]; then kayit release-acik-kaldi PASS "süreç $pid"
  else
    local neden; neden=$(altyapi_nedeni "ci/logs/release-logcat-$KAPI_IS.txt")
    if [ -n "$neden" ]; then
      # Kanıtlı altyapı nedeni: bir kez yeniden açılır, sonuç nedeniyle birlikte yazılır.
      adb shell am start -W -n $PKG/.MainActivity > /dev/null 2>&1; sleep 15
      pid=$(adb shell pidof $PKG | tr -d '\r')
      if [ -n "$pid" ]; then kayit release-acik-kaldi PASS "1. açılışta $neden; yeniden açılışta süreç ayakta"; else kayit release-acik-kaldi FAIL "süreç yok ($neden)"; fi
    else
      kayit release-acik-kaldi FAIL "süreç yok, altyapı nedeni bulunamadı"
    fi
  fi
  adb shell dumpsys meminfo $PKG 2>/dev/null | grep -E 'TOTAL PSS|TOTAL:' | head -1 | tr -d '\r' >> ci/logs/olcum-$KAPI_IS.txt
}
