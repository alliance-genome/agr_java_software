import * as types from './actionTypes';
import dataTypeApi from '../api/dataTypeApi';

export function loadDataTypes() {
	return (dispatch) => {
		//console.log("This is a test");
		dataTypeApi.getAll().then(dataTypes => {
			//console.log("DataTypes: " + dataTypes.length);
			dispatch(loadDataTypesSuccess(dataTypes));
		}).catch(error => {
			throw (error);
		});
	};
}

export const loadDataType = (id) => {
	console.log("This is another test");
	return function (dispatch) {
		dataTypeApi.getDataType(id).then(dataType => {
			console.log("DataType: " + dataType);
			dispatch(loadDataTypeSuccess(dataType));
		}).catch(error => {
			throw (error);
		});
	}
}

export function loadDataTypeAction() {
	return { type: types.LOAD_DATATYPES };
}

export function loadDataTypesSuccess(dataTypes) {
	return { type: types.LOAD_DATATYPES_SUCCESS, dataTypes };
}

export function loadDataTypeSuccess(dataType) {
	return { type: types.LOAD_DATATYPE_SUCCESS, dataType };
}