import "@testing-library/jest-dom";
import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import JiraConfigComponent from "./JiraConfigComponent";
import { JiraIntegrationService } from "./JiraIntegrationService";

jest.mock("./JiraIntegrationService");

describe("JiraConfigComponent Tests - Contract Strict Verification", () => {
    const mockProjectId = "PROJ-123";

    beforeEach(() => {
        jest.clearAllMocks();
    });

    test("1. Khởi tạo form với authType API_TOKEN, đọc status và render lastTestedAt", async () => {
        JiraIntegrationService.getConnection.mockResolvedValueOnce({
            siteUrl: "https://jira.example.com",
            projectKey: "TEST",
            authType: "API_TOKEN",
            configured: true,
            lastTestSucceeded: true,
            lastTestedAt: "2026-08-29T10:00:00Z",
        });

        render(<JiraConfigComponent projectId={mockProjectId} role="ADMIN" />);

        await waitFor(() => {
            expect(JiraIntegrationService.getConnection).toHaveBeenCalledWith(
                mockProjectId,
            );
        });

        await waitFor(() => {
            expect(screen.getByLabelText(/Jira Base URL/i).value).toBe(
                "https://jira.example.com",
            );
            expect(screen.getByLabelText(/Project Key/i).value).toBe("TEST");
            expect(
                screen.getByTestId("connection-status-badge").textContent,
            ).toBe("CONNECTED");
            expect(screen.getByTestId("last-tested-at")).toBeInTheDocument();
        });
    });

    test("2. Non-Admin (TEAM_LEADER) không có quyền truy cập màn hình", () => {
        render(
            <JiraConfigComponent
                projectId={mockProjectId}
                role="TEAM_LEADER"
            />,
        );

        expect(
            screen.getByTestId("unauthorized-message").textContent,
        ).toContain("Bạn không có quyền truy cập cấu hình này.");
    });

    test("3. Non-Admin (STUDENT) không có quyền truy cập màn hình", () => {
        render(
            <JiraConfigComponent projectId={mockProjectId} role="STUDENT" />,
        );

        expect(
            screen.getByTestId("unauthorized-message").textContent,
        ).toContain("Bạn không có quyền truy cập cấu hình này.");
    });

    test("4. Submit form gửi đầy đủ apiToken bắt buộc và authType API_TOKEN", async () => {
        JiraIntegrationService.getConnection.mockResolvedValueOnce({
            siteUrl: "",
            projectKey: "",
            authType: "API_TOKEN",
            configured: false,
        });
        JiraIntegrationService.configureConnection.mockResolvedValueOnce({});

        render(<JiraConfigComponent projectId={mockProjectId} role="ADMIN" />);

        const saveBtn = await screen.findByTestId("save-config-btn");
        await waitFor(() => expect(saveBtn).not.toBeDisabled());

        fireEvent.change(screen.getByLabelText(/Jira Base URL/i), {
            target: { value: "https://jira.example.com" },
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
                projectKey: "PROJ",
                authType: "API_TOKEN",
                apiToken: "secret-token-123",
            });
        });
    });

    test("5. Test connection xử lý khi HTTP 200 nhưng connected=false", async () => {
        JiraIntegrationService.getConnection.mockResolvedValue({
            siteUrl: "https://jira.example.com",
            projectKey: "PROJ",
            configured: true,
        });
        JiraIntegrationService.testConnection.mockResolvedValueOnce({
            connected: false,
            errorCode: "INVALID_CREDENTIALS",
            message: "Xác thực Jira thất bại.",
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
                screen.getByTestId("jira-error-message").textContent,
            ).toContain("Xác thực Jira thất bại.");
        });
    });
});
