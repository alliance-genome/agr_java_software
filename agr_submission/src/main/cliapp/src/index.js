import 'bootstrap/dist/css/bootstrap.min.css';
import './index.css';

import React from 'react';
import ReactDOM from 'react-dom';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';
import { Container, Row } from 'reactstrap';
import { Provider } from "react-redux";

import * as serviceWorker from './serviceWorker';

import { createStore, applyMiddleware } from 'redux';

import Homepage from './containers/homepage';
import AdminPage from './containers/adminpage';
import LoginPage from './containers/loginpage';
import AdminDataTypes from './containers/adminpage/AdminDataTypes';
import DataFiles from './containers/datafiles';

import Header from './components/Header';

import rootReducer from './reducers/rootReducer';

import thunk from 'redux-thunk';

const store = createStore(rootReducer, applyMiddleware(thunk));

ReactDOM.render(
	<Provider store={store}>
		<Router>
			<div>
				<Header />
				<main className="my-5 py-5">
					<Container className="px-0">
						<Row noGutters className="pt-2 pt-md-5 w-100 px-4 px-xl-0 position-relative">
							<Switch>
								<Route exact path="/" component={Homepage} />
								<Route path="/login" component={LoginPage} />


								<Route path="/admin/datatypes/:dataType?" component={AdminDataTypes} />
								<Route path="/admin/datatypes" component={AdminDataTypes} />
								<Route exact path="/admin" component={AdminPage} />
								
								<Route path="/datafiles/:datatype/:subdatatype" component={DataFiles} />
								<Route path="/datafiles" component={DataFiles} />
								
							</Switch>
						</Row>
					</Container>
				</main>
			</div>
		</Router>
	</Provider>,
	document.getElementById('root')
);
serviceWorker.register();
