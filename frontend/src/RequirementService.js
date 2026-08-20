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

const createHttpError = (status, message, fieldErrors = null) => {
    const error = new Error(message);
    error.status = status;
    if (fieldErrors) {
        error.fieldErrors = fieldErrors;
    }
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

const RequirementService = {
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

    getRequirementDetail: async (projectId, requirementId) => {
        if (!projectId || !requirementId) {
            throw createHttpError(
                400,
                "projectId và requirementId là bắt buộc",
            );
        }

        const endpoint = `${API_BASE_URL}/projects/${projectId}/requirements/${requirementId}`;
        const response = await fetch(endpoint, {
            method: "GET",
            headers: getAuthHeaders(),
        });

        if (!response.ok) {
            const message = await getErrorMessage(response);
            throw createHttpError(response.status, message);
        }

        const json = await response.json();
        return json.data || json;
    },

    createRequirement: async (projectId, data) => {
        if (!projectId) {
            throw createHttpError(400, "projectId là bắt buộc");
        }

        const payload = { ...data, status: "DRAFT" };
        const endpoint = `${API_BASE_URL}/projects/${projectId}/requirements`;
        const response = await fetch(endpoint, {
            method: "POST",
            headers: getAuthHeaders(),
            body: JSON.stringify(payload),
        });

        if (!response.ok) {
            let fieldErrors = null;
            let message = `Lỗi máy chủ (${response.status})`;
            try {
                const body = await response.json();
                message = body?.message || body?.error || message;
                fieldErrors = body?.fieldErrors || null;
            } catch {}
            throw createHttpError(response.status, message, fieldErrors);
        }

        const json = await response.json();
        return json.data || json;
    },

    updateRequirement: async (projectId, requirementId, data) => {
        if (!projectId || !requirementId) {
            throw createHttpError(
                400,
                "projectId và requirementId là bắt buộc",
            );
        }

        const {
            status,
            id,
            projectId: pId,
            createdAt,
            updatedAt,
            jiraIssueKey,
            ...payload
        } = data;
        const endpoint = `${API_BASE_URL}/projects/${projectId}/requirements/${requirementId}`;
        const response = await fetch(endpoint, {
            method: "PUT",
            headers: getAuthHeaders(),
            body: JSON.stringify(payload),
        });

        if (!response.ok) {
            let fieldErrors = null;
            let message = `Lỗi máy chủ (${response.status})`;
            try {
                const body = await response.json();
                message = body?.message || body?.error || message;
                fieldErrors = body?.fieldErrors || null;
            } catch {}
            throw createHttpError(response.status, message, fieldErrors);
        }

        const json = await response.json();
        return json.data || json;
    },
};

export { RequirementService };
export default RequirementService;
