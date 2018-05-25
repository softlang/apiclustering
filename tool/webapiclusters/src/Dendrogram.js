import React, {Component} from 'react'
import './App.css'
import * as d3 from "d3";
//import data from './data/flare.json';

var deepEqual = require('deep-equal');

export default class Dendrogram extends Component {
    constructor(props) {
        super(props);

        this.state = {
            width: 1000,
            height: 500,
            data: {linkage: {}},

        };
        const width = window.innerWidth * 0.7;
        const height = window.innerHeight;

    }

    createDendrogram(data) {
        const width = window.innerWidth * 0.7;
        const height = window.innerHeight * 3;

        let svg = this.state.svg;

        svg.select('g').remove();
        svg.select('rect').remove();

        let g = svg.append('g');

        svg.append("rect")
            .attr("width", width)
            .attr("height", height)
            .style("fill", "none")
            .style("pointer-events", "all")
            .call(d3.zoom()
                .on("zoom", zoomed));

        function zoomed() {
            g.attr("transform", "translate(" + 0 + "," + d3.event.transform.y + ")");
        }

        let cluster = d3.cluster()
            .size([8000, width - 160]);

        let root = d3.hierarchy(data);
        cluster(root);

        function thresholdPosition(x) {
            return (1.0 - x) * 800
        }

        let link = g.selectAll(".link")
            .data(root.descendants().slice(1))
            .enter().append("path")
            .attr("class", "link")
            .attr("d", diagonal)
            .attr("fill", "none")
            .attr("stroke", "black");


        let node = g.selectAll(".node")
            .data(root.descendants())
            .enter().append("g")
            .attr("class", function (d) {
                return "node" + (d.children ? " node--internal" : " node--leaf");
            })
            .attr("transform", function (d) {
                return "translate(" + thresholdPosition(d.data.threshold) + "," + d.x + ")";
            });


        // node.append("circle")
        //     .attr("r", 2.5);

        node.append("text")
            .attr("dy", 3)
            .attr("x", function (d) {
                return d.children ? -8 : 8;
            })
            .style("text-anchor", function (d) {
                return d.children ? "end" : "start";
            })
            .text(function (d) {
                return d.data.name
            });

        function diagonal(d) {
            return "M" + thresholdPosition(d.data.threshold) + "," + d.x
                + "L" + (thresholdPosition(d.parent.data.threshold) + 0) + "," + d.x
                + " " + thresholdPosition(d.parent.data.threshold) + "," + d.parent.x;
        }


        // function diagonal(d) {
        //     return "M" + d.data.threshold * 1000 + "," + d.x
        //         + "C" + (d.parent.data.threshold * 1000 + 0) + "," + d.x
        //         + " " + (d.parent.data.threshold * 1000 + 0) + "," + d.x
        //         + " " + d.parent.data.threshold * 1000 + "," + d.parent.x;
        // }

        // function diagonal(d) {
        //     let somec = 0
        //     return "M" + d.y + "," + d.x
        //         + "C" + (d.parent.y + somec) + "," + d.x
        //         + " " + (d.parent.y + somec) + "," + d.x
        //         + " " + d.parent.y + "," + d.parent.x;
        // }
    }

    shouldComponentUpdate() {
        return true;
    }

    componentWillReceiveProps(nextProps) {
        this.createDendrogram(nextProps.data.linkage);
    }

    onRef = (ref) => {
        // Note: () => ... binds the class to fkt.
        this.setState({svg: d3.select(ref)}, () => {
            this.createDendrogram(this.state.data.linkage)
        })
    };

    render() {
        const style = {
            width: window.innerWidth * 0.7,
            height: window.innerHeight,
            backgroundColor: 'white'
        };
        return <svg style={style} ref={this.onRef}>
            <g>

            </g>
        </svg>
    }
}
