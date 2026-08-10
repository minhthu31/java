import React from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import Login from "./Login";
import Cnpm48RoleDashboard from "./Cnpm48RoleDashboard";

export default function App() {
    return (
        <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/dashboard" element={<Cnpm48RoleDashboard />} />
            <Route path="/admin/*" element={<Cnpm48RoleDashboard />} />
            <Route path="/lecturer/*" element={<Cnpm48RoleDashboard />} />
            <Route path="/leader/*" element={<Cnpm48RoleDashboard />} />
            <Route path="/member/*" element={<Cnpm48RoleDashboard />} />
            <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
    );
}
