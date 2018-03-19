import pandas as pd
import numpy as np
import math
import matplotlib.pyplot as plt

if __name__ == '__main__':
    pd.set_option('display.max_columns', 100)
    pd.set_option('display.width', 1000)
    plt.rcParams["font.family"] = "consolas"
    plt.rcParams["font.size"] = 14

    counts_file = 'C:/Data/Corpus/APIBaseline/Counts.csv'
    extended_count = 'C:/Data/Corpus/APIBaseline/ExtendedCount.csv'

    # Read counts
    counts = pd.read_csv(counts_file, encoding='ISO-8859-1')
    # Filter counts
    counts = counts[counts.apply(
        lambda x: isinstance(x['groupId'], str) and isinstance(x['artifactId'], str) and isinstance(x['version'], str),
        axis=1)]

    # Aggregating counts
    frequentapi = counts.groupby(['groupId', 'artifactId']).sum().reset_index()
    frequentapi = frequentapi.sort_values(by='count', ascending=False)
    frequentapi['api_usage_rank'] = range(len(frequentapi))

    # frequentapi = frequentapi.sort_values(by='count', ascending=False)

    # frequentapi.to_csv(frequentapi_file)

    print(frequentapi)

    result = pd.merge(counts, frequentapi, on=['groupId', 'artifactId'], how='left').rename(
        columns={'count_x': 'version_usage', 'count_y': 'api_usage'})
    result = result.sort_values(by=['api_usage', 'version_usage'], ascending=False)

    print(result)
    result.to_csv(extended_count)


    # counts_file = 'C:/Data/Corpus/APIBaseline/Counts.csv'
    # frequentapi_file = 'C:/Data/Corpus/APIBaseline/FrequentApi.csv'
    #
    # # Read counts
    # counts = pd.read_csv(counts_file, encoding='ISO-8859-1')
    #
    # # Dropping column version.
    # counts.drop(['version'], axis=1, inplace=True)
    #
    # # Aggregating counts
    # frequentapi = counts.groupby(['groupId', 'artifactId']).sum()
    # frequentapi = frequentapi.sort_values(by='count', ascending=False)
    #
    # print(frequentapi)
    # frequentapi.to_csv(frequentapi_file)
