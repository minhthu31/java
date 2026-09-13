import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";
import MemberContributionComponent from "./MemberContributionComponent";
import * as service from "./memberContributionService";

jest.mock("./memberContributionService", () => ({
    __esModule: true,
    getProjectSprints: jest.fn(),
    getMemberContributions: jest.fn(),
}));

describe("CNPM-107 MemberContributionComponent Tests", () => {
    beforeEach(() => {
        jest.clearAllMocks();
        service.getProjectSprints.mockResolvedValue([
            { sprintId: 1, sprintName: "Sprint 1" },
        ]);
        service.getMemberContributions.mockResolvedValue({
            memberContributions: [],
        });
    });

    test("1. Hiển thị đúng dữ liệu thành viên, cột 'Tasks liên kết' và badge 'Chưa liên kết'", async () => {
        service.getMemberContributions.mockResolvedValueOnce({
            memberContributions: [
                {
                    memberId: 7,
                    username: "member.test",
                    fullName: "Nguyễn Văn Test",
                    githubLinked: false,
                    commits: 5,
                    pullRequests: 2,
                    openPullRequests: 1,
                    closedPullRequests: 1,
                    mergedPullRequests: 0,
                    linkedTasks: 3,
                },
            ],
        });

        render(<MemberContributionComponent projectId={1} />);

        await waitFor(() => {
            expect(screen.getByText("Tasks liên kết")).toBeInTheDocument();
            expect(screen.getByText("Nguyễn Văn Test")).toBeInTheDocument();
            expect(screen.getByText("@member.test")).toBeInTheDocument();
            expect(screen.getByText("Chưa liên kết")).toBeInTheDocument();
            expect(screen.getByText("3")).toBeInTheDocument();
            expect(screen.getByText("5")).toBeInTheDocument();
            expect(screen.getByText("2")).toBeInTheDocument();
        });
    });

    test("2. Tương thích CNPM-105: Hiển thị 'Đã liên kết' hoặc username qua githubLinked / githubUsername", async () => {
        service.getMemberContributions.mockResolvedValueOnce({
            memberContributions: [
                {
                    memberId: 8,
                    username: "leader.test",
                    fullName: "Nguyễn Văn Leader",
                    githubLinked: true,
                    commits: 10,
                    pullRequests: 4,
                    linkedTasks: 6,
                },
                {
                    memberId: 9,
                    username: "user.git",
                    fullName: "Trần GitHub",
                    githubLinked: false,
                    githubUsername: "trangit",
                    commits: 4,
                    pullRequests: 1,
                    linkedTasks: 2,
                },
            ],
        });

        render(<MemberContributionComponent projectId={1} />);

        await waitFor(() => {
            expect(screen.getByText("Nguyễn Văn Leader")).toBeInTheDocument();
            expect(screen.getByText("Đã liên kết")).toBeInTheDocument();
            expect(screen.getByText("@trangit")).toBeInTheDocument();
        });
    });

    test("3. Khắc phục vòng lặp: getProjectSprints chỉ được gọi đúng 1 lần khi không truyền prop sprints", async () => {
        render(<MemberContributionComponent projectId={1} />);

        await waitFor(() => {
            expect(service.getProjectSprints).toHaveBeenCalledTimes(1);
            expect(screen.getByText("Sprint 1")).toBeInTheDocument();
        });
    });

    test("4. Chặn gọi API và hiển thị thông báo lỗi khi fromDate >= toDate", async () => {
        render(<MemberContributionComponent projectId={1} />);

        // Đợi lần fetch khởi tạo ban đầu hoàn tất
        await waitFor(() => {
            expect(screen.getByText("Sprint 1")).toBeInTheDocument();
        });

        service.getMemberContributions.mockClear();

        fireEvent.change(screen.getByLabelText("Từ ngày"), {
            target: { value: "2026-09-15" },
        });
        fireEvent.change(screen.getByLabelText("Đến ngày"), {
            target: { value: "2026-09-10" },
        });
        fireEvent.click(screen.getByRole("button", { name: /Áp dụng/i }));

        expect(
            screen.getByText("Ngày bắt đầu phải nhỏ hơn ngày kết thúc."),
        ).toBeInTheDocument();

        // Đảm bảo không có request mới nào bị bắn lên
        expect(service.getMemberContributions).not.toHaveBeenCalled();
    });

    test("5. Kiểm tra component xử lý và hiển thị đúng theo schema ReportSummaryResponse", async () => {
        const backendPayload = {
            projectId: 1,
            sprintId: null,
            memberId: null,
            from: null,
            to: null,
            asOf: "2026-09-13T00:00:00Z",
            taskMetrics: {
                totalTasks: 2,
                completedTasks: 1,
                overdueTasks: 0,
                statusBreakdown: { DONE: 1, IN_PROGRESS: 1 },
            },
            memberContributions: [
                {
                    memberId: 1,
                    username: "leader.test",
                    fullName: "Test Team Leader",
                    githubLinked: true,
                    commits: 8,
                    pullRequests: 3,
                    openPullRequests: 1,
                    closedPullRequests: 0,
                    mergedPullRequests: 2,
                    linkedTasks: 2,
                },
            ],
            dataStatus: "COMPLETE",
            sources: [],
            warnings: [],
        };

        service.getMemberContributions.mockResolvedValueOnce(backendPayload);

        render(<MemberContributionComponent projectId={1} />);

        await waitFor(() => {
            expect(screen.getByText("Test Team Leader")).toBeInTheDocument();
            expect(screen.getByText("@leader.test")).toBeInTheDocument();
            expect(screen.getByText("Đã liên kết")).toBeInTheDocument();
            expect(screen.getByText("2")).toBeInTheDocument();
            expect(screen.getByText("8")).toBeInTheDocument();
        });
    });
});
