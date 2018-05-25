# ApiClustering
Classification of APIs by Hierarchical Clustering

## What is contained:
* The [tool](https://github.com/softlang/apiclustering/tree/master/tool) folder contains the web application and the clustering process.
* The [dataset](https://github.com/softlang/apiclustering/tree/master/dataset) folder contains the full data of paper (except some pom related files due to size).
* The dataset contains the API evolution plots in the [evolution](https://github.com/softlang/apiclustering/tree/master/dataset/evolution) folder (e.g., [com.google.guava-guava.png](https://github.com/softlang/apiclustering/tree/master/dataset/evaluation/com.google.guava-guava.png)).
* The dataset contains the feature correlations in the [evaluation](https://github.com/softlang/apiclustering/tree/master/dataset/evaluation) folder (e.g., [cbox_analytical_granularity.png](https://github.com/softlang/apiclustering/tree/master/dataset/evaluation/cbox_analytical_granularity.png)).
* The dataset contains the [curated API suite](https://github.com/softlang/apiclustering/tree/master/dataset/HaertelAL18.csv).

## How to reproduce the inference:
* Create a file "config.properties" in the project org.softlang.dscor and a "config.py" in the project org.softlang.dscorpython with the following entries:
* ``temp=<filepath to a temporary folder>``
* ``dataset=<filepath to a dataset folder>``
* (Optional) ``login_git=<github-username>`` 
* (Optional) ``password_git=<github-password>`` (only needed for the Git API)
* When running Java add the VM Arguments *-Xss* and *-Xmx* depending on your system (e.g., -Xss4m -Xmx10000m).

* Run `org.softlang.dscor.process.Poms` to mine the pom list from Github.
* Run `org.softlang.dscor.process.Clients` to extract the dependencies of each file contained in the pom list.
* ...
