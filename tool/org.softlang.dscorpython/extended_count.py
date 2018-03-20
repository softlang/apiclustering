import pandas as pd
import matplotlib.pyplot as plt
import paths

if __name__ == '__main__':
    pd.set_option('display.max_columns', 100)
    pd.set_option('display.width', 1000)
    plt.rcParams["font.family"] = "consolas"
    plt.rcParams["font.size"] = 14

    # Read counts
    counts = pd.read_csv(paths.counts, encoding='ISO-8859-1')
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

    result.to_csv(paths.extended_counts)
