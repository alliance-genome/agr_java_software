import React, { Component } from 'react';
//import { NavLink as RRNavLink } from 'react-router-dom';
//import { Route } from 'react-router-dom';
//import axios from 'axios';
import { Button, ListGroupItem, ListGroup, Form, FormGroup, Label, Input, Card, CardBody, CardTitle } from 'reactstrap';

class AdminEditDataType extends Component {

	state = { form_data: { } };
	formRef = React.createRef();

	componentDidMount() {
		this.formRef.current.reset()
		this.setState({ form_data: this.props.data});
	}

	componentDidUpdate(prevProps) {
		if (this.props.data.id !== prevProps.data.id) {
			this.formRef.current.reset();
			this.setState({ form_data: this.props.data});
		}
	}

	renderSchemaFiles(schemaFiles) {
		return schemaFiles.map((node, index) => {
			return (
				<ListGroup key={ node.id }>
					<ListGroupItem >{ node.schemaVersion.schema } -> { node.filePath }</ListGroupItem>
				</ListGroup>
			)
		})
	}

	renderDataSubTypes(dataSubTypes) {
		return dataSubTypes.map((node, index) => {
			return (
				<ListGroup key={ node.id }>
					<ListGroupItem >{ node.name }</ListGroupItem>
				</ListGroup>
			)
		})
	}

	changeHandler = event => {
		const name = event.target.name;
		const value = event.target.value;
		console.log("Name: " + event.target.name);
		console.log("Value: " + event.target.value);
		
		const { form_data } = this.state;
		const newFormData = {
			...form_data,
			[name]: value
		};

		this.setState({ form_data: newFormData });
		console.log("Change: Json: " + JSON.stringify(this.state));
	}

	saveDataType = formSubmitEvent => {
		formSubmitEvent.preventDefault();
		//this.props.data = this.state.form_data;
		console.log("saveDataType: Json: " + JSON.stringify(this.state.form_data));
	}

	render() {
		return (
			<Card>
				<CardBody>
					<CardTitle>ID: {this.state.form_data.id }</CardTitle>
					<Form onSubmit={ this.saveDataType } innerRef={this.formRef}>
						<FormGroup>
							<Label for="name">Name:</Label>
							<Input name="name" defaultValue={ this.state.form_data.name } onChange={ this.changeHandler } />
						</FormGroup>
						<FormGroup>
							<Label for="description">Description:</Label>
							<Input name="description" defaultValue={ this.state.form_data.description } onChange={ this.changeHandler } />
						</FormGroup>
						<FormGroup>
							<Label for="fileExtension">File Extension (Leave off the ".")</Label>
							<Input name="fileExtension" defaultValue={ this.state.form_data.fileExtension } onChange={ this.changeHandler } />
						</FormGroup>
						<FormGroup check>
							<Label>
								<Input name="dataSubTypeRequired" type="checkbox" defaultChecked={ this.state.form_data.dataSubTypeRequired } onChange={ this.changeHandler } />{' '}Does this Data Type have sub types?
							</Label>
						</FormGroup>
						<FormGroup check>
							<Label>
								<Input name="validationRequired" type="checkbox" defaultChecked={ this.state.form_data.validationRequired } onChange={ this.changeHandler } />{' '}Will this Data Type be validated?
							</Label>
						</FormGroup>
						<FormGroup>
							<Label for="schema_files">Schema Files:</Label>
							{ this.renderSchemaFiles(this.props.data.schemaFiles) }
						</FormGroup>
						<FormGroup>
							<Label for="data_subtypes">Sub Types:</Label>
							{ this.renderDataSubTypes(this.props.data.dataSubTypes) }
						</FormGroup>
						<Button type="submit">Submit</Button>
					</Form>
				</CardBody>
			</Card>

	
		);
	}
}

export default AdminEditDataType;
