import React, { Component, Fragment } from 'react';
//import PropTypes from 'prop-types';
import { CardBody, Card } from 'reactstrap';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

import DataTypeList from './DataTypeList';

import { loadDataTypes } from '../actions/dataTypeActions';

class SideCard extends Component {
 
	constructor(props) {
		super(props);
		const { dispatch } = props;
		this.boundActions = bindActionCreators(loadDataTypes, dispatch);
	}

	componentDidMount() {
		//this.props.dispatch(loadDataTypes());
	}

	componentDidUpdate() {
		//this.props.dispatch(loadDataTypes());
	}

	render() {

		return (
			<Fragment>
				{ this.props.datatypes && <Card>
					<CardBody>
						Data Types:
						<DataTypeList data={this.props.datatypes} />
					</CardBody>
				</Card> }
			</Fragment>
		);
	}
}



export default connect(store => ({ datatypes: store.datatypes }))(SideCard);