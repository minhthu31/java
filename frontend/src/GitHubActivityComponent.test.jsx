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

const mockEndOfDayCommit = {
    content: [
        {
            type: "COMMIT",
            key: "shaEndOfDay99999",
            summary: "feat(CNPM-99): commit phát sinh cuối ngày",
            actorUserId: 1,
            actorLogin: "member98",
            timestamp: "2026-09-05T23:30:00Z",
            url: "https://github.com/minhthu31/java-backend/commit/shaEndOfDay99999",
            issueKeys: ["CNPM-99"],
            linkedTaskIds: [9999],
        },
    ],
    totalPages: 1,
    first: true,
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

    test("3. Chỉ gọi API khi bấm nút Lọc, không gọi khi đang gõ phím; param issueKey và actorUserId dạng số", async () => {
        GitHubActivityService.getActivity.mockResolvedValue(mockCommitPage0);
        render(<GitHubActivityComponent projectId={1} />);

        await waitFor(() => {
            expect(screen.getByText("sha98ab")).toBeInTheDocument();
        });

        expect(GitHubActivityService.getActivity).toHaveBeenCalledTimes(1);

        const jiraInput = screen.getByLabelText(/Mã Jira:/i);
        fireEvent.change(jiraInput, { target: { value: "CNPM-98" } });

        const actorInput = screen.getByLabelText(/User ID:/i);
        fireEvent.change(actorInput, { target: { value: "1" } });

        expect(GitHubActivityService.getActivity).toHaveBeenCalledTimes(1);

        const searchButton = screen.getByRole("button", { name: /Lọc/i });
        fireEvent.click(searchButton);

        await waitFor(() => {
            expect(GitHubActivityService.getActivity).toHaveBeenCalledTimes(2);
            expect(GitHubActivityService.getActivity).toHaveBeenLastCalledWith(
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

    test("5. Lọc theo ngày kết thúc chuyển thành cuối ngày (23:59:59.999) và hiển thị hoạt động cuối ngày", async () => {
        GitHubActivityService.getActivity
            .mockResolvedValueOnce(mockCommitPage0)
            .mockResolvedValueOnce(mockEndOfDayCommit);

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
                    to: expect.stringMatching(/23:59:59\.999Z$/),
                    page: 0,
                    size: 10,
                }),
            );
            expect(
                screen.getByText("feat(CNPM-99): commit phát sinh cuối ngày"),
            ).toBeInTheDocument();
            expect(screen.getByText("shaEndO")).toBeInTheDocument();
        });
    });

    test("6. Đổi projectId khi đang ở trang sau: Không gọi page khác 0 cho project mới và ngăn request cũ ghi đè", async () => {
        let resolveProject1Page1;
        const project1Page1Promise = new Promise((resolve) => {
            resolveProject1Page1 = resolve;
        });

        GitHubActivityService.getActivity.mockImplementation(
            (projId, params) => {
                if (projId === 1 && params.page === 0) {
                    return Promise.resolve(mockCommitPage0);
                }
                if (projId === 1 && params.page === 1) {
                    return project1Page1Promise;
                }
                if (projId === 2) {
                    return Promise.resolve({
                        content: [
                            {
                                type: "COMMIT",
                                key: "shaProject2Success",
                                summary:
                                    "feat(PROJ-2): commit mới của project 2",
                                actorUserId: 2,
                                actorLogin: "member2",
                                timestamp: "2026-09-06T10:00:00Z",
                                url: "https://github.com/test/project2/commit/shaProject2Success",
                                issueKeys: ["PROJ-2"],
                                linkedTaskIds: [202],
                            },
                        ],
                        totalPages: 1,
                        first: true,
                        last: true,
                    });
                }
                return Promise.reject(new Error("Unknown call"));
            },
        );

        const { rerender } = render(<GitHubActivityComponent projectId={1} />);

        await waitFor(() => {
            expect(screen.getByText("sha98ab")).toBeInTheDocument();
        });

        const nextButton = screen.getByRole("button", { name: /Trang sau/i });
        fireEvent.click(nextButton);

        rerender(<GitHubActivityComponent projectId={2} />);

        await waitFor(() => {
            expect(
                screen.getByText("feat(PROJ-2): commit mới của project 2"),
            ).toBeInTheDocument();
        });

        resolveProject1Page1(mockCommitPage1);

        await new Promise((r) => setTimeout(r, 50));
        expect(
            screen.getByText("feat(PROJ-2): commit mới của project 2"),
        ).toBeInTheDocument();
        expect(
            screen.queryByText("feat(CNPM-9): test commit nine"),
        ).not.toBeInTheDocument();

        const callsForProject2 =
            GitHubActivityService.getActivity.mock.calls.filter(
                (call) => call[0] === 2,
            );
        expect(callsForProject2.length).toBeGreaterThanOrEqual(1);

        callsForProject2.forEach((call) => {
            expect(call[1].page).toBe(0);
            expect(call[1].page).not.toBe(1);
        });

        expect(
            GitHubActivityService.getActivity.mock.calls.some(
                ([projId, params]) => projId === 2 && params.page !== 0,
            ),
        ).toBe(false);
    });
});
