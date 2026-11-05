#!/bin/bash
# Kullanım: tools/push.sh "mesaj": değişiklikleri yükler ve derleme sonucunu bekler.
cd /home/claude/yks-pusula
git add -A
git commit -q -F - <<M
$1

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_0188gc9agb5EbwGab3qxyK9e
M
git push -q origin main 2>&1 | tail -1
git log --oneline -1
tools/wait_ci.sh ${2:-27} 90 | cut -c1-230
