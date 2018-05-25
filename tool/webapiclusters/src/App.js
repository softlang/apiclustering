import React, {Component} from 'react';
import './App.css';
import * as d3 from "d3";
import Network from "./Network";
import Dendrogram from "./Dendrogram";
import Menu from "./Menu";
import axios from 'axios';

var deepEqual = require('deep-equal');

// http://cmichel.io/how-to-use-d3js-in-react/
// https://facebook.github.io/immutable-js/
// https://daveceddia.com/ajax-requests-in-react/
// https://www.hongkiat.com/blog/node-js-server-side-javascript/

const server = "http://localhost:8080";

class App extends Component {
    constructor(props) {
        super(props);
        this.state = {
            threshold: 0.5,
            visualization: "Dendrogram",
            parameters: {}
        };
    }

    componentDidMount() {
        // Get the currently available parameters.
        axios.get(server + '?tpe=parameter').then(x => {
            let selected = {};
            Object.entries(x.data).forEach(([k, v]) => {
                selected[k] = v[0];
            });

            this.setState({
                selected: selected,
                parameters: x.data
            });
        });
    }

    onDeleteClicked() {
        this.setState({threshold: (this.state.threshold + 0.05)});
    }

    thresholdChanged(value) {
        this.setState({threshold: value});
        console.log("threshold: " + value);
    }

    visualizationChanged(event) {
        this.setState({visualization: event.target.value});
    }

    parameterChanged(event) {
        let parameter = event.target.dataset.key;
        let value = event.target.value;

        let selected = JSON.parse(JSON.stringify(this.state.selected));
        selected[parameter] = value;
        this.setState({selected: selected});
        console.log(parameter + " assigned to " + value);
    }


    shouldComponentUpdate(nextProps, nextState) {
        // Changing the selected parameters triggers new data fetching.
        if (!deepEqual(this.state.selected, nextState.selected)) {
            console.log("Fetch new data");
            let parameters = nextState.selected
            let query = Object.keys(parameters)
                .map(k => k + '=' + parameters[k])
                .join('&');

            axios.get(server + '?' + query).then(x => {
                this.setState({graph: x.data});
            });
        }
        return true;
    }

    data() {
        // Construct an index for the nodes.
        const index = {};
        let i;
        let graph = this.state.graph;

        return graph

        // if (graph == null | graph == {})
        //     return null;
        //
        // for (i = 0; i < graph.nodes.length; i++) {
        //     index[graph.nodes[i].name] = i
        // }
        //
        // return {
        //     nodes: graph.nodes.map(function (d) {
        //         //return {label: d.name, r: Math.pow(d.classes, 1 / 3)}
        //         return {label: d.name, r: 5}
        //     }),
        //     links: graph.links.filter((d) => (d.similarity < this.state.threshold)).map(function (d) {
        //         return {source: index[d.source], target: index[d.target], value: parseFloat(d.similarity)}
        //     })
        // };
    }

    render() {
        let visualization = null;
        let data = this.data();

        if (data === null)
            visualization = <p> no data </p>
        else if (this.state.visualization === "Dendrogram") {
            visualization = <Dendrogram data={data} style={{flex: 1}}/>
        }else if (this.state.visualization === "Network") {
            visualization = <Network data={data} style={{flex: 1}}/>
        } else if (this.state.visualization === "Plain") {
            visualization = <div>{JSON.stringify(data)}</div>
        }

        return (
            <div className='row'>
                <div className='col-md-9'>
                    {visualization}
                </div>
                <div className='col-md-3'>
                    <Menu
                        threshold={this.state.threshold}
                        parameters={this.state.parameters}
                        thresholdChanged={this.thresholdChanged.bind(this)}
                        visualizationChanged={this.visualizationChanged.bind(this)}
                        parameterChanged={this.parameterChanged.bind(this)}
                        onDeleteClicked={this.onDeleteClicked.bind(this)}/>
                </div>
            </div>
        );
    }
}

export default App;