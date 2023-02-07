import React from "react";
import ToolBar from "./components/tool-bar";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Home from "./pages";
import About from "./pages/about";
import Institutions from "./pages/institutions";
import SignIn from "./pages/sign-in";

export default function App() {
  return (
    <Router>
      <ToolBar />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/about" element={<About />} />
        <Route path="/institutions" element={<Institutions />} />
        <Route path="/sign-in" element={<SignIn />} />
      </Routes>
    </Router>
  );
}
