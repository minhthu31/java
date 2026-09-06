import api from "./api";
import { GitHubActivityService } from "./GitHubActivityService";

jest.mock("./api");

describe("GitHubActivityService Contract Tests", () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    test("Gửi đúng query params và unwrap content theo đúng DTO task 98", async () => {
        const mockResponse = {
            data: {
                timestamp: "2026-09-02T10:00:00Z",
                data: {
                    content: [
                        {
                            type: "COMMIT",
                            key: "sha98",
                            summary: "feat(CNPM-98): test commit activity",
                            actorUserId: 1,
                            actorLogin: "member98",
                            timestamp: "2026-09-02T10:00:00Z",
                            url: "https://github.com/minhthu31/java-backend/commit/sha98",
                            issueKeys: ["CNPM-98"],
                            linkedTaskIds: [9893],
                        },
                    ],
                    totalPages: 1,
                    first: true,
                    last: true,
                },
            },
        };

        api.get.mockResolvedValueOnce(mockResponse);

        const params = {
            type: "COMMIT",
            actorUserId: 1,
            issueKey: "CNPM-98",
            from: "2026-09-01T00:00:00Z",
            to: "2026-09-03T00:00:00Z",
            page: 0,
            size: 10,
        };

        const result = await GitHubActivityService.getActivity(1, params);

        expect(api.get).toHaveBeenCalledWith(
            "/projects/1/integrations/github/activities",
            { params },
        );
        expect(result.content).toHaveLength(1);
        expect(result.content[0].key).toBe("sha98");
        expect(result.content[0].summary).toBe(
            "feat(CNPM-98): test commit activity",
        );
        expect(result.content[0].actorLogin).toBe("member98");
        expect(result.content[0].timestamp).toBe("2026-09-02T10:00:00Z");
        expect(result.content[0].url).toBe(
            "https://github.com/minhthu31/java-backend/commit/sha98",
        );
        expect(result.content[0].issueKeys).toEqual(["CNPM-98"]);
        expect(result.content[0].linkedTaskIds).toEqual([9893]);
    });
});
