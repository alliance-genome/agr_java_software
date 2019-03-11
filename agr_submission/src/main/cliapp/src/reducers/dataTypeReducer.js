import * as types from '../actions/actionTypes';

let initialState = {
	dataTypes: [],
	dataType: {}
}

export default function dataTypeReducer(state = initialState, action) {
	switch (action.type) {
		case types.LOAD_DATATYPES_SUCCESS:
			return { dataTypes: action.dataTypes };
		case types.LOAD_DATATYPE_SUCCESS:
			return { dataType: action.dataType };
		default:
			return state;
	}
}