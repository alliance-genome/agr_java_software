import React, { Fragment, Component } from 'react';
import { Col } from 'reactstrap';
//import axios from 'axios';

//import DataTypes from '../../components/AdminTypes';

class Admin extends Component {

    render() {
        return (
            <Fragment>
                <Col xs={{ order: 2 }} md={{ size: 4, order: 1 }} tag="aside" className="pb-5 mb-5 pb-md-0 mb-md-0 mx-auto mx-md-0">
                    
                </Col>
                               
                <Col xs={{ order: 1 }} md={{ size: 7, offset: 1 }} tag="section" className="py-5 mb-5 py-md-0 mb-md-0">
                    
                </Col>
            </Fragment>
        )
    }
}

export default Admin;