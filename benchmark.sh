#!/bin/bash
mvn clean jmh:benchmark
python3 plot.py --include=parse -i target/itu_performance.json --theme tableau-colorblind10 --size=12,5 -o doc/parse.png
python3 plot.py --include=format -i target/itu_performance.json --theme tableau-colorblind10 --size=12,5 -o doc/format.png
