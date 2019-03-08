
class DataTypeApi {

	static getAll() {
		return fetch('http://localhost:8080/api/datatype/all').then(response => {
			console.log("NEW Data Type API Running");
			return response.json();
		}).catch(error => {
			return error;
		});
	}
}

export default DataTypeApi;