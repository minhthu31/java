import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import JiraConfigComponent from "./JiraConfigComponent";
import { JiraIntegrationService } from "./JiraIntegrationService";

jest.mock("./JiraIntegrationService");

describe("JiraConfigComponent Tests - Strict RBAC & Contract Verification", () => {
    const mockProjectId = "PROJ-123";

    beforeEach(() => {
        jest.clearAllMocks();
    });

    test("1. Khởi tạo form cho ADMIN, đọc status từ { data: ... } và render lastTestedAt", async () => {
        const testTimestamp = "2026-08-29T10:00:00Z";
        JiraIntegrationService.getConnection.mockResolvedValueOnce({
            data: {
                siteUrl: "https://jira.example.com",
                projectKey: "TEST",
                authType: "API_TOKEN",
                configured: true,
                lastTestSucceeded: true,
                lastTestedAt: testTimestamp,
            },
        });

        render(<JiraConfigComponent projectId={mockProjectId} role="ADMIN" />);

        await waitFor(() => {
            expect(
                screen.getByTestId("connection-status-badge").textContent,
            ).toBe("CONNECTED");
            const lastTestedEl = screen.getByTestId("last-tested-at");
            expect(lastTestedEl).toBeTruthy();
            expect(lastTestedEl.textContent).toContain(
                new Date(testTimestamp).toLocaleString("vi-VN"),
            );
        });
    });

    test("2. TEAM_LEADER bị chặn hoàn toàn khỏi màn hình cấu hình Jira", () => {
        render(
            <JiraConfigComponent
                projectId={mockProjectId}
                role="TEAM_LEADER"
            />,
        );

        expect(
            screen.getByTestId("unauthorized-message").textContent,
        ).toContain("Bạn không có quyền truy cập cấu hình này.");
        expect(screen.queryByLabelText(/Jira Base URL/i)).toBeNull();
        expect(screen.queryByTestId("save-config-btn")).toBeNull();
        expect(screen.queryByTestId("test-connection-btn")).toBeNull();
    });

    test("3. TEAM_MEMBER bị chặn hoàn toàn khỏi màn hình cấu hình Jira", () => {
        render(
            <JiraConfigComponent
                projectId={mockProjectId}
                role="TEAM_MEMBER"
            />,
        );

        expect(
            screen.getByTestId("unauthorized-message").textContent,
        ).toContain("Bạn không có quyền truy cập cấu hình này.");
        expect(screen.queryByLabelText(/Jira Base URL/i)).toBeNull();
    });

    test("4. ADMIN submit form gửi đầy đủ payload 5 trường và authType API_TOKEN", async () => {
        JiraIntegrationService.getConnection.mockResolvedValueOnce({
            data: {
                siteUrl: "",
                projectKey: "",
                authType: "API_TOKEN",
                configured: false,
            },
        });
        JiraIntegrationService.configureConnection.mockResolvedValueOnce({
            data: { success: true },
        });

        render(<JiraConfigComponent projectId={mockProjectId} role="ADMIN" />);

        fireEvent.change(screen.getByLabelText(/Jira Base URL/i), {
            target: { value: "https://jira.example.com" },
        });
        fireEvent.change(screen.getByLabelText(/Project Key/i), {
            target: { value: "PROJ" },
        });
        fireEvent.change(screen.getByLabelText(/Atlassian Account Email/i), {
            target: { value: "admin@example.com" },
        });
        fireEvent.change(screen.getByLabelText(/API Token/i), {
            target: { value: "secret-token-123" },
        });

        fireEvent.click(screen.getByTestId("save-config-btn"));

        await waitFor(() => {
            expect(
                JiraIntegrationService.configureConnection,
            ).toHaveBeenCalledWith(mockProjectId, {
                siteUrl: "https://jira.example.com",
                projectKey: "PROJ",
                email: "admin@example.com",
                authType: "API_TOKEN",
                apiToken: "secret-token-123",
            });
        });
    });

    test("5. Test connection thành công (connected=true) với response { data: { connected: true, ... } }", async () => {
        const testedTimestamp = "2026-08-29T15:30:00Z";
        JiraIntegrationService.getConnection.mockResolvedValueOnce({
            data: {
                siteUrl: "https://jira.example.com",
                projectKey: "PROJ",
                configured: true,
                lastTestSucceeded: null,
                lastTestedAt: null,
            },
        });
        JiraIntegrationService.testConnection.mockResolvedValueOnce({
            data: {
                connected: true,
                testedAt: testedTimestamp,
            },
        });

        render(<JiraConfigComponent projectId={mockProjectId} role="ADMIN" />);

        const testBtn = await screen.findByTestId("test-connection-btn");
        await waitFor(() => expect(testBtn.disabled).toBe(false));

        fireEvent.click(testBtn);

        await waitFor(() => {
            expect(JiraIntegrationService.testConnection).toHaveBeenCalledWith(
                mockProjectId,
            );
            expect(
                screen.getByTestId("jira-success-message").textContent,
            ).toContain("Kết nối tới Jira thành công!");
            expect(
                screen.getByTestId("connection-status-badge").textContent,
            ).toBe("CONNECTED");
            expect(screen.getByTestId("last-tested-at").textContent).toContain(
                new Date(testedTimestamp).toLocaleString("vi-VN"),
            );
        });
    });

    test("6. Test connection thất bại (connected=false) với response { data: { connected: false, ... } }", async () => {
        const testedTimestamp = "2026-08-29T15:35:00Z";
        JiraIntegrationService.getConnection.mockResolvedValueOnce({
            data: {
                siteUrl: "https://jira.example.com",
                projectKey: "PROJ",
                configured: true,
                lastTestSucceeded: null,
                lastTestedAt: null,
            },
        });
        JiraIntegrationService.testConnection.mockResolvedValueOnce({
            data: {
                connected: false,
                errorCode: "JIRA_AUTHENTICATION_FAILED",
                message: "Xác thực Jira thất bại.",
                testedAt: testedTimestamp,
            },
        });

        render(<JiraConfigComponent projectId={mockProjectId} role="ADMIN" />);

        const testBtn = await screen.findByTestId("test-connection-btn");
        await waitFor(() => expect(testBtn.disabled).toBe(false));

        fireEvent.click(testBtn);

        await waitFor(() => {
            expect(JiraIntegrationService.testConnection).toHaveBeenCalledWith(
                mockProjectId,
            );
            expect(
                screen.getByTestId("jira-error-message").textContent,
            ).toContain("Xác thực Jira thất bại.");
            expect(
                screen.getByTestId("connection-status-badge").textContent,
            ).toBe("CONNECTION_FAILED");
            expect(screen.getByTestId("last-tested-at").textContent).toContain(
                new Date(testedTimestamp).toLocaleString("vi-VN"),
            );
        });
    });
});
