#!/bin/bash
python3 plot.py --include=parse -i target/itu_performance.json --theme theme.mplstyle --size=12,4 -o doc/parse.png
python3 plot.py --include=format -i target/itu_performance.json --theme theme.mplstyle --size=12,4 -o doc/format.png
python3 plot.py --include=raw -i target/itu_performance.json --theme theme.mplstyle --size=12,4 -o doc/parse_raw.png
