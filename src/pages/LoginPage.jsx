import { useState } from "react";
import { useNavigate } from "react-router-dom";
import login from "../services/authService";

function LoginPage() {
    const [usernameOrEmail, setUsernameOrEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();

        setError("");

        // Validate Username / Email
        if (!usernameOrEmail.trim()) {
            setError("Vui lòng nhập Username hoặc Email");
            return;
        }

        // Validate Password
        if (!password) {
            setError("Vui lòng nhập mật khẩu");
            return;
        }

        try {
            setLoading(true);

            const result = await login(
                usernameOrEmail.trim(),
                password
            );

            switch (result.role) {
                case "ADMIN":
                    navigate("/admin");
                    break;

                case "LECTURER":
                    navigate("/lecturer");
                    break;

                case "TEAM_LEADER":
                    navigate("/team-leader");
                    break;

                case "TEAM_MEMBER":
                    navigate("/team-member");
                    break;

                default:
                    setError(
                        "Không xác định được vai trò tài khoản"
                    );
            }

        } catch (error) {

            if (error.response?.status === 401) {
                setError(
                    "Username/Email hoặc mật khẩu không đúng"
                );
            } else if (error.response?.status === 403) {
                setError(
                    "Tài khoản không được phép đăng nhập"
                );
            } else if (error.response?.status === 400) {
                setError(
                    "Thông tin đăng nhập không hợp lệ"
                );
            } else {
                setError(
                    "Không thể đăng nhập. Vui lòng thử lại."
                );
            }

        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-page">

            <div className="login-introduction">

                <h1>Jira & GitHub Manager</h1>

                <p>
                    Công cụ hỗ trợ quản lý yêu cầu và tiến độ
                    dự án phần mềm thông qua Jira và GitHub.
                </p>

                <div className="integration">
                    <span>Jira</span>
                    <span>----</span>
                    <span>🔗</span>
                    <span>----</span>
                    <span>GitHub</span>
                </div>

            </div>

            <div className="login-container">

                <form
                    className="login-form"
                    onSubmit={handleSubmit}
                >

                    <h2>Đăng nhập</h2>

                    <p className="login-description">
                        Đăng nhập để tiếp tục sử dụng hệ thống
                    </p>

                    <div className="form-group">

                        <label htmlFor="usernameOrEmail">
                            Username hoặc Email
                            <span>*</span>
                        </label>

                        <input
                            id="usernameOrEmail"
                            type="text"
                            placeholder="Nhập Username hoặc Email"
                            value={usernameOrEmail}
                            onChange={(e) =>
                                setUsernameOrEmail(e.target.value)
                            }
                            disabled={loading}
                        />

                    </div>

                    <div className="form-group">

                        <label htmlFor="password">
                            Password
                            <span>*</span>
                        </label>

                        <input
                            id="password"
                            type="password"
                            placeholder="Nhập mật khẩu"
                            value={password}
                            onChange={(e) =>
                                setPassword(e.target.value)
                            }
                            disabled={loading}
                        />

                    </div>

                    <div className="forgot-password">

                        <button
                            type="button"
                            onClick={() => {}}
                        >
                            Quên mật khẩu?
                        </button>

                    </div>

                    {error && (
                        <div
                            className="login-error"
                            role="alert"
                        >
                            {error}
                        </div>
                    )}

                    <button
                        type="submit"
                        className="login-button"
                        disabled={loading}
                    >
                        {loading
                            ? "Đang đăng nhập..."
                            : "Đăng nhập"}
                    </button>

                </form>

            </div>

        </div>
    );
}

export default LoginPage;

export default LoginPage;
