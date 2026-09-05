import api from "./api";
import { GitHubActivityService } from "./GitHubActivityService";

jest.mock("./api");

describe("GitHubActivityService Contract Tests", () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    test("Gọi đúng endpoint /activities và unwrap content mà không biến đổi DTO", async () => {
        const mockBackendResponse = {
            data: {
                content: [
                    {
                        type: "COMMIT",
                        sha: "c1a2b3d4e5f67890",
                        message: "feat: sync backend logic",
                        authorName: "Nguyen Van A",
                    },
                    {
                        type: "PULL_REQUEST",
                        number: 15,
                        title: "PR sync backend",
                        authorName: "Tran Thi B",
                        status: "OPEN",
                    },
                ],
            },
        };

        api.get.mockResolvedValueOnce(mockBackendResponse);

        const result = await GitHubActivityService.getActivity(1);

        expect(api.get).toHaveBeenCalledWith(
            "/api/v1/projects/1/integrations/github/activities",
        );
        expect(result.content).toHaveLength(2);
        expect(result.content[0].type).toBe("COMMIT");
        expect(result.content[1].type).toBe("PULL_REQUEST");
    });
});
