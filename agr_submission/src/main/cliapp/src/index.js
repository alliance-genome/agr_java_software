import 'bootstrap/dist/css/bootstrap.min.css';
import './index.css';

import React from 'react';
import ReactDOM from 'react-dom';
import { BrowserRouter as Router, Route } from 'react-router-dom';
import { Container, Row } from 'reactstrap';
import { Provider } from "react-redux";

import configureStore from "./store/configureStore";

import Homepage from './containers/homepage';
import AdminPage from './containers/adminpage';
import LoginPage from './containers/loginpage';
import AdminDataTypes from './containers/admin/AdminDataTypes';
import DataFiles from './containers/datafiles';

import Header from './components/Header';


ReactDOM.render(
	<Provider store={ configureStore() }>
		<Router>
			<div>
				<Header />
				<main className="my-5 py-5">
					<Container className="px-0">
						<Row noGutters className="pt-2 pt-md-5 w-100 px-4 px-xl-0 position-relative">
							<Route exact path="/" component={Homepage} />
							<Route exact path="/admin" name="adminshort" component={AdminPage} />
							<Route path="/admin/datatypes/:datatype?" name="adminlong" component={AdminDataTypes} />
							<Route exact path="/datafiles" name="datafilesshort" component={DataFiles} />
							<Route exact path="/login" component={LoginPage} />
							<Route path="/datafiles/:datatype/:subdatatype" name="datafileslong" component={DataFiles} />
						</Row>
					</Container>
				</main>
			</div>
		</Router>
	</Provider>,
	document.getElementById('root')
);
