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
            dataStatus: "COMPLETE",
            warnings: [],
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
                    linkedTasks: 3,
                },
            ],
            dataStatus: "COMPLETE",
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
            dataStatus: "COMPLETE",
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

    test("4. Cho phép lọc 1 ngày duy nhất (fromDate === toDate) và chặn khi fromDate > toDate", async () => {
        render(<MemberContributionComponent projectId={1} />);

        await waitFor(() => {
            expect(screen.getByText("Sprint 1")).toBeInTheDocument();
            expect(
                screen.getByText(/Chưa ghi nhận thông tin đóng góp nào/),
            ).toBeInTheDocument();
        });

        service.getMemberContributions.mockClear();

        fireEvent.change(screen.getByLabelText("Từ ngày"), {
            target: { value: "2026-09-15" },
        });
        fireEvent.change(screen.getByLabelText("Đến ngày"), {
            target: { value: "2026-09-15" },
        });
        fireEvent.click(screen.getByRole("button", { name: /Áp dụng/i }));

        await waitFor(() => {
            expect(service.getMemberContributions).toHaveBeenCalledTimes(1);
            expect(
                screen.getByText(/Chưa ghi nhận thông tin đóng góp nào/),
            ).toBeInTheDocument();
        });

        fireEvent.change(screen.getByLabelText("Từ ngày"), {
            target: { value: "2026-09-16" },
        });
        fireEvent.change(screen.getByLabelText("Đến ngày"), {
            target: { value: "2026-09-15" },
        });
        fireEvent.click(screen.getByRole("button", { name: /Áp dụng/i }));

        expect(
            screen.getByText("Ngày bắt đầu không được lớn hơn ngày kết thúc."),
        ).toBeInTheDocument();
        expect(service.getMemberContributions).toHaveBeenCalledTimes(1);
    });

    test("5. Hiển thị cảnh báo đồng bộ khi dataStatus không phải COMPLETE hoặc có warnings", async () => {
        service.getMemberContributions.mockResolvedValueOnce({
            dataStatus: "PARTIAL",
            warnings: ["GitHub sync is in progress"],
            memberContributions: [],
        });

        render(<MemberContributionComponent projectId={1} />);

        await waitFor(() => {
            expect(
                screen.getByTestId("sync-warning-banner"),
            ).toBeInTheDocument();
            expect(screen.getByText(/PARTIAL/i)).toBeInTheDocument();
            expect(
                screen.getByText(/GitHub sync is in progress/i),
            ).toBeInTheDocument();
        });
    });

    test("6. Chống race condition khi bấm lọc liên tiếp: chỉ giữ kết quả của request mới nhất", async () => {
        let resolveSlowRequest;
        const slowPromise = new Promise((resolve) => {
            resolveSlowRequest = resolve;
        });
        const fastPromise = Promise.resolve({
            dataStatus: "COMPLETE",
            memberContributions: [
                {
                    memberId: 101,
                    fullName: "Dữ liệu Mới Nhất",
                    linkedTasks: 5,
                    commits: 5,
                    pullRequests: 5,
                },
            ],
        });

        render(<MemberContributionComponent projectId={1} />);
        await waitFor(() =>
            expect(screen.getByText("Sprint 1")).toBeInTheDocument(),
        );

        service.getMemberContributions.mockReturnValueOnce(slowPromise);
        fireEvent.click(screen.getByRole("button", { name: /Áp dụng/i }));

        service.getMemberContributions.mockReturnValueOnce(fastPromise);
        fireEvent.click(screen.getByRole("button", { name: /Áp dụng/i }));

        await waitFor(() => {
            expect(screen.getByText("Dữ liệu Mới Nhất")).toBeInTheDocument();
        });

        resolveSlowRequest({
            dataStatus: "COMPLETE",
            memberContributions: [
                {
                    memberId: 99,
                    fullName: "Dữ liệu Cũ Lạc Hậu",
                    linkedTasks: 0,
                    commits: 0,
                    pullRequests: 0,
                },
            ],
        });

        await waitFor(() => {
            expect(
                screen.queryByText("Dữ liệu Cũ Lạc Hậu"),
            ).not.toBeInTheDocument();
            expect(screen.getByText("Dữ liệu Mới Nhất")).toBeInTheDocument();
        });
    });
});
