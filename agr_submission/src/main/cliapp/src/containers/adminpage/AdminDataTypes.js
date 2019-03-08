import React, { Fragment, Component } from 'react';
import { Col } from 'reactstrap';
import axios from 'axios';

import DataTypes from '../../components/DataTypes';
import AdminEditDataType from '../../components/admin/EditDataType'

class AdminDataTypes extends Component {

	state = { datatype: null }

	getData(datatype) {
		if(datatype != null) {
			axios.get('http://localhost:8080/api/datatype/' + datatype).then(response => { this.setState({ datatype: response.data }); });
		}
	}

	componentDidUpdate(prevProps) {
		if (this.props.match.params.datatype !== prevProps.match.params.datatype) {
			this.getData(this.props.match.params.datatype);
		}
	}

	componentDidMount() {
		var match = this.props.match;
		if(match != null) {
			this.getData(this.props.match.params.datatype);
		}
	}

	render() {
		return (
			<Fragment>
				<Col xs={{ order: 2 }} md={{ size: 4, order: 1 }} tag="aside" className="pb-5 mb-5 pb-md-0 mb-md-0 mx-auto mx-md-0">
					<DataTypes />
				</Col>
							   
				<Col xs={{ order: 1 }} md={{ size: 7, offset: 1 }} tag="section" className="py-5 mb-5 py-md-0 mb-md-0">
					{ this.state.datatype == null && <span>Please choose a data type</span> }
					{ this.state.datatype != null && <AdminEditDataType data={ this.state.datatype } /> }
				</Col>
			</Fragment>
		)
	}
}

export default AdminDataTypes;