import React, { Component } from 'react';
import { NavLink as RRNavLink } from 'react-router-dom';

import { Container, Row, Col, Navbar, Nav, NavLink, NavItem } from 'reactstrap';

class Header extends Component {


render() {
    return (
      <header>
        <Navbar fixed="top" color="light" light expand="xs" className="border-bottom border-gray bg-white" style={{ height: 80 }}>
        
          <Container>
            <Row noGutters className="position-relative w-100 align-items-center">
            
              <Col>
                <Nav className="mrx-auto" navbar>
                   <NavItem ><NavLink tag={RRNavLink} to="/">Home</NavLink></NavItem>
                   <NavItem ><NavLink tag={RRNavLink} to="/datafiles">Data Files</NavLink></NavItem>
                   <NavItem ><NavLink tag={RRNavLink} to="/swagger-ui">Swagger API</NavLink></NavItem>
                   <NavItem className="align-items-center"><NavLink tag={RRNavLink} to="/myfiles">My Files</NavLink></NavItem>
                </Nav>
              </Col>

              <Col className="justify-content-end">
                <Nav navbar>
                   <NavItem><NavLink tag={RRNavLink} to="/login">Login</NavLink></NavItem>
                </Nav>
              </Col>

            </Row>
          </Container>
          
        </Navbar>
      </header>
    );
  }
  
}

export default Header;
