import pandas as pd
import math
import matplotlib.pyplot as plt
import seaborn as sns
import os
import paths
from scipy.cluster.hierarchy import dendrogram

if __name__ == '__main__':
    pd.set_option('display.max_columns', 80)
    pd.set_option('display.width', 1000)
    plt.rcParams["font.family"] = "consolas"
    plt.rcParams["font.size"] = 14
    plt.rcParams['figure.figsize'] = 8, 8

    # Clean figure output folder.
    for root, dirs, files in os.walk(paths.evaluation_folder):
        for f in files:
            os.unlink(os.path.join(root, f))


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


    def save_figure(fig, name):
        fig.savefig(paths.evaluation_folder + "/" + name + ".png", dpi=100)


    # Read data
    final = pd.read_csv(paths.haertelAL18, encoding='ISO-8859-1')
    raw = pd.read_csv(paths.flat_results, encoding='ISO-8859-1')
    raw = raw.dropna()
    configurations = pd.read_csv(paths.results, encoding='ISO-8859-1')

    # API affiliation
    configurations['api_max_correlation'] = configurations[
        [x for x in configurations.columns if str(x).startswith("#corr_" + "api")]].apply(
        lambda x: x.max(), axis=1)
    configurations['api_mean_correlation'] = configurations[
        [x for x in configurations.columns if str(x).startswith("#corr_" + "api")]].apply(
        lambda x: x.mean(), axis=1)
    # Domains.
    configurations['domains_max_correlation'] = configurations[
        [x for x in configurations.columns if str(x).startswith("#corr_" + "domains")]].apply(
        lambda x: x.max(), axis=1)
    configurations['domains_mean_correlation'] = configurations[
        [x for x in configurations.columns if str(x).startswith("#corr_" + "domains")]].apply(
        lambda x: x.mean(), axis=1)
    # Categories.
    configurations['category_max_correlation'] = configurations[
        [x for x in configurations.columns if str(x).startswith("#corr_" + "category")]].apply(
        lambda x: x.max(), axis=1)
    configurations['category_mean_correlation'] = configurations[
        [x for x in configurations.columns if str(x).startswith("#corr_" + "category")]].apply(
        lambda x: x.mean(), axis=1)
    # Tags.
    configurations['tags_max_correlation'] = configurations[
        [x for x in configurations.columns if str(x).startswith("#corr_" + "tags")]].apply(
        lambda x: x.max(), axis=1)
    configurations['tags_mean_correlation'] = configurations[
        [x for x in configurations.columns if str(x).startswith("#corr_" + "tags")]].apply(
        lambda x: x.mean(), axis=1)

    configurations = configurations.drop([x for x in configurations.columns if str(x).startswith("#corr_")], axis=1)

    features = ['source', 'analytical', 'similarity', 'normalization', 'featureSelection', 'granularity', 'sampling',
                'clustering']

    # Ceaning names
    for feature in features:
        raw[feature] = raw[feature].apply(
            lambda x: x[len(feature):] if x.lower().startswith(feature.lower()) else x)
        configurations[feature] = configurations[feature].apply(
            lambda x: x[len(feature):] if x.lower().startswith(feature.lower()) else x)

    for feature in features:
        plt.close('all')

        # Selecting palette
        unique = raw[feature].unique()
        palette = dict(zip(unique, sns.color_palette("cubehelix", len(unique))))

        fig, ((ax0, ax1), (ax2, ax3)) = plt.subplots(nrows=2, ncols=2, sharex=True, sharey=True)

        ax0.set_title('Domains')
        ax1.set_title('Tags')
        ax2.set_title('Categories')
        ax3.set_title('API Affiliation')

        ax0.xaxis.label.set_visible(False)
        ax1.xaxis.label.set_visible(False)

        ax3.yaxis.label.set_visible(False)
        ax1.yaxis.label.set_visible(False)

        ci = 100
        marker = 'o'
        sns.tsplot(data=raw[(raw.sampling != 'API') & (raw['property'] == 'domains')], legend=True, ax=ax0,
                   time='step',
                   ci=ci,
                   color=palette,
                   unit='configuration_index', condition=feature,
                   value='correlation', )

        sns.tsplot(data=raw[(raw.sampling != 'API') & (raw['property'] == 'tags')], ax=ax1, legend=False,
                   time='step',
                   ci=ci,
                   color=palette,
                   unit='configuration_index', condition=feature,
                   value='correlation', )

        sns.tsplot(data=raw[(raw.sampling != 'API') & (raw['property'] == 'category')], ax=ax2, legend=False,
                   time='step', color=palette,
                   ci=ci,
                   unit='configuration_index', condition=feature,
                   value='correlation', )

        sns.tsplot(data=raw[(raw.sampling != 'API') & (raw['property'] == 'api')], ax=ax3, legend=False,
                   time='step',
                   color=palette,
                   ci=ci,
                   unit='configuration_index', condition=feature,
                   value='correlation', )

        save_figure(fig, "ts_" + feature)

    # Create top feature configurations.
    configurations_without_sampling = configurations[
        (configurations['sampling'] == 'None') & (configurations['source'] == 'HaertelAL18')]

    top_feature = []
    for feature in features:
        for alternative in configurations_without_sampling[feature].unique():
            top_feature = top_feature + [
                configurations_without_sampling[configurations_without_sampling[feature] == alternative].sort_values(
                    by='category_max_correlation',
                    ascending=False).head(1)]

    top_feature = pd.concat(top_feature)
    top_feature = top_feature.drop_duplicates()
    top_feature = top_feature.sort_values(by='category_max_correlation', ascending=False)

    # Now plot this top feature dendrograms:
    for index, row in top_feature.iterrows():
        plt.close('all')
        fig, ((ax0)) = plt.subplots(nrows=1, ncols=1, figsize=(15, 100))
        linkage = [[x[0], x[1], max(0.0, x[2]), x[3]] for x in reversed(read_linkage(str(row['linkage'])))]

        apis = pd.merge(pd.DataFrame({'coordinates': row['apis'].split(";")}), final, how='left', on=['coordinates'])

        apis['label'] = apis.apply(lambda x: '[' + str(x.category) + '] ' + x.coordinates, axis=1)

        # Add metadata again

        dendrogram(
            linkage,
            orientation='left',
            ax=ax0,
            labels=apis['label'].tolist(),
            # leaf_rotation=90.,  # rotates the x axis labels
            leaf_font_size=16.,  # font size for the x axis labels
        )
        # plt.xticks(rotation='vertical')
        plt.subplots_adjust(bottom=0.05, left=0.05, right=0.30, top=0.95)
        save_figure(fig, "top_" + str(row.configuration_index))

    # Save top features!

    top_feature = top_feature[
        features + ['category_max_correlation', 'tags_max_correlation', 'api_max_correlation', 'configuration_index']]
    top_feature['category_max_correlation'] = top_feature['category_max_correlation'].apply(
        lambda x: math.ceil(x * 1000) / 1000)
    top_feature['tags_max_correlation'] = top_feature['tags_max_correlation'].apply(
        lambda x: math.ceil(x * 1000) / 1000)
    top_feature['api_max_correlation'] = top_feature['api_max_correlation'].apply(
        lambda x: math.ceil(x * 1000) / 1000)

    top_feature.to_csv(paths.top, index=False)

    # Create boxplot.
    for feature in features:
        plt.close('all')

        # Selecting palette
        unique = configurations[feature].unique()
        palette = dict(zip(unique, sns.color_palette("cubehelix", len(unique))))

        fig, ((ax0, ax1), (ax2, ax3)) = plt.subplots(nrows=2, ncols=2, sharey=True)

        sns.boxplot(ax=ax0, x=feature, y="domains_max_correlation", data=configurations, palette=palette)
        sns.boxplot(ax=ax1, x=feature, y="tags_max_correlation", data=configurations, palette=palette)
        sns.boxplot(ax=ax2, x=feature, y="category_max_correlation", data=configurations, palette=palette)
        sns.boxplot(ax=ax3, x=feature, y="api_max_correlation", data=configurations, palette=palette)

        ax0.set_title('Domains')
        ax1.set_title('Tags')
        ax2.set_title('Categories')
        ax3.set_title('API Affiliation')

        ax0.set_ylabel('max correlation')
        ax1.set_ylabel('max correlation')
        ax2.set_ylabel('max correlation')
        ax3.set_ylabel('max correlation')

        ax0.xaxis.label.set_visible(False)
        ax1.xaxis.label.set_visible(False)
        ax2.xaxis.label.set_visible(False)
        ax3.xaxis.label.set_visible(False)
        ax3.yaxis.label.set_visible(False)
        ax1.yaxis.label.set_visible(False)

        save_figure(fig, "sbox_" + feature)

    for feature_a in features:
        for feature_b in features:
            if not feature_a == feature_b:
                plt.close('all')

                # Selecting palette
                unique = configurations[feature_b].unique()
                palette = dict(zip(unique, sns.color_palette("cubehelix", len(unique))))

                fig, ((ax0, ax1), (ax2, ax3)) = plt.subplots(nrows=2, ncols=2, sharey=True)

                sns.boxplot(ax=ax0, x=feature_a, hue=feature_b, y="domains_max_correlation", data=configurations,
                            palette=palette)
                sns.boxplot(ax=ax1, x=feature_a, hue=feature_b, y="tags_max_correlation", data=configurations,
                            palette=palette)
                sns.boxplot(ax=ax2, x=feature_a, hue=feature_b, y="category_max_correlation", data=configurations,
                            palette=palette)
                sns.boxplot(ax=ax3, x=feature_a, hue=feature_b, y="api_max_correlation", data=configurations,
                            palette=palette)

                ax0.set_title('Domains')
                ax1.set_title('Tags')
                ax2.set_title('Categories')
                ax3.set_title('API Affiliation')

                ax0.set_ylabel('max correlation')
                ax1.set_ylabel('max correlation')
                ax2.set_ylabel('max correlation')
                ax3.set_ylabel('max correlation')

                ax0.xaxis.label.set_visible(False)
                ax1.xaxis.label.set_visible(False)
                ax2.xaxis.label.set_visible(False)
                ax3.xaxis.label.set_visible(False)
                ax3.yaxis.label.set_visible(False)
                ax1.yaxis.label.set_visible(False)

                save_figure(fig, "cbox_" + feature_a + "_" + feature_b)
