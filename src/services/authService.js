import axios from "axios";

const login = async (usernameOrEmail, password) => {
    const response = await axios.post(
        "/api/v1/auth/login",
        {
            usernameOrEmail,
            password
        }
    );

    return response.data;
};

export default login;
