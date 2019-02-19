import React, { Component, Fragment } from 'react';
import axios from 'axios';
import { Button, Card, CardImg, CardBody, CardTitle, CardSubtitle, CardText } from 'reactstrap';

const BANNER = 'https://i.imgur.com/CaKdFMq.jpg';

class SideCard extends Component {

  state = { datatypes: null }

  componentDidMount() {
    axios.get('http://localhost:8080/api/datatype/all')
      .then(response => {
        this.setState({ datatypes: response.data });
        console.log(this.state.datatypes.map(type => ({name: type.name})));
      });
  }
 
  render() {

    let element = (
      this.state.datatypes != null ? this.state.datatypes.toString() : ""
    )

    return (
      <Fragment>
        { this.state.datatypes && <Card>
          <CardImg top width="100%" src={BANNER} alt="banner" />
          <CardBody>
            <CardTitle className="h3 mb-2 pt-2 font-weight-bold text-secondary">Glad Chinda</CardTitle>
            <CardSubtitle className="text-secondary mb-3 font-weight-light text-uppercase" style={{ fontSize: '0.8rem' }}>Web Developer, Lagos</CardSubtitle>
            <CardText className="text-secondary mb-4" style={{ fontSize: '0.75rem' }}>This is text</CardText>
            <Button color="success" className="font-weight-bold">View Profile</Button>
          </CardBody>
        </Card> }
      </Fragment>
    );
  }
}

export default SideCard;
