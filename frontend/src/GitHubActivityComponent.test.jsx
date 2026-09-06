import React from "react";
import "@testing-library/jest-dom";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { GitHubActivityComponent } from "./GitHubActivityComponent";
import { GitHubActivityService } from "./GitHubActivityService";

jest.mock("./GitHubActivityService");

const mockCommitPage0 = {
    content: [
        {
            type: "COMMIT",
            key: "sha98abcdef123456",
            summary: "feat(CNPM-98): test commit activity",
            actorUserId: 1,
            actorLogin: "member98",
            timestamp: "2026-09-02T10:00:00Z",
            url: "https://github.com/minhthu31/java-backend/commit/sha98abcdef123456",
            issueKeys: ["CNPM-98"],
            linkedTaskIds: [9893],
        },
    ],
    totalPages: 2,
    first: true,
    last: false,
};

const mockPrPage0 = {
    content: [
        {
            type: "PULL_REQUEST",
            key: "10",
            summary: "feat(CNPM-98): test PR activity",
            actorUserId: 1,
            actorLogin: "member98",
            timestamp: "2026-09-02T11:00:00Z",
            url: "https://github.com/minhthu31/java-backend/pull/10",
            issueKeys: ["CNPM-98"],
            linkedTaskIds: [9893],
        },
    ],
    totalPages: 1,
    first: true,
    last: true,
};

const mockCommitPage1 = {
    content: [
        {
            type: "COMMIT",
            key: "sha9000000000000",
            summary: "feat(CNPM-9): test commit nine",
            actorUserId: 1,
            actorLogin: "member98",
            timestamp: "2026-09-02T12:00:00Z",
            url: "https://github.com/minhthu31/java-backend/commit/sha9000000000000",
            issueKeys: ["CNPM-9"],
            linkedTaskIds: [9894],
        },
    ],
    totalPages: 2,
    first: false,
    last: true,
};

describe("GitHubActivityComponent Acceptance Tests (Task 98 DTO)", () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    test("1. Hiển thị Commit với key rút gọn 7 ký tự, summary, actorLogin, timestamp và link url", async () => {
        GitHubActivityService.getActivity.mockResolvedValueOnce(
            mockCommitPage0,
        );
        render(<GitHubActivityComponent projectId={1} />);

        await waitFor(() => {
            expect(screen.getByText("sha98ab")).toBeInTheDocument();
        });

        expect(
            screen.getByText("feat(CNPM-98): test commit activity"),
        ).toBeInTheDocument();
        expect(screen.getAllByText("member98").length).toBeGreaterThanOrEqual(
            1,
        );
        expect(screen.getAllByText("CNPM-98").length).toBeGreaterThanOrEqual(1);

        const commitLink = screen.getByText("sha98ab").closest("a");
        expect(commitLink).toHaveAttribute(
            "href",
            "https://github.com/minhthu31/java-backend/commit/sha98abcdef123456",
        );
    });

    test("2. Chuyển tab sang Pull Request: Gọi API param PULL_REQUEST và render đầy đủ key, summary, author, jira, url", async () => {
        GitHubActivityService.getActivity
            .mockResolvedValueOnce(mockCommitPage0)
            .mockResolvedValueOnce(mockPrPage0);

        render(<GitHubActivityComponent projectId={1} />);

        await waitFor(() => {
            expect(screen.getByText("sha98ab")).toBeInTheDocument();
        });

        const prTab = screen.getByRole("button", { name: /Pull Requests/i });
        fireEvent.click(prTab);

        await waitFor(() => {
            expect(GitHubActivityService.getActivity).toHaveBeenCalledWith(
                1,
                expect.objectContaining({ type: "PULL_REQUEST" }),
            );
            expect(screen.getByText("#10")).toBeInTheDocument();
            expect(
                screen.getByText("feat(CNPM-98): test PR activity"),
            ).toBeInTheDocument();
        });

        expect(screen.getAllByText("member98").length).toBeGreaterThanOrEqual(
            1,
        );
        expect(screen.getAllByText("CNPM-98").length).toBeGreaterThanOrEqual(1);

        const prLink = screen.getByText("#10").closest("a");
        expect(prLink).toHaveAttribute(
            "href",
            "https://github.com/minhthu31/java-backend/pull/10",
        );
    });

    test("3. Tìm kiếm theo mã Jira issueKey và actorUserId dạng số gọi lại API với query params chuẩn", async () => {
        GitHubActivityService.getActivity.mockResolvedValue(mockCommitPage0);
        render(<GitHubActivityComponent projectId={1} />);

        const jiraInput = screen.getByLabelText(/Mã Jira:/i);
        fireEvent.change(jiraInput, { target: { value: "CNPM-98" } });

        const actorInput = screen.getByLabelText(/User ID:/i);
        fireEvent.change(actorInput, { target: { value: "1" } });

        const searchButton = screen.getByRole("button", { name: /Lọc/i });
        fireEvent.click(searchButton);

        await waitFor(() => {
            expect(GitHubActivityService.getActivity).toHaveBeenCalledWith(
                1,
                expect.objectContaining({
                    issueKey: "CNPM-98",
                    actorUserId: 1,
                }),
            );
        });
    });

    test("4. Phân trang: Bấm 'Trang sau' gọi API page = 1 và hiển thị dữ liệu mới của trang 2", async () => {
        GitHubActivityService.getActivity
            .mockResolvedValueOnce(mockCommitPage0)
            .mockResolvedValueOnce(mockCommitPage1);

        render(<GitHubActivityComponent projectId={1} />);

        await waitFor(() => {
            expect(screen.getByText("Trang 1 / 2")).toBeInTheDocument();
            expect(
                screen.getByText("feat(CNPM-98): test commit activity"),
            ).toBeInTheDocument();
        });

        const nextButton = screen.getByRole("button", { name: /Trang sau/i });
        fireEvent.click(nextButton);

        await waitFor(() => {
            expect(GitHubActivityService.getActivity).toHaveBeenCalledWith(
                1,
                expect.objectContaining({ page: 1 }),
            );
            expect(
                screen.getByText("feat(CNPM-9): test commit nine"),
            ).toBeInTheDocument();
            expect(screen.getByText("sha9000")).toBeInTheDocument();
        });
    });
});
test("5. Lọc theo khoảng thời gian gửi from và to lên API", async () => {
    GitHubActivityService.getActivity.mockResolvedValue(mockCommitPage0);

    render(<GitHubActivityComponent projectId={1} />);

    await waitFor(() => {
        expect(screen.getByText("sha98ab")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText(/Từ:/i), {
        target: { value: "2026-09-01" },
    });

    fireEvent.change(screen.getByLabelText(/Đến:/i), {
        target: { value: "2026-09-05" },
    });

    fireEvent.click(screen.getByRole("button", { name: /Lọc/i }));

    await waitFor(() => {
        expect(GitHubActivityService.getActivity).toHaveBeenLastCalledWith(
            1,
            expect.objectContaining({
                type: "COMMIT",
                from: expect.any(String),
                to: expect.any(String),
                page: 0,
                size: 10,
            }),
        );
    });
});
