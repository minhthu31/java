import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { pathForRole } from "./App";
import { login } from "./authService";

export default function LoginPage() {
    const [form, setForm] = useState({ usernameOrEmail: "", password: "" });
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    async function submit(event) {
        event.preventDefault();
        setError("");

        if (!form.usernameOrEmail.trim()) {
            setError("Vui lòng nhập Username hoặc Email");
            return;
        }

        if (!form.password) {
            setError("Vui lòng nhập mật khẩu");
            return;
        }

        setLoading(true);

        try {
            const user = await login({
                usernameOrEmail: form.usernameOrEmail.trim(),
                password: form.password,
            });

            localStorage.setItem("accessToken", user.accessToken);
            localStorage.setItem("currentUser", JSON.stringify(user));
            navigate(pathForRole(user.role), { replace: true });
        } catch (requestError) {
            const status = requestError.response?.status;

            if (status === 401) {
                setError("Username/Email hoặc mật khẩu không đúng");
            } else if (status === 403) {
                setError("Tài khoản không được phép đăng nhập");
            } else if (status === 400) {
                setError("Thông tin đăng nhập không hợp lệ");
            } else {
                setError(
                    requestError.response?.data?.message ||
                        "Không thể kết nối đến hệ thống. Vui lòng thử lại.",
                );
            }
        } finally {
            setLoading(false);
        }
    }

    return (
        <main
            className="login-page"
            style={{
                minHeight: "100vh",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                backgroundColor: "#172554",
                padding: "24px",
                boxSizing: "border-box",
                fontFamily:
                    "'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif",
            }}
        >
            <div
                style={{
                    width: "100%",
                    maxWidth: "980px",
                    minHeight: "580px",
                    backgroundColor: "#ffffff",
                    borderRadius: "32px",
                    display: "flex",
                    overflow: "hidden",
                    boxShadow: "0 25px 50px -12px rgba(15, 23, 42, 0.65)",
                }}
            >
                <section
                    className="login-container"
                    style={{
                        width: "42%",
                        padding: "48px 42px",
                        display: "flex",
                        flexDirection: "column",
                        justifyContent: "space-between",
                        backgroundColor: "#ffffff",
                        boxSizing: "border-box",
                    }}
                >
                    <div>
                        <div
                            style={{
                                fontSize: "13px",
                                fontWeight: 800,
                                color: "#1d4ed8",
                                letterSpacing: "0.8px",
                                textTransform: "uppercase",
                            }}
                        >
                            CNPM Portal
                        </div>
                    </div>

                    <form
                        className="login-form"
                        onSubmit={submit}
                        noValidate
                        style={{ margin: "20px 0" }}
                    >
                        <h2
                            style={{
                                margin: "0 0 6px",
                                fontSize: "26px",
                                fontWeight: 700,
                                color: "#0f172a",
                                letterSpacing: "-0.5px",
                            }}
                        >
                            Đăng nhập
                        </h2>
                        <p
                            className="login-description"
                            style={{
                                margin: "0 0 24px",
                                fontSize: "13px",
                                color: "#64748b",
                            }}
                        >
                            Đăng nhập để tiếp tục sử dụng hệ thống
                        </p>

                        {error && (
                            <div
                                className="login-error"
                                role="alert"
                                style={{
                                    padding: "11px 16px",
                                    marginBottom: "18px",
                                    backgroundColor: "#fef2f2",
                                    border: "1px solid #fecaca",
                                    borderRadius: "12px",
                                    color: "#b91c1c",
                                    fontSize: "12px",
                                    fontWeight: 500,
                                    lineHeight: 1.4,
                                }}
                            >
                                {error}
                            </div>
                        )}

                        <div
                            className="form-group"
                            style={{ marginBottom: "16px" }}
                        >
                            <label
                                htmlFor="usernameOrEmail"
                                style={{
                                    display: "block",
                                    fontSize: "11px",
                                    fontWeight: 700,
                                    textTransform: "uppercase",
                                    letterSpacing: "0.5px",
                                    color: "#475569",
                                    marginBottom: "6px",
                                }}
                            >
                                Username hoặc Email{" "}
                                <span
                                    aria-hidden="true"
                                    style={{ color: "#ef4444" }}
                                >
                                    *
                                </span>
                            </label>
                            <input
                                id="usernameOrEmail"
                                type="text"
                                autoComplete="username"
                                placeholder="Nhập Username hoặc Email"
                                value={form.usernameOrEmail}
                                onChange={(event) =>
                                    setForm({
                                        ...form,
                                        usernameOrEmail: event.target.value,
                                    })
                                }
                                disabled={loading}
                                style={{
                                    width: "100%",
                                    padding: "12px 18px",
                                    borderRadius: "50px",
                                    border: "1.5px solid #cbd5e1",
                                    fontSize: "13px",
                                    color: "#0f172a",
                                    outline: "none",
                                    boxSizing: "border-box",
                                    transition: "border-color 0.2s ease",
                                    backgroundColor: loading
                                        ? "#f8fafc"
                                        : "#ffffff",
                                }}
                                onFocus={(e) =>
                                    (e.target.style.borderColor = "#2563eb")
                                }
                                onBlur={(e) =>
                                    (e.target.style.borderColor = "#cbd5e1")
                                }
                            />
                        </div>

                        <div
                            className="form-group"
                            style={{ marginBottom: "14px" }}
                        >
                            <label
                                htmlFor="password"
                                style={{
                                    display: "block",
                                    fontSize: "11px",
                                    fontWeight: 700,
                                    textTransform: "uppercase",
                                    letterSpacing: "0.5px",
                                    color: "#475569",
                                    marginBottom: "6px",
                                }}
                            >
                                Password{" "}
                                <span
                                    aria-hidden="true"
                                    style={{ color: "#ef4444" }}
                                >
                                    *
                                </span>
                            </label>
                            <input
                                id="password"
                                type="password"
                                autoComplete="current-password"
                                placeholder="Nhập mật khẩu"
                                value={form.password}
                                onChange={(event) =>
                                    setForm({
                                        ...form,
                                        password: event.target.value,
                                    })
                                }
                                disabled={loading}
                                style={{
                                    width: "100%",
                                    padding: "12px 18px",
                                    borderRadius: "50px",
                                    border: "1.5px solid #cbd5e1",
                                    fontSize: "13px",
                                    color: "#0f172a",
                                    outline: "none",
                                    boxSizing: "border-box",
                                    transition: "border-color 0.2s ease",
                                    backgroundColor: loading
                                        ? "#f8fafc"
                                        : "#ffffff",
                                }}
                                onFocus={(e) =>
                                    (e.target.style.borderColor = "#2563eb")
                                }
                                onBlur={(e) =>
                                    (e.target.style.borderColor = "#cbd5e1")
                                }
                            />
                        </div>

                        <div
                            className="forgot-password"
                            style={{
                                display: "flex",
                                justifyContent: "flex-end",
                                marginBottom: "20px",
                            }}
                        >
                            <button
                                type="button"
                                disabled={loading}
                                style={{
                                    background: "none",
                                    border: "none",
                                    padding: 0,
                                    fontSize: "12px",
                                    color: "#2563eb",
                                    cursor: loading ? "not-allowed" : "pointer",
                                    fontWeight: 500,
                                }}
                            >
                                Quên mật khẩu?
                            </button>
                        </div>

                        <button
                            type="submit"
                            className="login-button"
                            disabled={loading}
                            style={{
                                width: "100%",
                                padding: "13px",
                                borderRadius: "50px",
                                border: "none",
                                backgroundColor: "#1e3a8a",
                                color: "#ffffff",
                                fontSize: "13px",
                                fontWeight: 700,
                                letterSpacing: "0.6px",
                                cursor: loading ? "not-allowed" : "pointer",
                                boxShadow: "0 4px 14px rgba(30, 58, 138, 0.35)",
                                transition: "background 0.2s ease",
                            }}
                            onMouseOver={(e) => {
                                if (!loading)
                                    e.currentTarget.style.backgroundColor =
                                        "#172554";
                            }}
                            onMouseOut={(e) => {
                                if (!loading)
                                    e.currentTarget.style.backgroundColor =
                                        "#1e3a8a";
                            }}
                        >
                            {loading ? "Đang đăng nhập..." : "ĐĂNG NHẬP"}
                        </button>
                    </form>

                    <div
                        style={{
                            display: "flex",
                            gap: "6px",
                            justifyContent: "center",
                        }}
                    >
                        <span
                            style={{
                                width: "6px",
                                height: "6px",
                                borderRadius: "50%",
                                backgroundColor: "#0f172a",
                            }}
                        ></span>
                        <span
                            style={{
                                width: "6px",
                                height: "6px",
                                borderRadius: "50%",
                                backgroundColor: "#cbd5e1",
                            }}
                        ></span>
                        <span
                            style={{
                                width: "6px",
                                height: "6px",
                                borderRadius: "50%",
                                backgroundColor: "#cbd5e1",
                            }}
                        ></span>
                    </div>
                </section>

                <section
                    className="login-introduction"
                    aria-label="Giới thiệu hệ thống"
                    style={{
                        flex: 1,
                        position: "relative",
                        background:
                            "radial-gradient(at 12% 18%, rgba(254, 240, 138, 0.55) 0px, transparent 50%), " +
                            "radial-gradient(at 88% 12%, rgba(147, 197, 253, 0.6) 0px, transparent 55%), " +
                            "radial-gradient(at 50% 85%, rgba(30, 64, 175, 0.95) 0px, transparent 70%), " +
                            "linear-gradient(135deg, #1e40af 0%, #172554 100%)",
                        display: "flex",
                        flexDirection: "column",
                        justifyContent: "space-between",
                        padding: "48px 56px",
                        color: "#ffffff",
                        boxSizing: "border-box",
                    }}
                >
                    <div
                        style={{
                            display: "flex",
                            justifyContent: "flex-end",
                            gap: "24px",
                            fontSize: "11px",
                            fontWeight: 700,
                            letterSpacing: "0.8px",
                            color: "rgba(255, 255, 255, 0.75)",
                        }}
                    >
                        <span style={{ cursor: "pointer" }}>GIỚI THIỆU</span>
                        <span style={{ cursor: "pointer" }}>TÀI LIỆU</span>
                        <span style={{ cursor: "pointer" }}>HỖ TRỢ</span>
                    </div>

                    <div className="introduction-content">
                        <h1
                            style={{
                                fontSize: "54px",
                                fontWeight: 800,
                                lineHeight: 1.1,
                                margin: "0 0 16px",
                                letterSpacing: "-1.2px",
                                color: "#ffffff",
                            }}
                        >
                            Welcome.
                        </h1>
                        <p
                            style={{
                                margin: "0 0 32px",
                                fontSize: "14px",
                                lineHeight: 1.6,
                                color: "rgba(255, 255, 255, 0.85)",
                                maxWidth: "420px",
                            }}
                        >
                            Công cụ hỗ trợ quản lý yêu cầu và tiến độ dự án phần
                            mềm thông qua Jira và GitHub
                        </p>

                        <div
                            style={{
                                display: "flex",
                                justifyContent: "center",
                                width: "100%",
                            }}
                        >
                            <div
                                className="integration"
                                aria-label="Tích hợp Jira và GitHub"
                                style={{
                                    display: "inline-flex",
                                    alignItems: "center",
                                    gap: "18px",
                                    padding: "12px 28px",
                                    backgroundColor:
                                        "rgba(255, 255, 255, 0.15)",
                                    backdropFilter: "blur(12px)",
                                    borderRadius: "50px",
                                    border: "1.5px solid rgba(255, 255, 255, 0.25)",
                                    boxShadow:
                                        "0 8px 24px rgba(15, 23, 42, 0.25)",
                                    fontSize: "15px",
                                    fontWeight: 700,
                                    color: "#ffffff",
                                    letterSpacing: "0.3px",
                                }}
                            >
                                <span>Jira</span>
                                <span
                                    className="integration-line"
                                    style={{
                                        color: "rgba(255, 255, 255, 0.45)",
                                    }}
                                >
                                    —
                                </span>
                                <span
                                    style={{
                                        color: "#93c5fd",
                                        textTransform: "uppercase",
                                        fontSize: "13px",
                                        letterSpacing: "1px",
                                    }}
                                >
                                    SYNC
                                </span>
                                <span
                                    className="integration-line"
                                    style={{
                                        color: "rgba(255, 255, 255, 0.45)",
                                    }}
                                >
                                    —
                                </span>
                                <span>GitHub</span>
                            </div>
                        </div>
                    </div>

                    <div></div>
                </section>
            </div>
        </main>
    );
}
