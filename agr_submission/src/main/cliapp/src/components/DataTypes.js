import React, { Component, Fragment } from 'react';
import { NavLink as RRNavLink } from 'react-router-dom';
import { connect } from 'react-redux';
import { loadDataTypes } from "../actions/dataTypeActions";

import { CardBody, Card, ListGroup, ListGroupItem, NavLink, Badge } from 'reactstrap';

class DataTypes extends Component {

	//state = { datatypes: null }

	componentDidMount() {

		//console.log(this.props);
		//this.props.dispatch(loadDataTypes());
		//console.log();
		//this.props.dispatch();
		//axios.get('http://localhost:8080/api/datatype/all').then(response => {
		//	console.log("Old DataType API Running");
		//	this.setState({ datatypes: response.data });
		//});

	}

	renderdatatypes() {
		return this.props.datatypes.map((node, index) => {
			return (
				<ListGroupItem className="text-nowrap" key={node.id}>
					<NavLink to={"/admin/datatypes/" + node.id} tag={RRNavLink}>
						<Badge>{node.name}</Badge>: {node.description}
					</NavLink>
				</ListGroupItem>
			)
		})
	}

	render() {
		return (
			<Fragment>
				{this.props.dataTypes && <Card>
					<CardBody>
						<ListGroup>
							Data Types: {this.renderdatatypes()}
						</ListGroup>
					</CardBody>
				</Card>}
			</Fragment>
		);
	}
}



export default connect()(DataTypes);