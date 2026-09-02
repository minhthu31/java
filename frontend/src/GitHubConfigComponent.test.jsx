import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";
import { GitHubConfigComponent } from "./GitHubConfigComponent";
import { GitHubConfigService } from "./GitHubConfigService";

jest.mock("./GitHubConfigService");

describe("GitHubConfigComponent (Task 92)", () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test("người dùng không phải ADMIN bị chặn truy cập và hiển thị 403", () => {
        render(<GitHubConfigComponent currentUserRole="TEAM_MEMBER" />);
        expect(
            screen.getByText(/403 - Truy cập bị từ chối/i),
        ).toBeInTheDocument();
        expect(
            screen.queryByLabelText(/Repository Owner/i),
        ).not.toBeInTheDocument();
    });

    test("render đầy đủ các trường form và che giấu token dạng password cho Admin", async () => {
        GitHubConfigService.getConfig.mockResolvedValueOnce({
            owner: "",
            repository: "",
            hasToken: false,
            status: "NOT_CHECKED",
        });

        render(<GitHubConfigComponent currentUserRole="ADMIN" />);

        await waitFor(() => {
            expect(
                screen.getByRole("button", { name: /Save Configuration/i }),
            ).toBeInTheDocument();
        });

        expect(screen.getByLabelText(/Repository Owner/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Repository Name/i)).toBeInTheDocument();

        const tokenInput = screen.getByLabelText(/Personal Access Token/i);
        expect(tokenInput).toBeInTheDocument();
        expect(tokenInput).toHaveAttribute("type", "password");
        expect(
            screen.getByRole("button", { name: /Test Connection/i }),
        ).toBeInTheDocument();
    });

    test("mặc định hiển thị trạng thái Not Checked", async () => {
        GitHubConfigService.getConfig.mockResolvedValueOnce({
            owner: "",
            repository: "",
            hasToken: false,
            status: "NOT_CHECKED",
        });

        render(<GitHubConfigComponent currentUserRole="ADMIN" />);

        await waitFor(() => {
            expect(
                screen.getByTestId("connection-status-badge"),
            ).toHaveTextContent("Not Checked");
        });
    });

    test("không tải token từ server về ô input (đảm bảo bảo mật)", async () => {
        GitHubConfigService.getConfig.mockResolvedValueOnce({
            owner: "my-org",
            repository: "my-repo",
            hasToken: true,
            status: "CONNECTED",
        });

        render(<GitHubConfigComponent currentUserRole="ADMIN" />);

        await waitFor(() => {
            const tokenInput = screen.getByLabelText(/Personal Access Token/i);
            expect(tokenInput).toHaveValue("");
            expect(tokenInput).toHaveAttribute(
                "placeholder",
                "•••••••••••••••• (Đã cấu hình, nhập mới để đổi token khác)",
            );
        });
    });

    test("kiểm tra kết nối thành công: hiển thị loading, đổi trạng thái sang Connected và ghi nhận thời gian", async () => {
        GitHubConfigService.getConfig.mockResolvedValueOnce({});
        GitHubConfigService.testConnection.mockResolvedValueOnce({
            connected: true,
        });

        render(<GitHubConfigComponent currentUserRole="ADMIN" />);

        await waitFor(() => {
            expect(
                screen.getByRole("button", { name: /Test Connection/i }),
            ).toBeInTheDocument();
        });

        fireEvent.change(screen.getByLabelText(/Repository Owner/i), {
            target: { value: "test-org" },
        });
        fireEvent.change(screen.getByLabelText(/Repository Name/i), {
            target: { value: "test-repo" },
        });
        fireEvent.change(screen.getByLabelText(/Personal Access Token/i), {
            target: { value: "ghp_secretToken123" },
        });

        const testBtn = screen.getByRole("button", {
            name: /Test Connection/i,
        });
        fireEvent.click(testBtn);

        expect(
            screen.getByRole("button", { name: /Đang kiểm tra.../i }),
        ).toBeInTheDocument();

        await waitFor(() => {
            const badge = screen.getByTestId("connection-status-badge");
            expect(badge).toHaveTextContent("Connected");
            expect(screen.getByTestId("last-checked-time")).toBeInTheDocument();
        });
    });

    test("kiểm tra kết nối thất bại: hiển thị trạng thái Failed, lý do và danh sách hướng xử lý", async () => {
        GitHubConfigService.getConfig.mockResolvedValueOnce({});
        GitHubConfigService.testConnection.mockRejectedValueOnce({
            response: {
                data: { message: "Bad credentials or repository not found" },
            },
        });

        render(<GitHubConfigComponent currentUserRole="ADMIN" />);

        await waitFor(() => {
            expect(
                screen.getByRole("button", { name: /Test Connection/i }),
            ).toBeInTheDocument();
        });

        fireEvent.change(screen.getByLabelText(/Repository Owner/i), {
            target: { value: "wrong-org" },
        });
        fireEvent.change(screen.getByLabelText(/Repository Name/i), {
            target: { value: "wrong-repo" },
        });
        fireEvent.change(screen.getByLabelText(/Personal Access Token/i), {
            target: { value: "ghp_invalid" },
        });

        fireEvent.click(
            screen.getByRole("button", { name: /Test Connection/i }),
        );

        await waitFor(() => {
            const badge = screen.getByTestId("connection-status-badge");
            expect(badge).toHaveTextContent("Failed");
            expect(
                screen.getByText(/Lý do thất bại: Bad credentials/i),
            ).toBeInTheDocument();
            expect(screen.getByText(/Hướng xử lý gợi ý:/i)).toBeInTheDocument();
        });
    });

    test("lưu cấu hình thành công và không giữ lại token thô trong input", async () => {
        GitHubConfigService.getConfig.mockResolvedValueOnce({});
        GitHubConfigService.saveConfig.mockResolvedValueOnce({ success: true });

        render(<GitHubConfigComponent currentUserRole="ADMIN" />);

        await waitFor(() => {
            expect(
                screen.getByRole("button", { name: /Save Configuration/i }),
            ).toBeInTheDocument();
        });

        fireEvent.change(screen.getByLabelText(/Repository Owner/i), {
            target: { value: "my-org" },
        });
        fireEvent.change(screen.getByLabelText(/Repository Name/i), {
            target: { value: "my-repo" },
        });
        const tokenInput = screen.getByLabelText(/Personal Access Token/i);
        fireEvent.change(tokenInput, {
            target: { value: "ghp_secretTokenToSave" },
        });

        fireEvent.click(
            screen.getByRole("button", { name: /Save Configuration/i }),
        );

        await waitFor(() => {
            expect(
                screen.getByText(/Lưu cấu hình GitHub thành công/i),
            ).toBeInTheDocument();
            expect(tokenInput.value).toBe("");
        });
    });
});
