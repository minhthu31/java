import { Navigate } from "react-router-dom";

function ProtectedRoute({ allowedRole, children }) {
    const user = JSON.parse(localStorage.getItem("user"));

    // Chưa đăng nhập
    if (!user) {
        return <Navigate to="/login" replace />;
    }

    // Sai role
    if (user.role !== allowedRole) {
        return <Navigate to="/unauthorized" replace />;
    }

    return children;
}

export default ProtectedRoute;
