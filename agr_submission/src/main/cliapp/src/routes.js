import React from 'react';
import { Route, IndexRoute, browserHistory} from 'react-router';

import Homepage from './containers/homepage';

export default (
  <Route path="/" component={Homepage} history={browserHistory}>
    <IndexRoute component={Homepage}/>
    <Route path="*" component={Homepage}/>
  </Route>
);