import React, {Component} from 'react'
import './App.css'
import * as d3 from "d3";
var deepEqual = require('deep-equal');


export default class Network extends Component {
    constructor(props) {
        super(props);

        this.state = {
            width: 1000,
            height: 500
        };
        const width = window.innerWidth * 0.7;
        const height = window.innerHeight;

        let margin = {top: 0, left: 0, bottom: 0, right: 0};
        let chartWidth = width - (margin.left + margin.right);
        let chartHeight = height - (margin.top + margin.bottom);

        this.simulation = d3.forceSimulation()
            .force("link", d3.forceLink().distance(function (d) {
                return d.value;
            }).strength(0.3)) // This emphasizes edges.
            .force("collide",d3.forceCollide( function(d){return 30}).strength(0.5)  ) // This prevents node collision. (Do not need this)
            .force("charge", d3.forceManyBody().strength(-15)) // Nice, this is making space.
            .force("center", d3.forceCenter(chartWidth / 2, chartHeight / 2)) // Centers the plot.
            .force('x', d3.forceX(chartWidth / 20).strength(0.015)) // Prevents escape of nodes.
            .force('y', d3.forceY(chartHeight / 20).strength(0.015)) // Prevents escape of nodes.
            .velocityDecay(0.5)
            .on('tick', this.ticked.bind(this))
            .stop();
    }

    shouldComponentUpdate() {
        return true;
    }

    componentWillReceiveProps(nextProps) {
        this.createNodes(nextProps.data.nodes);
        this.createLinks(nextProps.data.links);
        this.simulation.restart();
    }

    onRef = (ref) => {
        // Note: () => ... binds the class to fkt.
        this.setState({g: d3.select(ref)}, () => {
            this.createNodes(this.props.data.nodes);
            this.createLinks(this.props.data.links);
            this.simulation.restart();
        })
    };

    ticked() {
        this.state.g
            .selectAll(".link")
            .attr("x1", function (d) {
                return d.source.x;
            })
            .attr("y1", function (d) {
                return d.source.y;
            })
            .attr("x2", function (d) {
                return d.target.x;
            })
            .attr("y2", function (d) {
                return d.target.y;
            });

        this.state.g
            .selectAll(".node")
            .attr("transform", function (d) {
                return "translate(" + d.x + "," + d.y + ")";
            });
    }

    createLinks(data) {
        // TODO: Factor these size things out.
        const width = window.innerWidth * 0.7;
        const height = window.innerHeight;

        let margin = {top: 0, left: 0, bottom: 0, right: 0};
        let chartWidth = width - (margin.left + margin.right);
        let chartHeight = height - (margin.top + margin.bottom);

        let chartLayer = this.state.g.classed("chartLayer", true);

        chartLayer
            .attr("width", chartWidth)
            .attr("height", chartHeight)
            .attr("transform", "translate(" + [margin.left, margin.top] + ")")

        let links = this.state.g
            .selectAll(".link")
            .data(data);

        links.exit().remove();

        let linksE = links
            .enter()
            .append("line")
            .classed("link",true)
            .attr("stroke", "grey");

        this.simulation.force("link").links(data);

        /* Restart the force layout */
    }

    createNodes(data) {
        // TODO: Factor these size things out.
        const width = window.innerWidth * 0.7;
        const height = window.innerHeight;

        let margin = {top: 0, left: 0, bottom: 0, right: 0};
        let chartWidth = width - (margin.left + margin.right);
        let chartHeight = height - (margin.top + margin.bottom);

        let chartLayer = this.state.g.classed("chartLayer", true);

        let position = {};
        if(this.state.lastNodes != null){
            this.state.lastNodes.forEach((x) =>
                position[x.label] = {x:x.x, y:x.y}
            );
        }

        for (var i = 0; i < data.length; i++) {
            let label = data[i].label;
            if (position[label] != null){
                data[i].x = position[label].x;
                data[i].y = position[label].y;
            }
        }

        chartLayer
            .attr("width", chartWidth)
            .attr("height", chartHeight)
            .attr("transform", "translate(" + [margin.left, margin.top] + ")")

        let nodes = this.state.g
            .selectAll(".node")
            .data(data,d => d.label);



        nodes.exit().remove();

        let nodesE = nodes
            .enter()
            .append("g")
            .classed("node",true);

        nodesE.call(d3.drag()
            .on("start", dragstarted.bind(this))
            .on("drag", dragged.bind(this))
            .on("end", dragended.bind(this)));

        nodesE.append("circle")
            .attr("r", function (d) {
                return d.r
            })
            .attr("stroke", "grey")
            .attr("stroke-width", "2")
            .attr("fill", "red");

        nodesE.append("title")
            .text(function (d) {
                return d.label;
            });

        nodesE.append("text")
            .attr("dx", 12)
            .attr("dy", ".35em")
            .text(function (d) {
                return d.label
            });

        this.simulation.nodes(data);

        /* Restart the force layout */
        this.setState({lastNodes: data})


        // simulation
        //     .nodes(data.nodes)
        //     .on("tick", ticked);
        //

        function dragstarted(d) {
            if (!d3.event.active) this.simulation.alphaTarget(0.3).restart();
            d.fx = d.x;
            d.fy = d.y;
        }

        function dragged(d) {
            d.fx = d3.event.x;
            d.fy = d3.event.y;
        }

        function dragended(d) {
            if (!d3.event.active) this.simulation.alphaTarget(0);
            d.fx = null;
            d.fy = null;
        }
    }

    render() {
        const style = {
            width: window.innerWidth * 0.7,
            height: window.innerHeight,
            backgroundColor:'powderblue'
        };
        return <svg style={style}>
            <g ref={this.onRef}>s
            </g>
        </svg>
    }
}
