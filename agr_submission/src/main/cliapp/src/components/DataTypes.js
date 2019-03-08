import React, { Component, Fragment } from 'react';
import { NavLink as RRNavLink } from 'react-router-dom';
import axios from 'axios';
import { CardBody, Card, ListGroup, ListGroupItem, NavLink, Badge } from 'reactstrap';

class DataTypes extends Component {

	state = { datatypes: null }

	componentDidMount() {
		axios.get('http://localhost:8080/api/datatype/all').then(response => {
			console.log("Old DataType API Running");
			this.setState({ datatypes: response.data });
		});
	}

	renderdatatypes() {
		return this.state.datatypes.map((node, index) => {
			return (
				<ListGroupItem className="text-nowrap" key={ node.id }>
					<NavLink to={"/admin/datatypes/" + node.id } tag={RRNavLink}>
						<Badge>{ node.name }</Badge>: { node.description }
					</NavLink>
				</ListGroupItem>
			)
		})
	}

	render() {
		return (
			<Fragment>
				{ this.state.datatypes && <Card>
					<CardBody>
						<ListGroup>
							Data Types: { this.renderdatatypes() }
						</ListGroup>
					</CardBody>
				</Card> }
			</Fragment>
		);
	}
}

export default DataTypes;