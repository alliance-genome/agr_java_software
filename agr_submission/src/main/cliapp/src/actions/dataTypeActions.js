import * as types from './actionTypes';
import dataTypeApi from '../api/dataTypeApi';

export function loadDataTypes() {
	return function(dispatch) {
		return dataTypeApi.getAll().then(dataTypes => {
			dispatch(loadDataTypesSuccess(dataTypes));
		}).catch(error => {
			throw(error);
		});
	};
}

export function loadDataTypesSuccess(dataTypes) {
	return { type: types.LOAD_DATATYPES_SUCCESS, dataTypes };
}