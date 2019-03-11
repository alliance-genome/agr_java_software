import React, { Fragment, Component } from 'react';
import { Col } from 'reactstrap';
import { connect } from 'react-redux';
import DataTypes from '../../components/DataTypes';
import AdminEditDataType from '../../components/admin/EditDataType';
import { loadDataType, loadDataTypes } from '../../actions/dataTypeActions';

class AdminDataTypes extends Component {

	getData(datatype) {
		this.props.dispatch(loadDataType(this.props.match.params.dataType));
	}

	componentDidUpdate(prevProps) {
		console.log("Update: ", this.props);
		if (this.props.match.params.datatype !== prevProps.match.params.dataType) {
			this.getData(this.props.match.params.dataType);
		}
	}

	componentDidMount() {
		//console.log("Did Mount: ", loadDataTypes());
		//this.props.dispatch(loadDataTypes());
		var match = this.props.match;
		console.log("Did Mount: ", this.props);
		if (match != null && match.params != null && match.params.dataType != null) {
			this.getData(this.props.match.params.dataType);
		}
	}

	render() {
		return (
			<Fragment>
				<Col xs={{ order: 2 }} md={{ size: 4, order: 1 }} tag="aside" className="pb-5 mb-5 pb-md-0 mb-md-0 mx-auto mx-md-0">
					<DataTypes dataTypes={this.props.dataTypes} />
				</Col>

				<Col xs={{ order: 1 }} md={{ size: 7, offset: 1 }} tag="section" className="py-5 mb-5 py-md-0 mb-md-0">
					{this.props.datatype == null && <span>Please choose a data type</span>}
					{this.props.datatype != null && <AdminEditDataType data={this.state.datatype} />}
				</Col>
			</Fragment>
		)
	}
}

export default connect()(AdminDataTypes);