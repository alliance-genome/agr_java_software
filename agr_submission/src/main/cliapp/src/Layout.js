import React, { Fragment, Component } from 'react';
import { BrowserRouter as Router, Route } from "react-router-dom";
import { Container, Row } from 'reactstrap';

import Header from './components/Header';


import Homepage from './containers/homepage';
import Admin from './containers/admin';
import AdminDataTypes from './containers/admin/AdminDataTypes';
import DataFiles from './containers/datafiles';

class Layout extends Component {
	render() {
		return (
			<Router component={Homepage}>
				<Fragment>
					<Header />
					<main className="my-5 py-5">
						<Container className="px-0">
							<Row noGutters className="pt-2 pt-md-5 w-100 px-4 px-xl-0 position-relative">
								<Route name="home" exact path="/" component={Homepage}/>
								<Route name="adminshort" path="/admin" component={Admin}/>
								<Route name="adminlong" path="/admin/datatypes/:datatype?" component={AdminDataTypes}/>
								<Route exact name="datafilesshort" path="/datafiles" component={DataFiles}/>
								<Route name="datafileslong" path="/datafiles/:datatype/:subdatatype" component={DataFiles}/>
							</Row>
						</Container>
					</main>
				</Fragment>
			</Router>
		);
	}
}

export default Layout;
