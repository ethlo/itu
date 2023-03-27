#!/usr/bin/env python3

import argparse
import json

import matplotlib.pyplot as plt
import pandas as pd
import sys


class ReportGenerator(object):

    def __init__(self):

        self.settings = {}
        self.parser = argparse.ArgumentParser(description='ReportGenerator')
        self.parser.add_argument('-i', default='target/itu_performance.json', help='The JMH result (JSON) file')
        self.parser.add_argument('-o', default='output.png', help='Output file path for bar chart image')
        self.parser.add_argument('--size', default='10,16', help='Plot size')
        self.parser.add_argument('--theme', default='default', help='Output theme for bar chart image')
        self.parser.add_argument('--include', default='parse,parseRaw,format',
                                 help='Prefix for test methods to include')

        if not len(sys.argv) > 1:
            self.parser.print_help()
            sys.exit(1)

        self.args = vars(self.parser.parse_args())
        self.source_path = self.args['i']
        self.include_methods = tuple(x.strip() for x in self.args['include'].split(','))
        self.theme = self.args['theme']
        self.fig_size = tuple(int(x.strip()) for x in self.args['size'].split(','))
        data = self.extract_data()
        self.render(data, self.args['o'])

    def render(self, dtf, target):
        plt.style.use(self.theme)
        plot = dtf.plot(x=0,
                        kind='barh',
                        stacked=False,
                        title='Nanoseconds per operation (lower is better)',
                        figsize=self.fig_size)

        for container in plot.containers:
            plot.bar_label(container)

        fig = plot.get_figure()
        fig.savefig(target)

    def extract_data(self):
        (test_methods, impls) = self.load_json()
        y = []
        for tm in test_methods:
            if tm.startswith(self.include_methods):
                values = [tm]
                for name in impls:
                    value = test_methods[tm].get(name, 0)
                    values.append(int(value))
                y.append(values)
        impls.insert(0, 'Benchmark method')
        return pd.DataFrame(y, columns=impls)

    def load_json(self):
        with open(self.source_path) as json_data:
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
                    .replace('LenientParserBenchmarkTest', '') \
                    .rsplit(".", 1)[1]
                if impl not in impls:
                    impls.append(impl)
                test_method = s[1]
                if test_method not in test_methods:
                    test_methods[test_method] = {}
                test_methods[test_method][impl] = score
            return test_methods, impls


if __name__ == "__main__":
    # for style in plt.style.available:
    ReportGenerator()
