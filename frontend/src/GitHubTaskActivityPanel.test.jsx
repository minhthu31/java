import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";
import { GitHubTaskActivityPanel } from "./GitHubTaskActivityPanel";
import { GitHubActivityService } from "./GitHubActivityService";

jest.mock("./GitHubActivityService");

describe("GitHubTaskActivityPanel", () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test("hiển thị commit và Pull Request liên kết với Task", async () => {
        GitHubActivityService.getTaskActivities.mockResolvedValueOnce({
            content: [
                {
                    type: "COMMIT",
                    key: "abc1234",
                    summary: "feat(CNPM-101): demo",
                    actorLogin: "member",
                    url: "https://github.com/example/repo/commit/abc1234",
                },
                {
                    type: "PULL_REQUEST",
                    key: "101",
                    summary: "CNPM-101 integration",
                    actorLogin: "member",
                    url: "https://github.com/example/repo/pull/101",
                },
            ],
        });

        render(<GitHubTaskActivityPanel projectId={1} taskId={101} />);

        await waitFor(() => {
            expect(screen.getByText("feat(CNPM-101): demo")).toBeInTheDocument();
            expect(screen.getByText("CNPM-101 integration")).toBeInTheDocument();
        });
        expect(GitHubActivityService.getTaskActivities).toHaveBeenCalledWith(1, 101, {
            page: 0,
            size: 20,
        });
    });

    test("hiển thị trạng thái rỗng an toàn", async () => {
        GitHubActivityService.getTaskActivities.mockResolvedValueOnce({ content: [] });

        render(<GitHubTaskActivityPanel projectId={1} taskId={101} />);

        expect(
            await screen.findByText("Chưa có commit hoặc Pull Request liên kết."),
        ).toBeInTheDocument();
    });
});
