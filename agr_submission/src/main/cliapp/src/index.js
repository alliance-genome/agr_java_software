import 'bootstrap/dist/css/bootstrap.min.css';
import React from 'react';
import {render} from 'react-dom';
import './index.css';
import { Router, browserHistory } from 'react-router';
import routes from './routes';

render(<Router history={browserHistory} routes={routes} />, document.getElementById('root'));
