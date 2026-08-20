import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import "@testing-library/jest-dom";
import Dashboard from "./Dashboard";
import { RequirementService } from "./RequirementService";
import * as authService from "./authService";

jest.mock("./RequirementService");
jest.mock("./authService");

const mockNavigate = jest.fn();
jest.mock("react-router-dom", () => ({
    ...jest.requireActual("react-router-dom"),
    useNavigate: () => mockNavigate,
}));

describe("CNPM-63: Test tích hợp Dashboard", () => {
    beforeEach(() => {
        jest.clearAllMocks();
        localStorage.clear();
    });

    test("1. Sử dụng đúng currentUser và bảo toàn giao diện Sprint 1", () => {
        authService.currentUser.mockReturnValue({
            username: "leader.test",
            fullName: "Test Leader",
            role: "TEAM_LEADER",
            projectId: 1,
        });

        render(
            <MemoryRouter>
                <Dashboard title="Trưởng nhóm" />
            </MemoryRouter>,
        );

        expect(screen.getByText(/Xin chào, Test Leader/i)).toBeInTheDocument();
        expect(screen.getByText(/leader\.test/i)).toBeInTheDocument();
        expect(screen.getByText("Yêu cầu dự án")).toBeInTheDocument();
        expect(screen.getByText("Công việc được giao")).toBeInTheDocument();
        expect(screen.getByText("Tiến độ nhóm")).toBeInTheDocument();
        expect(screen.getByText("Hoạt động GitHub")).toBeInTheDocument();
    });

    test("2. Không có user -> chuyển về login/unauthorized", () => {
        authService.currentUser.mockReturnValue(null);

        render(
            <MemoryRouter>
                <Dashboard />
            </MemoryRouter>,
        );

        expect(mockNavigate).toHaveBeenCalledWith("/login");
    });

    test("3. Không có project -> không gọi API và hiển thị yêu cầu chọn dự án", () => {
        authService.currentUser.mockReturnValue({
            username: "leader.test",
            role: "TEAM_LEADER",
        });

        render(
            <MemoryRouter>
                <Dashboard />
            </MemoryRouter>,
        );

        expect(screen.getByTestId("no-project-message")).toBeInTheDocument();
        expect(RequirementService.getRequirements).not.toHaveBeenCalled();
    });

    test("4. Team Leader thấy nút quản lý (Tạo Requirement)", async () => {
        authService.currentUser.mockReturnValue({
            username: "leader.test",
            role: "TEAM_LEADER",
            projectId: 1,
        });
        RequirementService.getRequirements.mockResolvedValue({
            content: [],
            totalPages: 1,
            totalElements: 0,
        });

        render(
            <MemoryRouter>
                <Dashboard />
            </MemoryRouter>,
        );

        expect(
            await screen.findByText("+ Tạo Requirement"),
        ).toBeInTheDocument();
    });

    test("5. Lecturer chỉ xem, không thấy nút Tạo/Sửa", async () => {
        authService.currentUser.mockReturnValue({
            username: "teacher.test",
            role: "LECTURER",
            projectId: 1,
        });
        RequirementService.getRequirements.mockResolvedValue({
            content: [],
            totalPages: 1,
            totalElements: 0,
        });

        render(
            <MemoryRouter>
                <Dashboard />
            </MemoryRouter>,
        );

        expect(await screen.findByText("Requirements")).toBeInTheDocument();
        expect(screen.queryByText("+ Tạo Requirement")).not.toBeInTheDocument();
    });

    test.each(["ADMIN", "STUDENT", "TEAM_MEMBER"])(
        "6. %s không gọi API Requirement theo CNPM-52",
        (role) => {
            authService.currentUser.mockReturnValue({
                username: "user.test",
                role: role,
                projectId: 1,
            });

            render(
                <MemoryRouter>
                    <Dashboard />
                </MemoryRouter>,
            );

            expect(
                screen.getByTestId("unauthorized-message"),
            ).toBeInTheDocument();
            expect(RequirementService.getRequirements).not.toHaveBeenCalled();
        },
    );

    test("7. Đăng xuất gọi đúng authService.logout() và navigate về /login", () => {
        authService.currentUser.mockReturnValue({
            username: "leader.test",
            role: "TEAM_LEADER",
            projectId: 1,
        });

        render(
            <MemoryRouter>
                <Dashboard />
            </MemoryRouter>,
        );

        fireEvent.click(screen.getByText("Đăng xuất"));
        expect(authService.logout).toHaveBeenCalled();
        expect(mockNavigate).toHaveBeenCalledWith("/login");
    });
});
