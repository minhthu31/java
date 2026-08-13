import { useNavigate } from "react-router-dom";

function DashboardPage() {
    const navigate = useNavigate();

    const user = JSON.parse(
        localStorage.getItem("user")
    );

    const handleLogout = () => {
        localStorage.removeItem("user");

        navigate("/login");
    };

    return (
        <div
            style={{
                padding: "40px",
                fontFamily: "Arial, sans-serif"
            }}
        >
            <h1>Đăng nhập thành công</h1>

            {user && (
                <div>

                    <p>
                        <strong>Username:</strong>{" "}
                        {user.username}
                    </p>

                    <p>
                        <strong>Email:</strong>{" "}
                        {user.email}
                    </p>

                    <p>
                        <strong>Họ tên:</strong>{" "}
                        {user.fullName}
                    </p>

                    <p>
                        <strong>Role:</strong>{" "}
                        {user.role}
                    </p>

                </div>
            )}

            <button onClick={handleLogout}>
                Đăng xuất
            </button>
        </div>
    );
}

export default DashboardPage;
