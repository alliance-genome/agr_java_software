import React, { Component, Fragment } from 'react';
import axios from 'axios';
import { CardBody, Card } from 'reactstrap';
import DataTypeList from './DataTypeList';

class SideCard extends Component {

  state = { datatypes: null }

  componentDidMount() {
    axios.get('http://localhost:8080/api/datatype/all')
      .then(response => {
        this.setState({ datatypes: response.data });
      });
  }

  render() {

    return (
      <Fragment>
        { this.state.datatypes && <Card>
          <CardBody>
            Data Types:
            <DataTypeList data={this.state.datatypes} />
          </CardBody>
        </Card> }
      </Fragment>
    );
  }
}

export default SideCard;
