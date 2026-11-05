#!/bin/bash
cd /home/claude/yks-pusula; H=$(git rev-parse --short HEAD)
for i in $(seq 1 ${1:-90}); do git fetch -q --depth 1 origin ci-results 2>/dev/null
if git show FETCH_HEAD:logs/commit.txt 2>/dev/null | grep -q "^$H"; then echo BITTI; git show FETCH_HEAD:logs/ozet.txt; git show FETCH_HEAD:logs/hatalar.txt | head -${2:-60}; exit 0; fi; sleep 20; done; echo ZAMAN ASIMI
