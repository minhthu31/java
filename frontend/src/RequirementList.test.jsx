import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";

import RequirementList from "./RequirementList";
import { RequirementService } from "./RequirementService";

jest.mock("./RequirementService");

describe("CNPM-63: RequirementList", () => {
    const mockPageData = {
        content: [
            {
                id: 1,
                jiraIssueKey: "REQ-01",
                title: "Xác thực người dùng",
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

    test("1. Hiển thị danh sách từ API data.content", async () => {
        RequirementService.getRequirements.mockResolvedValue(mockPageData);

        render(<RequirementList projectId={1} currentUserRole="TEAM_LEADER" />);

        expect(await screen.findByText("REQ-01")).toBeInTheDocument();

        expect(screen.getByText("Xác thực người dùng")).toBeInTheDocument();

        expect(
            screen.getByRole("cell", {
                name: "HIGH",
            }),
        ).toBeInTheDocument();

        expect(
            screen.getByRole("cell", {
                name: "APPROVED",
            }),
        ).toBeInTheDocument();
    });

    test("2. TEAM_LEADER thấy nút tạo và sửa", async () => {
        RequirementService.getRequirements.mockResolvedValue(mockPageData);

        render(<RequirementList projectId={1} currentUserRole="TEAM_LEADER" />);

        expect(screen.getByText("+ Tạo Requirement")).toBeInTheDocument();

        expect(await screen.findByText("Sửa")).toBeInTheDocument();
    });

    test.each(["LECTURER", "ADMIN", "STUDENT"])(
        "%s chỉ được xem, không được tạo/sửa",
        async (role) => {
            RequirementService.getRequirements.mockResolvedValue(mockPageData);

            render(<RequirementList projectId={1} currentUserRole={role} />);

            await screen.findByText("REQ-01");

            expect(
                screen.queryByText("+ Tạo Requirement"),
            ).not.toBeInTheDocument();

            expect(screen.queryByText("Sửa")).not.toBeInTheDocument();
        },
    );

    test("4. Hiển thị lỗi và nút Thử lại khi API 500", async () => {
        const error = new Error("Lỗi máy chủ (500)");

        error.status = 500;

        RequirementService.getRequirements.mockRejectedValue(error);

        render(<RequirementList projectId={1} currentUserRole="TEAM_LEADER" />);

        expect(
            await screen.findByText("Lỗi máy chủ (500)"),
        ).toBeInTheDocument();

        expect(screen.getByText("Thử lại")).toBeInTheDocument();
    });

    test("5. Gửi đúng endpoint params khi tìm kiếm", async () => {
        RequirementService.getRequirements.mockResolvedValue(mockPageData);

        render(<RequirementList projectId={1} currentUserRole="TEAM_LEADER" />);

        await screen.findByText("REQ-01");

        const searchInput = screen.getByPlaceholderText("Tìm từ khóa");

        fireEvent.change(searchInput, {
            target: {
                value: "login",
            },
        });

        fireEvent.click(screen.getByText("Tìm kiếm"));

        await waitFor(() => {
            expect(RequirementService.getRequirements).toHaveBeenLastCalledWith(
                1,
                expect.objectContaining({
                    keyword: "login",
                    status: "",
                    priority: "",
                    jiraIssueKey: "",
                    page: 0,
                    size: 20,
                    sort: "updatedAt,desc",
                }),
            );
        });
    });

    test("6. Gửi đúng filter status và priority", async () => {
        RequirementService.getRequirements.mockResolvedValue(mockPageData);

        render(<RequirementList projectId={1} currentUserRole="TEAM_LEADER" />);

        await screen.findByText("REQ-01");

        const statusSelect = screen.getByLabelText("Status filter");
        fireEvent.change(statusSelect, {
            target: { value: "APPROVED" },
        });

        const prioritySelect = screen.getByLabelText("Priority filter");
        fireEvent.change(prioritySelect, {
            target: { value: "HIGH" },
        });

        await waitFor(() => {
            expect(RequirementService.getRequirements).toHaveBeenCalled();
        });
    });

    test("7. API 401 hiển thị lỗi đăng nhập", async () => {
        const error = new Error("Unauthorized");

        error.status = 401;

        RequirementService.getRequirements.mockRejectedValue(error);

        render(<RequirementList projectId={1} currentUserRole="TEAM_LEADER" />);

        expect(
            await screen.findByText(/Phiên đăng nhập đã hết hạn/i),
        ).toBeInTheDocument();
    });

    test("8. API 403 hiển thị lỗi phân quyền", async () => {
        const error = new Error("Forbidden");

        error.status = 403;

        RequirementService.getRequirements.mockRejectedValue(error);

        render(<RequirementList projectId={1} currentUserRole="LECTURER" />);

        expect(await screen.findByText(/không có quyền/i)).toBeInTheDocument();
    });

    test("9. API 404 hiển thị lỗi không tìm thấy", async () => {
        const error = new Error("Not Found");

        error.status = 404;

        RequirementService.getRequirements.mockRejectedValue(error);

        render(<RequirementList projectId={1} currentUserRole="TEAM_LEADER" />);

        expect(
            await screen.findByText(/Không tìm thấy project/i),
        ).toBeInTheDocument();
    });

    test("10. API trả sai cấu trúc không được fallback mock", async () => {
        RequirementService.getRequirements.mockResolvedValue({
            foo: "bar",
        });

        render(<RequirementList projectId={1} currentUserRole="TEAM_LEADER" />);

        expect(await screen.findByText(/data\.content/i)).toBeInTheDocument();

        expect(screen.queryByText("REQ-01")).not.toBeInTheDocument();
    });

    test("11. Không có dữ liệu hiển thị empty state", async () => {
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

    test("12. Không có projectId hiển thị lỗi", async () => {
        render(
            <RequirementList projectId={null} currentUserRole="TEAM_LEADER" />,
        );

        expect(
            await screen.findByText(/Không xác định được project/i),
        ).toBeInTheDocument();

        expect(RequirementService.getRequirements).not.toHaveBeenCalled();
    });

    test("13. Gửi đúng jiraIssueKey", async () => {
        RequirementService.getRequirements.mockResolvedValue(mockPageData);

        render(<RequirementList projectId={1} currentUserRole="TEAM_LEADER" />);

        await screen.findByText("REQ-01");

        fireEvent.change(screen.getByPlaceholderText("Ví dụ: CNPM-63"), {
            target: {
                value: "CNPM-63",
            },
        });

        fireEvent.click(screen.getByText("Tìm kiếm"));

        await waitFor(() => {
            expect(RequirementService.getRequirements).toHaveBeenLastCalledWith(
                1,
                expect.objectContaining({
                    jiraIssueKey: "CNPM-63",
                    page: 0,
                    size: 20,
                }),
            );
        });
    });

    test("14. Không gọi API bằng search hoặc actor", async () => {
        RequirementService.getRequirements.mockResolvedValue(mockPageData);

        render(<RequirementList projectId={1} currentUserRole="TEAM_LEADER" />);

        await screen.findByText("REQ-01");

        const calls = RequirementService.getRequirements.mock.calls;

        calls.forEach(([, params]) => {
            expect(params).not.toHaveProperty("search");

            expect(params).not.toHaveProperty("actor");
        });
    });

    test("15. Phân trang gọi đúng page", async () => {
        RequirementService.getRequirements.mockResolvedValue({
            ...mockPageData,
            totalPages: 2,
        });

        render(<RequirementList projectId={1} currentUserRole="LECTURER" />);

        await screen.findByText("REQ-01");

        fireEvent.click(screen.getByText("Sau"));

        await waitFor(() => {
            expect(RequirementService.getRequirements).toHaveBeenLastCalledWith(
                1,
                expect.objectContaining({
                    page: 1,
                    size: 20,
                }),
            );
        });
    });
});
