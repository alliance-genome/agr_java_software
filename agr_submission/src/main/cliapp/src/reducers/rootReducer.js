import { combineReducers } from 'redux';
import dataTypeReducer from './dataTypeReducer';
import dataFileReducer from './dataFileReducer';

const rootReducer = combineReducers({
    dataTypeReducer,
    dataFileReducer
})

export default rootReducer