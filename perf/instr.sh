#!/usr/bin/env bash
#
# instr.sh - instructions, branches and branch-misses per parse, from hardware counters.
#
#   perf/instr.sh                        # rebuild itu + date-time-wars, measure the char[] buffer path
#   perf/instr.sh 'candidates\.itu\..*'  # any JMH benchmark regex from date-time-wars
#   NO_BUILD=1 perf/instr.sh             # skip the rebuild
#
# Why this and not ns/op: on a laptop the timing noise (±2-3%) is the size of the effects worth chasing on
# a 10-20 ns parse, while instructions/op is deterministic to about 1% (the residual is the JIT making
# different inlining/allocation choices between runs, so a suspicious jump is worth a second run).
# Elapsed time still decides in the end - confirm a kept series with date-time-wars' bench.sh --thorough.
#
# Needs: perf (linux-tools) with kernel.perf_event_paranoid <= 2, and the date-time-wars checkout next to
# this repo (or DTW_DIR pointing at it). Counters are reported for the P-cores (cpu_core) on hybrid Intel.
set -euo pipefail

ITU_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DTW_DIR="${DTW_DIR:-$ITU_DIR/../date-time-wars}"
OUT_DIR="${OUT_DIR:-$ITU_DIR/perf/out}"
mkdir -p "$OUT_DIR"; OUT_DIR="$(cd "$OUT_DIR" && pwd)"   # absolute: the scripts cd into date-time-wars
PATTERN="${1:-candidates\\.itu_buffer.*}"

if [[ "${NO_BUILD:-0}" != 1 ]]; then
    (cd "$ITU_DIR" && mvn -q -ntp -DskipTests clean install)
    (cd "$DTW_DIR" && mvn -q -ntp -DskipTests clean package)
fi

cd "$DTW_DIR"
java -jar target/date-time-wars.jar "$PATTERN" -f 1 -wi 3 -w 1s -i 3 -r 1s -prof perfnorm > "$OUT_DIR/instr-last.log" 2>&1

python3 - "$OUT_DIR/instr-last.log" <<'PY'
import re, sys, collections
rows = collections.OrderedDict()
for line in open(sys.argv[1]):
    # "<Class>.<method>[:cpu_core/<counter>/]  <param>  avgt [cnt] <score> ..." - the counter rows have no cnt column
    m = re.match(r'(\S+?)(?::(?:cpu_core/)?([\w-]+)/?)?\s{2,}(\S+)\s+avgt\s+(?:\d+\s+)?([\d.]+)', line)
    if m and not m.group(1).startswith('#'):
        rows.setdefault((m.group(1), m.group(3)), {})[m.group(2) or 'ns'] = float(m.group(4))
for (bench, param), r in rows.items():
    if 'instructions' not in r:
        continue
    print(f"{bench.split('.')[-2]:32s} {param:38s} {r['instructions']:7.1f} instr {r['branches']:6.1f} br "
          f"{r.get('branch-misses', 0):.3f} miss {r['ns']:6.2f} ns")
PY
