import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { pathForRole } from './App';
import { login } from './authService';

export default function LoginPage() {
  const [form, setForm] = useState({ usernameOrEmail: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  async function submit(event) {
    event.preventDefault();
    setError('');

    if (!form.usernameOrEmail.trim()) {
      setError('Vui lòng nhập Username hoặc Email');
      return;
    }

    if (!form.password) {
      setError('Vui lòng nhập mật khẩu');
      return;
    }

    setLoading(true);

    try {
      const user = await login({
        usernameOrEmail: form.usernameOrEmail.trim(),
        password: form.password,
      });

      localStorage.setItem('accessToken', user.accessToken);
      localStorage.setItem('currentUser', JSON.stringify(user));
      navigate(pathForRole(user.role), { replace: true });
    } catch (requestError) {
      const status = requestError.response?.status;

      if (status === 401) {
        setError('Username/Email hoặc mật khẩu không đúng');
      } else if (status === 403) {
        setError('Tài khoản không được phép đăng nhập');
      } else if (status === 400) {
        setError('Thông tin đăng nhập không hợp lệ');
      } else {
        setError(
          requestError.response?.data?.message
            || 'Không thể kết nối đến hệ thống. Vui lòng thử lại.',
        );
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="login-page">
      <section className="login-introduction" aria-label="Giới thiệu hệ thống">
        <div className="introduction-content">
          <h1>Jira &amp; GitHub Manager</h1>
          <p>
            Công cụ hỗ trợ quản lý yêu cầu và tiến độ dự án phần mềm thông
            qua Jira và GitHub.
          </p>
          <div className="integration" aria-label="Tích hợp Jira và GitHub">
            <span>Jira</span>
            <span className="integration-line">----</span>
            <span aria-hidden="true">🔗</span>
            <span className="integration-line">----</span>
            <span>GitHub</span>
          </div>
        </div>
      </section>

      <section className="login-container">
        <form className="login-form" onSubmit={submit} noValidate>
          <h2>Đăng nhập</h2>
          <p className="login-description">Đăng nhập để tiếp tục sử dụng hệ thống</p>

          <div className="form-group">
            <label htmlFor="usernameOrEmail">
              Username hoặc Email <span aria-hidden="true">*</span>
            </label>
            <input
              id="usernameOrEmail"
              type="text"
              autoComplete="username"
              placeholder="Nhập Username hoặc Email"
              value={form.usernameOrEmail}
              onChange={(event) => setForm({ ...form, usernameOrEmail: event.target.value })}
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">
              Password <span aria-hidden="true">*</span>
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              placeholder="Nhập mật khẩu"
              value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })}
              disabled={loading}
            />
          </div>

          <div className="forgot-password">
            <button type="button" disabled={loading}>
              Quên mật khẩu?
            </button>
          </div>

          {error && (
            <div className="login-error" role="alert">
              {error}
            </div>
          )}

          <button type="submit" className="login-button" disabled={loading}>
            {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
          </button>
        </form>
      </section>
    </main>
  );
}
