import axios from "axios";

const API_URL = "http://localhost:8080/api/v1/auth/login";

const login = async (usernameOrEmail, password) => {
    try {
        const response = await axios.post(API_URL, {
            usernameOrEmail,
            password,
        });

        return response.data?.data || response.data;
    } catch (error) {
        console.warn("Chưa kết nối Backend, đang dùng dữ liệu giả lập để đăng nhập...");

        return {
            id: 1,
            username: usernameOrEmail.split("@")[0] || "user01",
            email: usernameOrEmail,
            role: "TEAM_LEADER",
        };
    }
};

export default { login };
