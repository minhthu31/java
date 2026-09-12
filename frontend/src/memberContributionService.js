export const getBaseUrl = () =>
    process.env.REACT_APP_API_BASE_URL || "http://localhost:8080/api/v1";

const getAuthHeaders = () => {
    const token =
        localStorage.getItem("token") ||
        localStorage.getItem("accessToken") ||
        sessionStorage.getItem("token");

    return {
        Accept: "application/json",
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
    };
};

export const getProjectSprints = async (projectId) => {
    try {
        const response = await fetch(
            `${getBaseUrl()}/projects/${projectId}/reports/progress`,
            { headers: getAuthHeaders() },
        );
        if (!response.ok) return [];
        const contentType = response.headers.get("content-type");
        if (!contentType || !contentType.includes("application/json"))
            return [];
        const json = await response.json();
        const data = json.data || json;
        const rawSprints = data.sprints || [];

        return rawSprints.map((s) => ({
            sprintId: s.sprintId ?? s.id,
            sprintName:
                s.sprintName ?? s.name ?? `Sprint #${s.sprintId ?? s.id}`,
        }));
    } catch {
        return [];
    }
};

export const getMemberContributions = async (
    projectId,
    { sprintId, fromDate, toDate } = {},
) => {
    const params = new URLSearchParams();
    if (sprintId) params.append("sprintId", sprintId);
    if (fromDate) params.append("from", `${fromDate}T00:00:00Z`);
    if (toDate) {
        const nextDay = new Date(`${toDate}T00:00:00Z`);
        nextDay.setUTCDate(nextDay.getUTCDate() + 1);
        params.append("to", `${nextDay.toISOString().split("T")[0]}T00:00:00Z`);
    }

    const queryString = params.toString() ? `?${params.toString()}` : "";
    const url = `${getBaseUrl()}/projects/${projectId}/reports/summary${queryString}`;

    const response = await fetch(url, { headers: getAuthHeaders() });

    if (!response.ok) {
        if (response.status === 401)
            throw new Error("Phiên làm việc đã hết hạn (401).");
        if (response.status === 403)
            throw new Error(
                "Bạn không có quyền truy cập báo cáo thành viên (403).",
            );
        if (response.status === 404)
            throw new Error("Dịch vụ báo cáo thành viên chưa sẵn sàng (404).");
        throw new Error(
            `Máy chủ báo lỗi khi tải dữ liệu (${response.status}).`,
        );
    }

    const contentType = response.headers.get("content-type");
    if (!contentType || !contentType.includes("application/json")) {
        throw new Error(
            "Phản hồi máy chủ không hợp lệ (không phải định dạng JSON).",
        );
    }

    const json = await response.json();
    return json.data || json;
};
