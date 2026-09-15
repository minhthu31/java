import axios from "axios";

const getBaseUrl = () =>
    process.env.REACT_APP_API_BASE_URL || "http://localhost:8080/api/v1";

const api = axios.create({
    baseURL: getBaseUrl(),
});

api.interceptors.request.use((config) => {
    const token = localStorage.getItem("accessToken");
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export default api;
