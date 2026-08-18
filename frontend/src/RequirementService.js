const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || "/api/v1";

const getAuthHeaders = () => {
    const token =
        localStorage.getItem("token") ||
        localStorage.getItem("accessToken") ||
        localStorage.getItem("jwtToken") ||
        localStorage.getItem("jwt");

    const headers = {
        Accept: "application/json",
        "Content-Type": "application/json",
    };

    if (token) {
        headers.Authorization = token.startsWith("Bearer ")
            ? token
            : `Bearer ${token}`;
    }

    return headers;
};

const createHttpError = (status, message) => {
    const error = new Error(message);
    error.status = status;
    return error;
};

const getErrorMessage = async (response) => {
    try {
        const body = await response.json();
        return (
            body?.message ||
            body?.error ||
            body?.data?.message ||
            `Lỗi máy chủ (${response.status})`
        );
    } catch {
        return `Lỗi máy chủ (${response.status})`;
    }
};

export const RequirementService = {
    getRequirements: async (projectId, params = {}) => {
        if (
            projectId === undefined ||
            projectId === null ||
            projectId === "" ||
            Number(projectId) <= 0
        ) {
            throw createHttpError(
                400,
                "projectId is required and must be valid",
            );
        }

        const {
            keyword = "",
            status = "",
            priority = "",
            jiraIssueKey = "",
            page = 0,
            size = 20,
            sort = "updatedAt,desc",
        } = params;

        const query = new URLSearchParams();

        if (keyword.trim()) {
            query.append("keyword", keyword.trim());
        }
        if (status.trim()) {
            query.append("status", status.trim());
        }
        if (priority.trim()) {
            query.append("priority", priority.trim());
        }
        if (jiraIssueKey.trim()) {
            query.append("jiraIssueKey", jiraIssueKey.trim());
        }

        query.append("page", String(page));
        query.append("size", String(size));

        if (sort) {
            query.append("sort", sort);
        }

        const endpoint =
            `${API_BASE_URL}/projects/${projectId}/requirements` +
            `?${query.toString()}`;

        const response = await fetch(endpoint, {
            method: "GET",
            headers: getAuthHeaders(),
        });

        if (!response.ok) {
            const message = await getErrorMessage(response);
            throw createHttpError(response.status, message);
        }

        const json = await response.json();

        if (!json || !json.data || !Array.isArray(json.data.content)) {
            throw createHttpError(
                500,
                "Response API không đúng cấu trúc data.content.",
            );
        }

        return json.data;
    },
};

export default RequirementService;
