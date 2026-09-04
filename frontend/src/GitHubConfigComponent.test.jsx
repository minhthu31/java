import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";
import { GitHubConfigComponent } from "./GitHubConfigComponent";
import { GitHubConfigService } from "./GitHubConfigService";

jest.mock("./GitHubConfigService");

describe("GitHubConfigComponent (Task 92 - Chuẩn OpenAPI CNPM-88)", () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test("người dùng không phải ADMIN bị chặn truy cập và hiển thị 403", () => {
        render(
            <GitHubConfigComponent
                currentUserRole="TEAM_MEMBER"
                projectId={1}
            />,
        );
        expect(
            screen.getByText(/403 - Truy cập bị từ chối/i),
        ).toBeInTheDocument();
    });

    test("chưa chọn dự án thì hiển thị thông báo Chưa chọn dự án", () => {
        render(
            <GitHubConfigComponent currentUserRole="ADMIN" projectId={null} />,
        );
        expect(screen.getByTestId("no-project-message")).toBeInTheDocument();
    });

    test("render form đầy đủ với token dạng password và status Not Checked", async () => {
        GitHubConfigService.getConfig.mockResolvedValueOnce({
            projectId: 1,
            repositoryFullName: "",
            configured: false,
            status: "NOT_CHECKED",
        });

        render(<GitHubConfigComponent currentUserRole="ADMIN" projectId={1} />);

        await waitFor(() => {
            expect(
                screen.getByRole("button", { name: /Save Configuration/i }),
            ).toBeInTheDocument();
        });

        expect(screen.getByLabelText(/Repository Owner/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Repository Name/i)).toBeInTheDocument();
        const tokenInput = screen.getByLabelText(/Personal Access Token/i);
        expect(tokenInput).toHaveAttribute("type", "password");
        expect(screen.getByTestId("connection-status-badge")).toHaveTextContent(
            "Not Checked",
        );
    });

    test("không tải token từ server về ô input (đảm bảo tính an toàn của writeOnly token)", async () => {
        GitHubConfigService.getConfig.mockResolvedValueOnce({
            projectId: 1,
            repositoryFullName: "my-org/my-repo",
            configured: true,
            status: "CONNECTED",
        });

        render(<GitHubConfigComponent currentUserRole="ADMIN" projectId={1} />);

        await waitFor(() => {
            const tokenInput = screen.getByLabelText(/Personal Access Token/i);
            expect(tokenInput).toHaveValue("");
        });
    });

    test("lưu cấu hình dùng phương thức PUT gửi đúng contract OpenAPI và làm trống ô token sau khi lưu", async () => {
        GitHubConfigService.getConfig.mockResolvedValueOnce({
            projectId: 1,
            repositoryFullName: "",
            configured: false,
            status: "NOT_CHECKED",
        });
        GitHubConfigService.saveConfig.mockResolvedValueOnce({
            projectId: 1,
            repositoryFullName: "my-org/my-repo",
            configured: true,
            status: "NOT_CHECKED",
        });

        render(<GitHubConfigComponent currentUserRole="ADMIN" projectId={1} />);

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
        fireEvent.change(tokenInput, { target: { value: "ghp_validToken" } });

        fireEvent.click(
            screen.getByRole("button", { name: /Save Configuration/i }),
        );

        await waitFor(() => {
            expect(GitHubConfigService.saveConfig).toHaveBeenCalledWith(1, {
                repositoryOwner: "my-org",
                repositoryName: "my-repo",
                apiVersion: "2026-03-10",
                accessToken: "ghp_validToken",
            });
            expect(
                screen.getByText(/Lưu cấu hình GitHub thành công/i),
            ).toBeInTheDocument();
            expect(tokenInput.value).toBe("");
        });
    });

    test("kiểm tra kết nối thành công: hiển thị trạng thái Connected", async () => {
        GitHubConfigService.getConfig.mockResolvedValueOnce({
            projectId: 1,
            repositoryFullName: "test-org/test-repo",
            configured: true,
            status: "NOT_CHECKED",
        });
        GitHubConfigService.testConnection.mockResolvedValueOnce({
            projectId: 1,
            connected: true,
            testedAt: "2026-09-02T12:00:00Z",
        });

        render(<GitHubConfigComponent currentUserRole="ADMIN" projectId={1} />);

        await waitFor(() => {
            expect(screen.getByLabelText(/Repository Owner/i)).toHaveValue(
                "test-org",
            );
        });

        fireEvent.click(
            screen.getByRole("button", { name: /Test Connection/i }),
        );

        await waitFor(() => {
            expect(
                screen.getByTestId("connection-status-badge"),
            ).toHaveTextContent("Connected");
            expect(screen.getByTestId("last-checked-time")).toBeInTheDocument();
        });
    });

    test("kiểm tra kết nối thất bại: hiển thị trạng thái Failed (CONNECTION_FAILED) và hướng xử lý gợi ý", async () => {
        GitHubConfigService.getConfig.mockResolvedValueOnce({
            projectId: 1,
            repositoryFullName: "wrong-org/wrong-repo",
            configured: true,
            status: "NOT_CHECKED",
        });
        GitHubConfigService.testConnection.mockRejectedValueOnce({
            response: {
                data: {
                    code: "GITHUB_AUTHENTICATION_FAILED",
                    message: "Bad credentials or repository not found",
                },
            },
        });

        render(<GitHubConfigComponent currentUserRole="ADMIN" projectId={1} />);

        await waitFor(() => {
            expect(screen.getByLabelText(/Repository Owner/i)).toHaveValue(
                "wrong-org",
            );
        });

        fireEvent.click(
            screen.getByRole("button", { name: /Test Connection/i }),
        );

        await waitFor(() => {
            expect(
                screen.getByTestId("connection-status-badge"),
            ).toHaveTextContent("Failed");
            expect(
                screen.getByText(/Lý do thất bại: Bad credentials/i),
            ).toBeInTheDocument();
            expect(screen.getByText(/Hướng xử lý gợi ý:/i)).toBeInTheDocument();
        });
    });

    test("khóa Test Connection khi cấu hình chưa được lưu hoặc đang bị chỉnh sửa", async () => {
        GitHubConfigService.getConfig.mockResolvedValueOnce({
            projectId: 1,
            repositoryFullName: "",
            configured: false,
            status: "NOT_CHECKED",
        });

        render(<GitHubConfigComponent currentUserRole="ADMIN" projectId={1} />);

        await waitFor(() => {
            expect(
                screen.getByRole("button", { name: /Save Configuration/i }),
            ).toBeInTheDocument();
        });

        const testBtn = screen.getByRole("button", {
            name: /Test Connection/i,
        });
        expect(testBtn).toBeDisabled();

        fireEvent.change(screen.getByLabelText(/Personal Access Token/i), {
            target: { value: "ghp_unsavedToken" },
        });
        expect(testBtn).toBeDisabled();
    });

    test("hiển thị thông báo lỗi và vô hiệu hóa Save và Test khi tải cấu hình ban đầu thất bại", async () => {
        GitHubConfigService.getConfig.mockRejectedValueOnce({
            response: {
                data: { message: "Lỗi kết nối CSDL khi tải cấu hình" },
            },
        });

        render(<GitHubConfigComponent currentUserRole="ADMIN" projectId={1} />);

        await waitFor(() => {
            expect(
                screen.getByTestId("initial-load-error"),
            ).toBeInTheDocument();
        });

        expect(
            screen.getByText(/Lỗi kết nối CSDL khi tải cấu hình/i),
        ).toBeInTheDocument();
        expect(
            screen.getByRole("button", { name: /Save Configuration/i }),
        ).toBeDisabled();
        expect(
            screen.getByRole("button", { name: /Test Connection/i }),
        ).toBeDisabled();
        expect(
            screen.getByRole("button", { name: /Thử lại/i }),
        ).toBeInTheDocument();
    });
});
