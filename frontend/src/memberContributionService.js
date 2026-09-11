const API_BASE_URL = "/api/v1";

export const getMemberContributions = async (
    projectId,
    { sprintId, fromDate, toDate } = {},
) => {
    const token =
        localStorage.getItem("token") ||
        localStorage.getItem("accessToken") ||
        sessionStorage.getItem("token");

    const params = new URLSearchParams();
    if (sprintId) params.append("sprintId", sprintId);
    if (fromDate) params.append("from", `${fromDate}T00:00:00Z`);
    if (toDate) params.append("to", `${toDate}T23:59:59Z`);

    const queryString = params.toString() ? `?${params.toString()}` : "";
    const url = `${API_BASE_URL}/projects/${projectId}/reports/summary${queryString}`;

    const response = await fetch(url, {
        headers: {
            Accept: "application/json",
            "Content-Type": "application/json",
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
    });

    const contentType = response.headers.get("content-type");
    if (
        !response.ok ||
        !contentType ||
        !contentType.includes("application/json")
    ) {
        return null;
    }

    const result = await response.json();
    return result.data || result;
};
