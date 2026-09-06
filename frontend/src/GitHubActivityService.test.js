import api from "./api";
import { GitHubActivityService } from "./GitHubActivityService";

jest.mock("./api");

describe("GitHubActivityService Contract Tests", () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    test("Gọi đúng endpoint /integrations/github/activities và unwrap content mà không biến đổi DTO", async () => {
        const mockResponse = {
            data: {
                success: true,
                data: {
                    content: [
                        {
                            type: "COMMIT",
                            externalId: "a1b2c3d4e5f67890",
                            title: "feat: unified commit",
                            actorLogin: "developer1",
                            occurredAt: "2026-09-01T10:00:00Z",
                            htmlUrl:
                                "https://github.com/my-org/my-repo/commit/a1b2c3d4e5f67890",
                            issueKeys: ["CNPM-98"],
                            linkedTaskIds: [98],
                        },
                        {
                            type: "PULL_REQUEST",
                            externalId: "pr-12",
                            title: "feat: unified pr",
                            actorLogin: "developer2",
                            occurredAt: "2026-09-02T10:00:00Z",
                            htmlUrl:
                                "https://github.com/my-org/my-repo/pull/12",
                            issueKeys: ["CNPM-99"],
                            linkedTaskIds: [99],
                        },
                    ],
                },
            },
        };

        api.get.mockResolvedValueOnce(mockResponse);

        const result = await GitHubActivityService.getActivity(1);

        expect(api.get).toHaveBeenCalledWith(
            "/projects/1/integrations/github/activities",
        );
        expect(result.content).toHaveLength(2);
        expect(result.content[0].externalId).toBe("a1b2c3d4e5f67890");
        expect(result.content[0].title).toBe("feat: unified commit");
        expect(result.content[0].actorLogin).toBe("developer1");
        expect(result.content[0].occurredAt).toBe("2026-09-01T10:00:00Z");
    });
});
