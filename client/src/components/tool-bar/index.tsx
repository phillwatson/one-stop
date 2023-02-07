import Container from 'react-bootstrap/Container';
import Nav from 'react-bootstrap/Nav';
import Navbar from 'react-bootstrap/Navbar';
import './tool-bar.css';

export default function ToolBar() {
  return (
    <Navbar>
      <Container>
        <Nav className="toolbar">
          <Nav.Link className="item" href="/about">About</Nav.Link>
          <Nav.Link className="item" href="/institutions">Institutions</Nav.Link>
          <Nav.Link className="item" href="/sign-up">Sign Up</Nav.Link>
        </Nav>
      </Container>
    </Navbar>
  );
};
