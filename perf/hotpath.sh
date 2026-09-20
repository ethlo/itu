#!/usr/bin/env bash
#
# hotpath.sh - which machine instructions a parse actually executes, with a sample count on each.
#
#   perf/hotpath.sh '5050-01-01T12:02:01.123Z' B          # -> perf/out/dis-B.txt
#   awk '$1 != "0.00%"' perf/out/dis-B.txt                # only the instructions that got samples
#
# A poor man's JMH perfasm for machines without hsdis: the forked JMH JVM prints the raw bytes of the
# C2-compiled parser (-XX:CompileCommand=print), perf samples it while it runs, and objdump disassembles
# the bytes. The listing shows every instruction of the nmethod with the share of samples it received;
# reading it top to bottom and skipping the 0.00% blocks (slow paths) gives the hot path, which is what
# the instruction counts from instr.sh are made of - bounds checks, spills, xmm<->gpr moves and all.
#
# Arguments: the input string (a JMH @Param value of the benchmark) and a tag for the output files.
# Optional: BENCH (JMH regex, default the char[] buffer path) and METHODS (comma-separated
# class::method list to print, default the two parseLenient entry points).
set -euo pipefail

ITU_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DTW_DIR="${DTW_DIR:-$ITU_DIR/../date-time-wars}"
OUT_DIR="${OUT_DIR:-$ITU_DIR/perf/out}"
mkdir -p "$OUT_DIR"; OUT_DIR="$(cd "$OUT_DIR" && pwd)"   # absolute: the scripts cd into date-time-wars
INPUT="$1"; TAG="$2"
BENCH="${BENCH:-candidates\\.itu_buffer.*}"
METHODS="${METHODS:-com.ethlo.time.ITU::parseLenient,com.ethlo.time.internal.fixed.ITUCharArrayParser::parseLenient}"

print_args=()
IFS=',' read -r -a methods <<<"$METHODS"
for m in "${methods[@]}"; do print_args+=("-XX:CompileCommand=print,$m"); done

cd "$DTW_DIR"
java -jar target/date-time-wars.jar "$BENCH" -p "dateString=$INPUT" -f 1 -wi 3 -w 2s -i 1 -r 15s \
    -jvmArgs "-XX:+UnlockDiagnosticVMOptions ${print_args[*]}" > "$OUT_DIR/asm-$TAG.log" 2>&1 &
jmh=$!
sleep 10                                     # past warmup, into the measurement iteration
pid=$(pgrep -n -f ForkedMain)                # the forked JVM, not the JMH host
perf record -q -e cycles:u -c 10000 -p "$pid" -o "$OUT_DIR/perf-$TAG.data" -- sleep 5 2>/dev/null || true
wait $jmh
perf script -i "$OUT_DIR/perf-$TAG.data" -F ip 2>/dev/null | awk '{print $1}' | sort | uniq -c | sort -rn > "$OUT_DIR/perf-$TAG.hist"

python3 - "$OUT_DIR" "$TAG" "$METHODS" <<'PY'
import sys, re, subprocess, collections
out, tag, methods = sys.argv[1], sys.argv[2], sys.argv[3].split(',')
names = [m.split('::')[0].split('.')[-1] + '::' + m.split('::')[1] for m in methods]
lines = open(f"{out}/asm-{tag}.log", errors='replace').read().split('\n')
hist = collections.Counter()
for l in open(f"{out}/perf-{tag}.hist"):
    c, ip = l.split()
    hist[int(ip, 16)] += int(c)
total = sum(hist.values()) or 1

# Every C2 nmethod dump of the requested methods: (samples inside it, header, base address, bytes)
blocks = []
for i, l in enumerate(lines):
    if 'Compiled method (c2)' in l and any(n in l for n in names):
        base = None; data = bytearray(); addr = None
        for m in lines[i + 1:]:
            if m.startswith('[/MachCode]'):
                break
            mm = re.match(r'\s*(0x[0-9a-f]+):\s+([0-9a-f |]+)$', m)
            if not mm:
                continue
            a = int(mm.group(1), 16); hexs = mm.group(2).replace('|', '').replace(' ', '')
            if not hexs:
                continue
            if base is None:
                base = a
            if addr is not None and a > addr:
                data.extend(b'\x90' * (a - addr))
            data.extend(bytes.fromhex(hexs)); addr = a + len(hexs) // 2
        if base is not None:
            n = sum(c for ip, c in hist.items() if base <= ip < base + len(data))
            blocks.append((n, l.strip(), base, bytes(data)))
blocks.sort(key=lambda b: -b[0])
for n, name, base, data in blocks:
    print(f"{n:8d} samples ({100.0 * n / total:5.1f}%)  {name}  base={base:#x} size={len(data)}")
n, name, base, data = blocks[0]
open(f"{out}/code-{tag}.bin", 'wb').write(data)
dis = subprocess.run(['objdump', '-D', '-b', 'binary', '-m', 'i386:x86-64', f'--adjust-vma={base:#x}', f"{out}/code-{tag}.bin"],
                     capture_output=True, text=True).stdout
rows = []
for l in dis.split('\n'):
    m = re.match(r'\s*([0-9a-f]+):\s+((?:[0-9a-f]{2} )+)\s*(.*)$', l)
    if m:
        ip = int(m.group(1), 16); c = hist.get(ip, 0)
        rows.append(f"{(100.0 * c / total if c else 0):6.2f}%  {ip:#x}: {m.group(3)}")
open(f"{out}/dis-{tag}.txt", 'w').write('\n'.join(rows) + '\n')
print(f"total samples {total}; wrote {out}/dis-{tag}.txt ({len(rows)} instructions)")
PY
grep "ns/op" "$OUT_DIR/asm-$TAG.log" | tail -1
