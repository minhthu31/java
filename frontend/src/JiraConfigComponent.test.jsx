import React from "react";
import {
    render,
    screen,
    fireEvent,
    waitFor,
    act,
} from "@testing-library/react";
import "@testing-library/jest-dom";
import JiraConfigComponent from "./JiraConfigComponent";
import { JiraService } from "./JiraService";
import * as authService from "./authService";

jest.mock("./JiraService");
jest.mock("./authService");

const mockInitialConfig = {
    baseUrl: "https://jira.mycompany.com",
    accountIdentifier: "admin@mycompany.com",
    projectKey: "CNPM",
    connectionStatus: "NOT_CHECKED",
    lastTestedAt: null,
};

describe("JiraConfigComponent Tests", () => {
    beforeEach(() => {
        jest.clearAllMocks();
        authService.currentUser.mockReturnValue({
            id: 99,
            username: "admin.user",
            role: "ROLE_ADMIN",
        });
        JiraService.getConfig.mockResolvedValue(mockInitialConfig);
    });

    test("1. Người không phải ADMIN bị chặn truy cập và không gọi API", async () => {
        authService.currentUser.mockReturnValue({
            id: 1,
            username: "leader.user",
            role: "TEAM_LEADER",
        });

        await act(async () => {
            render(<JiraConfigComponent />);
        });

        expect(screen.getByTestId("unauthorized-message")).toBeInTheDocument();
        expect(screen.queryByTestId("save-config-btn")).not.toBeInTheDocument();
        expect(JiraService.getConfig).not.toHaveBeenCalled();
    });

    test("2. Tải cấu hình ban đầu: Input token là password và không tự động fill token từ server", async () => {
        await act(async () => {
            render(<JiraConfigComponent />);
        });

        await waitFor(() => {
            expect(screen.getByLabelText(/Jira Base URL/i)).toHaveValue(
                "https://jira.mycompany.com",
            );
            expect(screen.getByLabelText(/Account Identifier/i)).toHaveValue(
                "admin@mycompany.com",
            );
            expect(screen.getByLabelText(/Jira Project Key/i)).toHaveValue(
                "CNPM",
            );
        });

        const tokenInput = screen.getByLabelText(/Jira API Token/i);
        expect(tokenInput).toHaveAttribute("type", "password");
        expect(tokenInput).toHaveValue("");
        expect(screen.getByTestId("status-badge")).toHaveTextContent(
            "Not Checked",
        );
    });

    test("3. Lưu cấu hình thành công khi submit form", async () => {
        JiraService.saveConfig.mockResolvedValueOnce({ success: true });

        await act(async () => {
            render(<JiraConfigComponent />);
        });

        await waitFor(() => {
            expect(screen.getByLabelText(/Jira Base URL/i)).toHaveValue(
                "https://jira.mycompany.com",
            );
        });

        fireEvent.change(screen.getByLabelText(/Jira API Token/i), {
            target: { value: "my-super-secret-token" },
        });

        await act(async () => {
            fireEvent.click(screen.getByTestId("save-config-btn"));
        });

        expect(JiraService.saveConfig).toHaveBeenCalledWith(
            expect.objectContaining({
                baseUrl: "https://jira.mycompany.com",
                accountIdentifier: "admin@mycompany.com",
                projectKey: "CNPM",
                apiToken: "my-super-secret-token",
            }),
        );
        expect(screen.getByTestId("success-message")).toHaveTextContent(
            "Lưu cấu hình Jira thành công!",
        );
    });

    test("4. Test Connection thành công: Cập nhật badge Connected, thời điểm test và hiển thị thông báo", async () => {
        JiraService.testConnection.mockResolvedValueOnce({
            message: "Kết nối thành công tới Jira Cloud",
        });

        await act(async () => {
            render(<JiraConfigComponent />);
        });

        await waitFor(() => {
            expect(screen.getByLabelText(/Jira Base URL/i)).toHaveValue(
                "https://jira.mycompany.com",
            );
        });

        await act(async () => {
            fireEvent.click(screen.getByTestId("test-connection-btn"));
        });

        expect(screen.getByTestId("status-badge")).toHaveTextContent(
            "Connected",
        );
        expect(screen.getByTestId("last-tested-info")).not.toHaveTextContent(
            "Chưa từng kiểm tra",
        );
        expect(screen.getByTestId("success-message")).toHaveTextContent(
            "Kết nối thành công tới Jira Cloud",
        );
    });

    test("5. Test Connection thất bại (401 Unauthorized): Cập nhật badge Failed và hiển thị hướng dẫn khắc phục", async () => {
        JiraService.testConnection.mockRejectedValueOnce({
            response: {
                status: 401,
                data: { message: "Invalid Jira API Token" },
            },
        });

        await act(async () => {
            render(<JiraConfigComponent />);
        });

        await waitFor(() => {
            expect(screen.getByLabelText(/Jira Base URL/i)).toHaveValue(
                "https://jira.mycompany.com",
            );
        });

        await act(async () => {
            fireEvent.click(screen.getByTestId("test-connection-btn"));
        });

        expect(screen.getByTestId("status-badge")).toHaveTextContent("Failed");
        expect(screen.getByTestId("error-message")).toHaveTextContent(
            "Invalid Jira API Token",
        );
        expect(screen.getByTestId("troubleshooting-tip")).toHaveTextContent(
            "Kiểm tra lại Account Identifier và API Token Jira",
        );
    });

    test("6. Hiển thị trạng thái loading khi đang test connection", async () => {
        JiraService.testConnection.mockReturnValueOnce(new Promise(() => {}));

        await act(async () => {
            render(<JiraConfigComponent />);
        });

        await waitFor(() => {
            expect(screen.getByLabelText(/Jira Base URL/i)).toHaveValue(
                "https://jira.mycompany.com",
            );
        });

        await act(async () => {
            fireEvent.click(screen.getByTestId("test-connection-btn"));
        });

        expect(screen.getByTestId("test-connection-btn")).toHaveTextContent(
            "Đang kiểm tra kết nối...",
        );
        expect(screen.getByTestId("test-connection-btn")).toBeDisabled();
    });
});
