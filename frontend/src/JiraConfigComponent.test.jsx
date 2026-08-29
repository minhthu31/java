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

    test("1. Khởi tạo form với authType API_TOKEN và đọc lastTestSucceeded", async () => {
        JiraIntegrationService.getConnection.mockResolvedValueOnce({
            siteUrl: "https://jira.example.com",
            email: "admin@example.com",
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
            expect(
                screen.getByLabelText(/Email tài khoản Atlassian/i).value,
            ).toBe("admin@example.com");
            expect(screen.getByLabelText(/Project Key/i).value).toBe("TEST");
            expect(
                screen.getByTestId("connection-status-badge").textContent,
            ).toBe("CONNECTED");
        });
    });

    test("2. TEAM_LEADER được quyền đọc GET cấu hình và không thấy nút Test Connection", async () => {
        JiraIntegrationService.getConnection.mockResolvedValueOnce({
            siteUrl: "https://jira.example.com",
            email: "leader@example.com",
            projectKey: "LEAD",
            authType: "API_TOKEN",
            configured: true,
            lastTestSucceeded: null,
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
            expect(screen.queryByTestId("test-connection-btn")).toBeNull();
            expect(screen.getByTestId("save-config-btn")).toBeInTheDocument();
        });
    });

    test("3. Submit form gửi đầy đủ apiToken và authType API_TOKEN", async () => {
        JiraIntegrationService.getConnection.mockResolvedValueOnce({
            siteUrl: "",
            email: "",
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
        fireEvent.change(screen.getByLabelText(/Email tài khoản Atlassian/i), {
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
                authType: "API_TOKEN",
                apiToken: "secret-token-123",
            });
        });
    });

    test("4. Test connection gọi đúng projectId và không truyền body", async () => {
        JiraIntegrationService.getConnection.mockResolvedValueOnce({
            siteUrl: "https://jira.example.com",
            email: "admin@example.com",
            projectKey: "PROJ",
            configured: true,
        });
        JiraIntegrationService.testConnection.mockResolvedValueOnce({});

        render(<JiraConfigComponent projectId={mockProjectId} role="ADMIN" />);

        const testBtn = await screen.findByTestId("test-connection-btn");
        await waitFor(() => expect(testBtn).not.toBeDisabled());

        fireEvent.click(testBtn);

        await waitFor(() => {
            expect(JiraIntegrationService.testConnection).toHaveBeenCalledWith(
                mockProjectId,
            );
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
