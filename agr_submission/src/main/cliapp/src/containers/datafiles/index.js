import React, { Fragment, Component } from 'react';
import { ListGroup, ListGroupItem, Badge, Col } from 'reactstrap';
import axios from 'axios';

import SideCard from '../../components/SideCard';

class DataFiles extends Component {

    dateFormat = require('dateformat');

    state = { datafiles: null }

    componentDidUpdate(prevProps) {
        if (this.props.match.params.datatype !== prevProps.match.params.datatype || this.props.match.params.subdatatype !== prevProps.match.params.subdatatype) {
            axios.get('http://localhost:8080/api/datafile/' + this.props.match.params.datatype + '/' + this.props.match.params.subdatatype)
                .then(response => {
                    this.setState({ datafiles: response.data });
                });
        }
    }

    componentDidMount() {
        if (this.props.match.params.datatype != null || this.props.match.params.subdatatype != null) {
            axios.get('http://localhost:8080/api/datafile/' + this.props.match.params.datatype + '/' + this.props.match.params.subdatatype)
                .then(response => {
                    this.setState({ datafiles: response.data });
                });
        }
    }

    datafiles(data) {
        if (data != null) {
            return data.map((node, index) => {
                return (
                    <ListGroupItem key={node.id}>
                        <ListGroup>
                            <ListGroupItem>{node.s3Path} <Badge pill href={'http://download.alliancegenome.org/' + node.s3Path}>Download</Badge></ListGroupItem>
                            <ListGroupItem>Uploaded: {this.dateFormat(node.uploadDate)}</ListGroupItem>
                            <ListGroupItem>Schema Version: {node.schemaVersion.schema}</ListGroupItem>
                            <ListGroupItem>Data Type: {node.dataType.name}</ListGroupItem>
                            <ListGroupItem>Data SubType: {node.dataSubType.name}</ListGroupItem>
                            <ListGroupItem hidden>{JSON.stringify(node)}</ListGroupItem>
                        </ListGroup>
                    </ListGroupItem>
                )
            })
        }

    }

    render() {

        return (
            <Fragment>
                <Col xs={{ order: 2 }} md={{ size: 4, order: 1 }} tag="aside" className="pb-5 mb-5 pb-md-0 mb-md-0 mx-auto mx-md-0">
                    <SideCard />
                </Col>

                <Col xs={{ order: 1 }} md={{ size: 7, offset: 1 }} tag="section" className="py-5 mb-5 py-md-0 mb-md-0">
                    <ListGroup>{this.datafiles(this.state.datafiles)}</ListGroup>
                </Col>
            </Fragment>
        )
    }
}

export default DataFiles;