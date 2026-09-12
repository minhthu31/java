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

describe("CNPM-107 MemberContributionComponent", () => {
    beforeEach(() => {
        jest.clearAllMocks();
        service.getProjectSprints.mockResolvedValue([
            { sprintId: 1, sprintName: "Sprint 1" },
        ]);
    });

    test("1. Render dung du lieu thanh vien theo backend contract (githubLinked = false)", async () => {
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
        });

        render(<MemberContributionComponent projectId={1} />);

        await waitFor(() => {
            expect(screen.getByText("Nguyễn Văn Test")).toBeInTheDocument();
            expect(screen.getByText("@member.test")).toBeInTheDocument();
            expect(screen.getByText("Chưa liên kết")).toBeInTheDocument();
            expect(screen.getByText("3")).toBeInTheDocument();
            expect(screen.getByText("5")).toBeInTheDocument();
            expect(screen.getByText("2")).toBeInTheDocument();
            expect(screen.queryByRole("link")).not.toBeInTheDocument();
        });
    });

    test("2. Render badge 'Đã liên kết' khi githubLinked = true", async () => {
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
            ],
        });

        render(<MemberContributionComponent projectId={1} />);

        await waitFor(() => {
            expect(screen.getByText("Nguyễn Văn Leader")).toBeInTheDocument();
            expect(screen.getByText("Đã liên kết")).toBeInTheDocument();
        });
    });

    test("3. Loc theo Sprint goi API voi dung sprintId", async () => {
        service.getMemberContributions.mockResolvedValue({
            memberContributions: [],
        });

        render(
            <MemberContributionComponent
                projectId={1}
                sprints={[{ sprintId: 10, sprintName: "Sprint 10" }]}
            />,
        );

        const select = await screen.findByRole("combobox", { name: /Sprint/i });
        fireEvent.change(select, { target: { value: "10" } });
        fireEvent.click(screen.getByRole("button", { name: /Áp dụng/i }));

        await waitFor(() => {
            expect(service.getMemberContributions).toHaveBeenCalledWith(
                1,
                expect.objectContaining({ sprintId: "10" }),
            );
        });
    });

    test("4. Nut Dat lai reset form va goi API voi bo loc rong", async () => {
        service.getMemberContributions.mockResolvedValue({
            memberContributions: [],
        });

        render(
            <MemberContributionComponent
                projectId={1}
                sprints={[{ sprintId: 10, sprintName: "Sprint 10" }]}
            />,
        );

        const select = await screen.findByRole("combobox", { name: /Sprint/i });
        fireEvent.change(select, { target: { value: "10" } });
        fireEvent.click(screen.getByRole("button", { name: /Đặt lại/i }));

        expect(select.value).toBe("");
        await waitFor(() => {
            expect(service.getMemberContributions).toHaveBeenCalledWith(1, {
                sprintId: undefined,
                fromDate: undefined,
                toDate: undefined,
            });
        });
    });

    test("5. Component truyen dung gia tri fromDate va toDate da chon vao service khi bam Ap dung", async () => {
        service.getMemberContributions.mockResolvedValue({
            memberContributions: [],
        });

        render(<MemberContributionComponent projectId={1} />);

        fireEvent.change(screen.getByLabelText("Từ ngày"), {
            target: { value: "2026-09-10" },
        });
        fireEvent.change(screen.getByLabelText("Đến ngày"), {
            target: { value: "2026-09-15" },
        });
        fireEvent.click(screen.getByRole("button", { name: /Áp dụng/i }));

        await waitFor(() => {
            expect(service.getMemberContributions).toHaveBeenCalledWith(
                1,
                expect.objectContaining({
                    fromDate: "2026-09-10",
                    toDate: "2026-09-15",
                }),
            );
        });
    });

    test("6. Hien thi thong bao loi khi API that bai", async () => {
        service.getMemberContributions.mockRejectedValueOnce(
            new Error("Dịch vụ báo cáo thành viên chưa sẵn sàng (404)."),
        );

        render(<MemberContributionComponent projectId={1} />);

        await waitFor(() => {
            expect(screen.getByRole("alert")).toHaveTextContent(
                "Dịch vụ báo cáo thành viên chưa sẵn sàng (404).",
            );
            expect(
                screen.getByRole("button", { name: /Thử lại/i }),
            ).toBeInTheDocument();
        });
    });

    test("7. Tu tai danh sach Sprint khi khong truyen sprints tu props", async () => {
        service.getMemberContributions.mockResolvedValue({
            memberContributions: [],
        });

        render(<MemberContributionComponent projectId={1} />);

        await waitFor(() => {
            expect(service.getProjectSprints).toHaveBeenCalledWith(1);
            expect(
                screen.getByRole("option", { name: "Sprint 1" }),
            ).toBeInTheDocument();
        });
    });
});
