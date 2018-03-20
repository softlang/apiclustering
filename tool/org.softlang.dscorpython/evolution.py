import pandas as pd
import numpy as np
import math
import paths
import matplotlib.pyplot as plt
import os
import glob
import matplotlib.gridspec as grd
from scipy.cluster.hierarchy import dendrogram
from scipy.cluster.hierarchy import fcluster

if __name__ == '__main__':
    pd.set_option('display.max_columns', 100)
    pd.set_option('display.width', 1000)
    plt.rcParams["font.family"] = "consolas"
    plt.rcParams["font.size"] = 14
    plt.rcParams['figure.figsize'] = 16, 6

    # TODO: Extract paths.
    # api_evolution_file = 'C:/Data/Corpus/APIBaseline/Delta2.csv'
    # figure_folder = 'C:/Data/Corpus/APIBaseline/figures2'
    # final_file = 'C:/Data/Corpus/APIBaseline/HaertelAL18.csv'
    # first_final_file = 'C:/Data/Corpus/APIBaseline/FirstFinal.csv'


    def save_figure(fig, name):
        fig.savefig(paths.evolution_folder + "/" + name + ".png")


    def read_linkage(text):
        link = []
        if not (text == '' or text == 'nan'):
            for x in text.split(' '):
                left_index = int(x.split(';')[0])
                right_index = int(x.split(';')[1])
                similarity = float(x.split(';')[2])
                size = int(x.split(';')[3])
                link.append([left_index, right_index, similarity, size])
        return link


    # Clean figure output folder.
    for root, dirs, files in os.walk(paths.evolution_folder):
        for f in files:
            os.unlink(os.path.join(root, f))

    data = pd.read_csv(paths.version_delta)
    data['api'] = data.apply(lambda x: x['groupId'] + '-' + x['artifactId'], axis=1)
    data['coordinates'] = data.apply(lambda x: x['groupId'] + ':' + x['artifactId'] + ':' + x['version'], axis=1)

    # Add the usage ranks: 'vesion_usage_rank'
    data.sort_values(by='version_usage', ascending=False, inplace=True)
    data['version_usage_rank'] = range(0, len(data))


    # Assigning important api specific measures.
    def extend_api(api):
        # Add cluster column, single cluster if no linkage (sorting according to version_index is required).

        api = api.sort_values(by='version_index', ascending=True)
        link = read_linkage(str(api['linkage'].iloc[0]))
        if not link == []:
            api['cluster'] = fcluster(link, 0.3, criterion='distance')
        else:
            api['cluster'] = 1

        # Add the 'cluster_usage_rank_in_api'
        cluster_usage = api.groupby('cluster')['version_usage'].sum().to_frame('cluster_usage')
        cluster_usage = cluster_usage.sort_values(by='cluster_usage', ascending=False)
        cluster_usage['cluster_usage_rank_in_api'] = range(0, len(cluster_usage))

        api = pd.merge(api, cluster_usage, left_on='cluster', right_index=True)

        # Assigning important cluster specific measures.
        def extend_cluster(cluster):
            cluster.sort_values(by='version_usage', ascending=False, inplace=True)
            cluster['version_usage_rank_in_cluster'] = range(0, len(cluster))
            return cluster

        api = api.groupby('cluster').apply(extend_cluster)
        api['number_api_clusters'] = len(api.groupby('cluster'))
        api['number_api_versions'] = len(api)

        # Add 'version_usage_rank_in_api'
        api = api.sort_values(by='version_usage', ascending=False)
        api.version_usage_rank_in_api = range(0, len(api))

        api = api.reset_index(drop=True)

        return api


    # Load and filter data.

    data = data.groupby(['api']).apply(lambda x: extend_api(x))

    # Now crazy rating function (Improve that tomorrow).
    number_api = 93
    number_clusters = 10000
    data['rating'] = data.apply(lambda x: (float(
        x.api_usage_rank * 1 + x.cluster_usage_rank_in_api * number_api + x.version_usage_rank_in_cluster * number_clusters)),
                                axis=1)
    data = data.sort_values(by='rating', ascending=True)
    data['rank'] = range(0, len(data))

    # Save csv as final data.
    data.to_csv(paths.haertelAL18, index= False)

    # Plot the data and add plotting specific columns that are not saved.
    for (id, api) in data.groupby(['api']):
        # Maximals and counts.
        version_count = len(api)
        category = str(api['category'].iloc[0])
        groupId = str(api['groupId'].iloc[0])
        artifactId = str(api['artifactId'].iloc[0])
        api_usage = int(api['api_usage'].iloc[0])
        max_vesions_usage = api['version_usage'].max()
        max_size = api['size'].max()

        # Sorting according to versions.
        api.sort_values(by='version_index', ascending=True, inplace=True)

        # Make some numbers relative for plotting.
        api['version_usage_r'] = api.apply(lambda x: x['version_usage'] / max_vesions_usage, axis=1)
        api['size_r'] = api.apply(lambda x: x['size'] / max_size, axis=1)

        # Marking to versions.
        cut_rank = 300
        api['top_version_usage_r'] = api.apply(lambda x: x.version_usage_r if x['rank'] <= cut_rank else np.NaN, axis=1)

        # Start plotting magic.
        plt.close('all')

        fig, (ax0, ax1) = plt.subplots(nrows=2)
        fig.suptitle(groupId + ":" + artifactId + " [" + category + "] " + str(api_usage) + " usages", fontsize=20)

        # Draw dendrogram if possible.
        link = read_linkage(str(api['linkage'].iloc[0]))
        if not link == []:
            dendrogram(
                link,
                link_color_func=lambda x: "k",
                ax=ax0,
                no_labels=True,
                leaf_rotation=90.,  # rotates the x axis labels
                leaf_font_size=8.,  # font size for the x axis labels
            )

        # Set limits after plotting
        ax0.set_xlim(0, (len(api) * 10))

        # Setting plot labels
        api['label'] = api.apply(lambda x: x.version + " (" + str(x.cluster) + ")", axis=1)

        api['Add'] = api['add']
        api['Remove'] = api['remove']
        api['Usage'] = api['version_usage_r']
        api['Size'] = api['size_r']
        api['Top'] = api['top_version_usage_r']

        # Draw line-chart.
        api.plot(kind='line', ax=ax1, linewidth=2.0, style=['-k', '--r', '-.b', ':y'],
                 x=['label'],
                 y=['Add', 'Remove', 'Usage',
                    'Size'])

        api.plot(kind='line', ax=ax1, linewidth=2.0, markersize=15, style=['b*'],
                 x=['label'],
                 y=['Top'])

        ax1.set_xlim(-0.5, (len(api) - 0.5))
        # ax1.xaxis.label.set_visible(False)
        # ax1.legend_.remove()
        fig.subplots_adjust(bottom=0.3, left=0.05, right=0.95, top=0.90)
        plt.xticks(rotation='vertical')
        plt.xticks(np.arange(version_count), api.label)
        save_figure(fig, id)
