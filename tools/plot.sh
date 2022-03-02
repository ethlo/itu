#!/usr/bin/env bash

#pip install graph-cli==0.1.17

SCRIPT=$(readlink -f "$0")
SCRIPT_PATH=$(dirname "$SCRIPT")
INPUT_CSV_FILE=$(realpath "$SCRIPT_PATH/../target/benchmark.csv")

graph \
--barh \
--bar-label \
"$INPUT_CSV_FILE" \
--output "$SCRIPT_PATH/../doc/performance.png" \
--xlabel '' \
--ylabel '' \
--figsize 1600x1200 \
--title "Comparison of RFC-3339 parse/format performance" \
--fontsize 14 \
--bar-format="  %.0f"

#--xscale 100000000 \
#--text="200:400=thisasasdmasdkalskdaøsdlkaølsdkaølsdkaølsdkaølsdkalødkaølsdkasødlkaølsdkaøsldkaøslkdasødlkaølsdk"