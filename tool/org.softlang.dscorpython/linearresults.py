import pandas as pd
import numpy as np
import math
import matplotlib.pyplot as plt
import seaborn as sns
import paths
from scipy.cluster.hierarchy import dendrogram

if __name__ == '__main__':
    pd.set_option('display.max_columns', 80)
    pd.set_option('display.width', 1000)
    plt.rcParams["font.family"] = "consolas"
    plt.rcParams["font.size"] = 14

    results_file = paths.results
    linear_results_file = paths.flat_results

    # Read results
    results = pd.read_csv(results_file, encoding='ISO-8859-1')
    # results = results[results.source == 'HaertelAL18']
    print(results)

    regular_property = [x for x in results.columns if not str(x).startswith("#")]
    # Remove since linkage contains to many information.
    regular_property.remove('linkage')
    regular_property.remove('apis')

    xs = []
    for p in [x for x in results.columns if str(x).startswith("#corr_")]:
        x = results[regular_property].copy()
        x['correlation'] = results.apply(lambda x: x[p], axis=1)
        x['property'] = p.split('_')[1]
        x['step'] = p.split('_')[2]
        xs.append(x)

    data = pd.concat(xs)

    data.to_csv(linear_results_file, index=False)
