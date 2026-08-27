import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";
import JiraConfigComponent from "./JiraConfigComponent";
import { JiraIntegrationService } from "./JiraIntegrationService";

jest.mock("./JiraIntegrationService", () => {
    const mockService = {
        getConnection: jest.fn(),
        configureConnection: jest.fn(),
        testConnection: jest.fn(),
    };
    return {
        __esModule: true,
        default: mockService,
        JiraIntegrationService: mockService,
    };
});

describe("JiraConfigComponent Tests - Contract Strict Verification", () => {
    const mockProjectId = "PROJ-123";

    beforeEach(() => {
        jest.clearAllMocks();
        localStorage.setItem("role", "ADMIN");
    });

    test("1. Khởi tạo form với authType API_TOKEN và đọc lastTestSucceeded", async () => {
        JiraIntegrationService.getConnection.mockResolvedValueOnce({
            data: {
                siteUrl: "https://jira.example.com",
                email: "admin@example.com",
                projectKey: "TEST",
                apiToken: "saved-token",
                authType: "API_TOKEN",
                lastTestSucceeded: true,
            },
        });

        render(<JiraConfigComponent projectId={mockProjectId} role="ADMIN" />);

        await waitFor(() => {
            expect(JiraIntegrationService.getConnection).toHaveBeenCalledWith(
                mockProjectId,
            );
            expect(screen.getByLabelText(/Jira Base URL/i).value).toBe(
                "https://jira.example.com",
            );
            expect(
                screen.getByTestId("connection-status-badge").textContent,
            ).toContain("CONNECTED");
        });
    });

    test("2. TEAM_LEADER được quyền đọc GET cấu hình", async () => {
        JiraIntegrationService.getConnection.mockResolvedValueOnce({
            data: {
                siteUrl: "https://jira.example.com",
                email: "leader@example.com",
                projectKey: "LEAD",
                authType: "API_TOKEN",
                lastTestSucceeded: true,
            },
        });

        render(
            <JiraConfigComponent
                projectId={mockProjectId}
                role="TEAM_LEADER"
            />,
        );

        await waitFor(() => {
            expect(JiraIntegrationService.getConnection).toHaveBeenCalledWith(
                mockProjectId,
            );
            expect(screen.getByLabelText(/Jira Base URL/i).value).toBe(
                "https://jira.example.com",
            );
        });
    });

    test("3. Submit form gửi đầy đủ apiToken và authType API_TOKEN", async () => {
        JiraIntegrationService.getConnection.mockResolvedValueOnce({
            data: null,
        });
        JiraIntegrationService.configureConnection.mockResolvedValueOnce({
            data: { success: true },
        });

        render(<JiraConfigComponent projectId={mockProjectId} role="ADMIN" />);

        const saveBtn = await screen.findByTestId("save-config-btn");
        await waitFor(() => expect(saveBtn).not.toBeDisabled());

        fireEvent.change(screen.getByLabelText(/Jira Base URL/i), {
            target: { value: "https://jira.example.com" },
        });
        fireEvent.change(screen.getByLabelText(/Email tài khoản/i), {
            target: { value: "admin@example.com" },
        });
        fireEvent.change(screen.getByLabelText(/Project Key/i), {
            target: { value: "PROJ" },
        });
        fireEvent.change(screen.getByLabelText(/API Token/i), {
            target: { value: "secret-token-123" },
        });

        fireEvent.click(saveBtn);

        await waitFor(() => {
            expect(
                JiraIntegrationService.configureConnection,
            ).toHaveBeenCalledWith(mockProjectId, {
                siteUrl: "https://jira.example.com",
                email: "admin@example.com",
                projectKey: "PROJ",
                apiToken: "secret-token-123",
                authType: "API_TOKEN",
            });
        });
    });

    test("4. Test connection gọi đúng projectId và không truyền body", async () => {
        JiraIntegrationService.getConnection.mockResolvedValueOnce({
            data: null,
        });
        JiraIntegrationService.testConnection.mockResolvedValueOnce({
            data: { lastTestSucceeded: false },
        });

        render(<JiraConfigComponent projectId={mockProjectId} role="ADMIN" />);

        const testBtn = await screen.findByTestId("test-connection-btn");
        await waitFor(() => expect(testBtn).not.toBeDisabled());

        fireEvent.click(testBtn);

        await waitFor(() => {
            expect(JiraIntegrationService.testConnection).toHaveBeenCalledWith(
                mockProjectId,
            );
            expect(
                screen.getByTestId("connection-status-badge").textContent,
            ).toContain("FAILED");
        });
    });

    test("5. Hiển thị thông báo khi không có quyền truy cập (STUDENT)", () => {
        render(
            <JiraConfigComponent projectId={mockProjectId} role="STUDENT" />,
        );
        expect(
            screen.getByTestId("unauthorized-message").textContent,
        ).toContain("Bạn không có quyền truy cập cấu hình này.");
    });
});
