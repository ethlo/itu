#!/usr/bin/env python3

import json

import matplotlib.pyplot as plt
import pandas as pd


def load_json(file_path):
    with open(file_path) as json_data:
        data = json.load(json_data)
        # pprint(data)
        impls = []
        test_methods = {}
        for e in data:
            benchmark = e.get("benchmark")
            primary = e.get('primaryMetric')
            score = primary.get('score')
            s = benchmark.rsplit(".", 1)
            impl = s[0].replace('Rfc3339ParserBenchmarkTest', '') \
                .replace('Rfc3339FormatterBenchmarkTest', '') \
                .rsplit(".", 1)[1]
            if impl not in impls:
                impls.append(impl)
            test_method = s[1]
            if test_method not in test_methods:
                test_methods[test_method] = {}
            test_methods[test_method][impl] = score
        return test_methods, impls


def render(dtf, style, target):
    plt.style.use(style)
    plot = dtf.plot(x=0,
                         kind='barh',
                         stacked=False,
                         title='Nanoseconds per operation (lower is better)',
                         figsize=(16, 16))

    for container in plot.containers:
        plot.bar_label(container)

    fig = plot.get_figure()
    fig.savefig(target)


def extract_data():
    (test_methods, impls) = load_json('../target/itu_performance.json')
    y = []
    for tm in test_methods:
        values = [tm]
        for name in impls:
            value = test_methods[tm].get(name, 0)
            values.append(int(value))
        y.append(values)
    impls.insert(0, 'Benchmark method')
    dtf = pd.DataFrame(y, columns=impls)
    return dtf


if __name__ == "__main__":
    dtf = extract_data()
    # for style in plt.style.available:
    render(dtf, 'seaborn', '../doc/performance.png')
