import React, {Component} from 'react'
import 'rc-slider/assets/index.css';
import Slider from 'rc-slider';

// https://silviomoreto.github.io/bootstrap-select/examples/

export default class Menu extends Component {

    // Note: <input value={this.props.threshold} type="number" step="0.01" className="form-control" id="pwd" onChange={this.thresholdChanged.bind(this)}/>

    render() {
        let style = {padding: 5};

        let parameters = Object.entries(this.props.parameters).map(([key, value]) => {
            return (
                <tr>
                    <th style={style}>{key.toString()}</th>
                    <th style={style}>
                        <select data-key={key.toString()} className="form-control" onChange={this.props.parameterChanged}>
                            {value.map(x => {
                                return (<option key={x.toString()} value={x.toString()}>{x}</option>)
                            })}
                        </select>
                    </th>
                </tr>
            );
        });


        return (<div>
            <div className="panel panel-primary">
                <div className="panel-heading">Visualization Type</div>
                <div className="panel-body">
                  <select className="form-control" onChange={this.props.visualizationChanged}>
                      <option key="Dendrogram">Dendrogram</option>
                      <option key="Network">Network</option>
                      <option key="Plain">Plain</option>
                  </select>
                </div>
            </div>
            <div className="panel panel-primary">
                <div className="panel-heading">Threshold</div>
                <div className="panel-body">
                    <Slider defaultValue={0.5} step={0.01} min={0.0} max={1.0} value={this.props.threshold}
                            onChange={this.props.thresholdChanged}/>
                </div>
            </div>
            <div className="panel panel-primary">
                <div className="panel-heading">Parameters</div>
                <div className="panel-body">
                    <table className="table table-striped">
                        {parameters}
                    </table>
                </div>
            </div>
        </div>);
    }
}