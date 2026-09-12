import * as service from "./memberContributionService";

describe("memberContributionService Unit Tests", () => {
    const originalEnv = process.env;

    beforeEach(() => {
        process.env = { ...originalEnv };
        global.fetch = jest.fn();
        localStorage.clear();
    });

    afterAll(() => {
        process.env = originalEnv;
    });

    test("1. Cận trên toDate được chuyển thành 00:00:00Z của ngày kế tiếp (+1 ngày)", async () => {
        global.fetch.mockResolvedValueOnce({
            ok: true,
            headers: {
                get: (header) =>
                    header.toLowerCase() === "content-type"
                        ? "application/json"
                        : null,
            },
            json: async () => ({ data: { memberContributions: [] } }),
        });

        await service.getMemberContributions(1, {
            fromDate: "2026-09-10",
            toDate: "2026-09-15",
        });

        expect(global.fetch).toHaveBeenCalledTimes(1);
        const calledUrl = global.fetch.mock.calls[0][0];

        expect(calledUrl).toContain("from=2026-09-10T00%3A00%3A00Z");
        expect(calledUrl).toContain("to=2026-09-16T00%3A00%3A00Z");
    });

    test("2. Sử dụng đúng REACT_APP_API_BASE_URL và đính kèm Bearer token", async () => {
        process.env.REACT_APP_API_BASE_URL = "http://test-api:8080/api/v1";
        localStorage.setItem("token", "mock-token-xyz");

        global.fetch.mockResolvedValueOnce({
            ok: true,
            headers: {
                get: (header) =>
                    header.toLowerCase() === "content-type"
                        ? "application/json"
                        : null,
            },
            json: async () => ({ memberContributions: [] }),
        });

        await service.getMemberContributions(5, { sprintId: "12" });

        const [calledUrl, options] = global.fetch.mock.calls[0];
        expect(calledUrl).toContain(
            "http://test-api:8080/api/v1/projects/5/reports/summary?sprintId=12",
        );
        expect(options.headers.Authorization).toBe("Bearer mock-token-xyz");
    });

    test("3. Ném lỗi chi tiết khi gặp mã 401", async () => {
        global.fetch.mockResolvedValueOnce({
            ok: false,
            status: 401,
        });

        await expect(service.getMemberContributions(1)).rejects.toThrow(
            "Phiên làm việc đã hết hạn (401).",
        );
    });

    test("4. Ném lỗi chi tiết khi gặp mã 403", async () => {
        global.fetch.mockResolvedValueOnce({
            ok: false,
            status: 403,
        });

        await expect(service.getMemberContributions(1)).rejects.toThrow(
            "Bạn không có quyền truy cập báo cáo thành viên (403).",
        );
    });

    test("5. Ném lỗi chi tiết khi gặp mã 404", async () => {
        global.fetch.mockResolvedValueOnce({
            ok: false,
            status: 404,
        });

        await expect(service.getMemberContributions(1)).rejects.toThrow(
            "Dịch vụ báo cáo thành viên chưa sẵn sàng (404).",
        );
    });

    test("6. Ném lỗi khi phản hồi máy chủ không phải JSON", async () => {
        global.fetch.mockResolvedValueOnce({
            ok: true,
            headers: {
                get: (header) =>
                    header.toLowerCase() === "content-type"
                        ? "text/html"
                        : null,
            },
            json: async () => ({}),
        });

        await expect(service.getMemberContributions(1)).rejects.toThrow(
            "Phản hồi máy chủ không hợp lệ (không phải định dạng JSON).",
        );
    });

    test("7. getProjectSprints bóc tách và chuẩn hóa danh sách sprints từ progress API", async () => {
        global.fetch.mockResolvedValueOnce({
            ok: true,
            headers: {
                get: (header) =>
                    header.toLowerCase() === "content-type"
                        ? "application/json"
                        : null,
            },
            json: async () => ({
                data: {
                    sprints: [{ id: 1, name: "Sprint 1" }],
                },
            }),
        });

        const sprints = await service.getProjectSprints(1);
        expect(sprints).toEqual([{ sprintId: 1, sprintName: "Sprint 1" }]);
    });
});
