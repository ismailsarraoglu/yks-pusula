#!/bin/bash
# Kullanım: wait_hash.sh kisa-hash [deneme]
cd /home/claude/yks-pusula
for i in $(seq 1 ${2:-200}); do git fetch -q --depth 1 origin ci-results 2>/dev/null
if git show FETCH_HEAD:logs/commit.txt 2>/dev/null | grep -q "^$1"; then echo BITTI; git show FETCH_HEAD:logs/ozet.txt; exit 0; fi; sleep 20; done; echo ZAMAN ASIMI
