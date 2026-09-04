import React from "react";
import "@testing-library/jest-dom";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import GitHubActivityComponent from "./GitHubActivityComponent";
import { GitHubActivityService } from "./GitHubActivityService";

jest.mock("./GitHubActivityService");

const mockActivityData = {
    commits: [
        {
            sha: "a1b2c3d4e5f67890",
            message: "feat: implement login page",
            authorName: "Nguyen Van A",
            committedAt: "2026-09-01T10:00:00Z",
            htmlUrl:
                "https://github.com/my-org/my-repo/commit/a1b2c3d4e5f67890",
            relatedTaskKey: "CNPM-101",
        },
        {
            sha: "b2c3d4e5f6789012",
            message: "fix: resolve token expire",
            authorName: "Tran Thi B",
            committedAt: "2026-09-02T14:30:00Z",
            htmlUrl:
                "https://github.com/my-org/my-repo/commit/b2c3d4e5f6789012",
            relatedTaskKey: null,
        },
    ],
    pullRequests: [
        {
            id: 1,
            number: 12,
            title: "Support GitHub Integration",
            authorName: "Nguyen Van A",
            status: "OPEN",
            createdAt: "2026-09-03T08:00:00Z",
            htmlUrl: "https://github.com/my-org/my-repo/pull/12",
            relatedTaskKey: "CNPM-102",
        },
        {
            id: 2,
            number: 11,
            title: "Database Migration Flyway",
            authorName: "Le Van C",
            status: "MERGED",
            createdAt: "2026-09-02T11:00:00Z",
            htmlUrl: "https://github.com/my-org/my-repo/pull/11",
            relatedTaskKey: "CNPM-99",
        },
    ],
};

describe("GitHubActivityComponent Acceptance Tests", () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    test("1. Hiển thị danh sách commit với SHA 7 ký tự, message, tác giả và Task liên quan", async () => {
        GitHubActivityService.getActivity.mockResolvedValueOnce(
            mockActivityData,
        );
        render(<GitHubActivityComponent projectId={1} />);

        await waitFor(() => {
            expect(screen.getByText("a1b2c3d")).toBeInTheDocument();
        });

        expect(
            screen.getByText("feat: implement login page"),
        ).toBeInTheDocument();
        expect(
            screen.getAllByText("Nguyen Van A").length,
        ).toBeGreaterThanOrEqual(1);
        expect(screen.getByText("CNPM-101")).toBeInTheDocument();

        const commitLink = screen.getByText("a1b2c3d").closest("a");
        expect(commitLink).toHaveAttribute(
            "href",
            "https://github.com/my-org/my-repo/commit/a1b2c3d4e5f67890",
        );
    });

    test("2. Hiển thị Pull Request với trạng thái, Task liên quan và URL thật", async () => {
        GitHubActivityService.getActivity.mockResolvedValueOnce(
            mockActivityData,
        );
        render(<GitHubActivityComponent projectId={1} />);

        await waitFor(() => {
            expect(screen.getByText("a1b2c3d")).toBeInTheDocument();
        });

        const prTabButton = screen.getByRole("button", {
            name: /Pull Requests/i,
        });
        fireEvent.click(prTabButton);

        expect(screen.getByText("OPEN")).toBeInTheDocument();
        expect(
            screen.getByText("#12 Support GitHub Integration"),
        ).toBeInTheDocument();
        expect(screen.getByText("CNPM-102")).toBeInTheDocument();

        const prLink = screen
            .getByText("#12 Support GitHub Integration")
            .closest("a");
        expect(prLink).toHaveAttribute(
            "href",
            "https://github.com/my-org/my-repo/pull/12",
        );
    });

    test("3. Bộ lọc hoạt động chính xác theo thành viên", async () => {
        GitHubActivityService.getActivity.mockResolvedValueOnce(
            mockActivityData,
        );
        render(<GitHubActivityComponent projectId={1} />);

        await waitFor(() => {
            expect(screen.getByText("a1b2c3d")).toBeInTheDocument();
        });

        const authorSelect = screen.getByRole("combobox", {
            name: /Filter by Author/i,
        });
        fireEvent.change(authorSelect, { target: { value: "Nguyen Van A" } });

        expect(
            screen.getByText("feat: implement login page"),
        ).toBeInTheDocument();
        expect(
            screen.queryByText("fix: resolve token expire"),
        ).not.toBeInTheDocument();
    });

    test("4. Hiển thị Empty state khi danh sách lọc không có kết quả", async () => {
        GitHubActivityService.getActivity.mockResolvedValueOnce(
            mockActivityData,
        );
        render(<GitHubActivityComponent projectId={1} />);

        await waitFor(() => {
            expect(screen.getByText("a1b2c3d")).toBeInTheDocument();
        });

        const authorSelect = screen.getByRole("combobox", {
            name: /Filter by Author/i,
        });
        fireEvent.change(authorSelect, { target: { value: "Tran Thi B" } });

        const prTabButton = screen.getByRole("button", {
            name: /Pull Requests/i,
        });
        fireEvent.click(prTabButton);

        expect(screen.getByTestId("empty-prs")).toHaveTextContent(
            "Không có Pull Request nào phù hợp với bộ lọc.",
        );
    });

    test("5. Hiển thị thông báo lỗi thân thiện và che giấu token/thông tin nhạy cảm khi API thất bại", async () => {
        GitHubActivityService.getActivity.mockRejectedValueOnce(
            new Error(
                "500 Internal Server Error: github_pat_secret_token_exposed",
            ),
        );
        render(<GitHubActivityComponent projectId={1} />);

        await waitFor(() => {
            expect(screen.getByTestId("error-banner")).toBeInTheDocument();
        });

        expect(
            screen.getByText(
                "Không thể tải dữ liệu hoạt động GitHub. Vui lòng thử lại sau.",
            ),
        ).toBeInTheDocument();
        expect(screen.queryByText(/github_pat_/i)).not.toBeInTheDocument();
    });
});
