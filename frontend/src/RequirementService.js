const API_BASE_URL =
    process.env.REACT_APP_API_BASE_URL || "http://localhost:8080/api/v1";

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

const createHttpError = (status, message, fieldErrors = null) => {
    const error = new Error(message);
    error.status = status;
    error.fieldErrors = fieldErrors;
    return error;
};

const handleResponse = async (response) => {
    let body = null;
    try {
        body = await response.json();
    } catch {
        body = null;
    }

    if (!response.ok) {
        const message =
            body?.message ||
            body?.error ||
            body?.data?.message ||
            `Lỗi máy chủ (${response.status})`;
        const fieldErrors = body?.fieldErrors || body?.errors || null;
        throw createHttpError(response.status, message, fieldErrors);
    }

    return body?.data !== undefined ? body.data : body;
};

export const RequirementService = {
    // GET list
    getRequirements: async (projectId, params = {}) => {
        if (!projectId) throw createHttpError(400, "projectId is required");

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
        if (keyword.trim()) query.append("keyword", keyword.trim());
        if (status.trim()) query.append("status", status.trim());
        if (priority.trim()) query.append("priority", priority.trim());
        if (jiraIssueKey.trim())
            query.append("jiraIssueKey", jiraIssueKey.trim());
        query.append("page", String(page));
        query.append("size", String(size));
        if (sort) query.append("sort", sort);

        const endpoint = `${API_BASE_URL}/projects/${projectId}/requirements?${query.toString()}`;
        const response = await fetch(endpoint, {
            method: "GET",
            headers: getAuthHeaders(),
        });

        const data = await handleResponse(response);
        if (!data || !Array.isArray(data.content)) {
            throw createHttpError(
                500,
                "Response API không đúng cấu trúc data.content.",
            );
        }
        return data;
    },

    // GET by ID (CNPM-64)
    getRequirementById: async (projectId, requirementId) => {
        if (!projectId || !requirementId) {
            throw createHttpError(
                400,
                "projectId and requirementId are required",
            );
        }
        const endpoint = `${API_BASE_URL}/projects/${projectId}/requirements/${requirementId}`;
        const response = await fetch(endpoint, {
            method: "GET",
            headers: getAuthHeaders(),
        });
        return await handleResponse(response);
    },

    // POST create (CNPM-64) -> status chỉ là DRAFT
    createRequirement: async (projectId, requirementData) => {
        if (!projectId) throw createHttpError(400, "projectId is required");

        const payload = {
            ...requirementData,
            status: "DRAFT",
        };

        const endpoint = `${API_BASE_URL}/projects/${projectId}/requirements`;
        const response = await fetch(endpoint, {
            method: "POST",
            headers: getAuthHeaders(),
            body: JSON.stringify(payload),
        });
        return await handleResponse(response);
    },

    // PUT update (CNPM-64) -> KHÔNG gửi status qua PUT
    updateRequirement: async (projectId, requirementId, requirementData) => {
        if (!projectId || !requirementId) {
            throw createHttpError(
                400,
                "projectId and requirementId are required",
            );
        }

        const { status, id, createdAt, updatedAt, ...updatePayload } =
            requirementData;

        const endpoint = `${API_BASE_URL}/projects/${projectId}/requirements/${requirementId}`;
        const response = await fetch(endpoint, {
            method: "PUT",
            headers: getAuthHeaders(),
            body: JSON.stringify(updatePayload),
        });
        return await handleResponse(response);
    },
};

export default RequirementService;
