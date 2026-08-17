import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";
import RequirementList from "./RequirementList";
import { RequirementService } from "./RequirementService";

jest.mock("./RequirementService");

describe("CNPM-63: RequirementList Tests", () => {
    const mockPageData = {
        content: [
            {
                id: 1,
                jiraIssueKey: "REQ-01",
                title: "Xác thực người dùng",
                actor: "Sinh viên",
                description: "Đăng nhập JWT",
                priority: "HIGH",
                status: "APPROVED",
                updatedAt: "2026-08-15T10:00:00Z",
            },
        ],
        totalPages: 1,
        totalElements: 1,
        size: 20,
        number: 0,
    };

    beforeEach(() => {
        jest.clearAllMocks();
    });

    test("1. Hiển thị title, actor, priority và status từ API data.content", async () => {
        RequirementService.getRequirements.mockResolvedValue(mockPageData);
        render(<RequirementList projectId={1} currentUserRole="TEAM_LEADER" />);

        expect(await screen.findByText("REQ-01")).toBeInTheDocument();
        expect(screen.getByText("Xác thực người dùng")).toBeInTheDocument();
        expect(screen.getByText("Sinh viên")).toBeInTheDocument();
        expect(screen.getByRole("cell", { name: "HIGH" })).toBeInTheDocument();
        expect(
            screen.getByRole("cell", { name: "APPROVED" }),
        ).toBeInTheDocument();
    });

    test("2. Team Leader thấy nút tạo và sửa", async () => {
        RequirementService.getRequirements.mockResolvedValue(mockPageData);
        render(<RequirementList projectId={1} currentUserRole="TEAM_LEADER" />);

        expect(screen.getByText("+ Tạo Requirement")).toBeInTheDocument();
        expect(await screen.findByText("Sửa")).toBeInTheDocument();
    });

    test("3. Lecturer chỉ xem, không thấy nút tạo/sửa", async () => {
        RequirementService.getRequirements.mockResolvedValue(mockPageData);
        render(<RequirementList projectId={1} currentUserRole="LECTURER" />);

        await screen.findByText("REQ-01");
        expect(screen.queryByText("+ Tạo Requirement")).not.toBeInTheDocument();
        expect(screen.queryByText("Sửa")).not.toBeInTheDocument();
    });

    test.each(["ADMIN", "STUDENT", "TEAM_MEMBER"])(
        "4. Role %s không được truy cập và không gọi API",
        async (role) => {
            render(<RequirementList projectId={1} currentUserRole={role} />);

            expect(
                await screen.findByText(/không có quyền truy cập/i),
            ).toBeInTheDocument();
            expect(RequirementService.getRequirements).not.toHaveBeenCalled();
        },
    );

    test("5. Không có projectId -> không gọi API", async () => {
        render(
            <RequirementList projectId={null} currentUserRole="TEAM_LEADER" />,
        );

        expect(
            await screen.findByText(/Không xác định được project/i),
        ).toBeInTheDocument();
        expect(RequirementService.getRequirements).not.toHaveBeenCalled();
    });

    test("6. Lỗi API 500 hiển thị Error State và nút Thử lại", async () => {
        const error = new Error("Lỗi máy chủ (500)");
        error.status = 500;
        RequirementService.getRequirements.mockRejectedValue(error);

        render(<RequirementList projectId={1} currentUserRole="TEAM_LEADER" />);

        expect(
            await screen.findByText("Lỗi máy chủ (500)"),
        ).toBeInTheDocument();
        expect(screen.getByText("Thử lại")).toBeInTheDocument();
    });

    test("7. Hiển thị Empty State khi không có Requirement nào", async () => {
        RequirementService.getRequirements.mockResolvedValue({
            content: [],
            totalPages: 0,
            totalElements: 0,
            size: 20,
            number: 0,
        });

        render(<RequirementList projectId={1} currentUserRole="LECTURER" />);

        expect(
            await screen.findByText("Không có Requirement nào."),
        ).toBeInTheDocument();
    });

    test("8. Tìm kiếm gửi đúng query params", async () => {
        RequirementService.getRequirements.mockResolvedValue(mockPageData);
        render(<RequirementList projectId={1} currentUserRole="TEAM_LEADER" />);

        await screen.findByText("REQ-01");

        fireEvent.change(screen.getByPlaceholderText("Tìm từ khóa"), {
            target: { value: "login" },
        });
        fireEvent.change(screen.getByPlaceholderText("Ví dụ: CNPM-63"), {
            target: { value: "CNPM-63" },
        });

        fireEvent.click(screen.getByText("Tìm kiếm"));

        await waitFor(() => {
            expect(RequirementService.getRequirements).toHaveBeenLastCalledWith(
                1,
                expect.objectContaining({
                    keyword: "login",
                    jiraIssueKey: "CNPM-63",
                    page: 0,
                    size: 20,
                    sort: "updatedAt,desc",
                }),
            );
        });
    });
});
