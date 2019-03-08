import * as types from '../actions/actionTypes';
import initialState from './initialState';

export default function dataTypeReducer(state = initialState, action) {
	switch (action.type) {
		case types.LOAD_DATATYPES_SUCCESS:
			return action.dataTypes
		default:
			return state;
	}
}