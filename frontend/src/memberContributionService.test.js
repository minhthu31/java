import {
    getBaseUrl,
    getProjectSprints,
    getMemberContributions,
} from "./memberContributionService";

describe("memberContributionService Unit Tests", () => {
    beforeEach(() => {
        jest.clearAllMocks();
        localStorage.clear();
        sessionStorage.clear();
        global.fetch = jest.fn();
    });

    test("getBaseUrl trả về API base url mặc định", () => {
        expect(getBaseUrl()).toBe("http://localhost:8080/api/v1");
    });

    test("getProjectSprints trả về danh sách sprint chuẩn hóa khi API thành công", async () => {
        const mockData = {
            data: {
                sprints: [
                    { id: 10, name: "Sprint 10" },
                    { sprintId: 11, sprintName: "Sprint 11" },
                ],
            },
        };

        global.fetch.mockResolvedValueOnce({
            ok: true,
            status: 200,
            headers: {
                get: (header) =>
                    header.toLowerCase() === "content-type"
                        ? "application/json"
                        : null,
            },
            json: async () => mockData,
        });

        const sprints = await getProjectSprints(1);
        expect(sprints).toEqual([
            { sprintId: 10, sprintName: "Sprint 10" },
            { sprintId: 11, sprintName: "Sprint 11" },
        ]);
    });

    test("getProjectSprints ném lỗi phiên hết hạn khi gặp mã 401", async () => {
        global.fetch.mockResolvedValueOnce({
            ok: false,
            status: 401,
            headers: { get: () => null },
        });

        await expect(getProjectSprints(1)).rejects.toThrow(
            "Phiên làm việc đã hết hạn (401).",
        );
    });

    test("getMemberContributions gọi đúng query params và trả dữ liệu thành công", async () => {
        const mockSummary = {
            projectId: 1,
            taskMetrics: {},
            memberContributions: [{ memberId: 1, commits: 5 }],
        };

        global.fetch.mockResolvedValueOnce({
            ok: true,
            status: 200,
            headers: {
                get: (header) =>
                    header.toLowerCase() === "content-type"
                        ? "application/json"
                        : null,
            },
            json: async () => ({ data: mockSummary }),
        });

        const result = await getMemberContributions(1, {
            sprintId: "2",
            fromDate: "2026-09-01",
            toDate: "2026-09-10",
        });

        expect(global.fetch).toHaveBeenCalledWith(
            expect.stringContaining("/projects/1/reports/summary?sprintId=2"),
            expect.any(Object),
        );
        expect(result).toEqual(mockSummary);
    });

    test("getMemberContributions ném lỗi khi projectId không hợp lệ", async () => {
        await expect(getMemberContributions(null)).rejects.toThrow(
            "projectId không hợp lệ.",
        );
    });
});
