import React from "react";
import {
    render,
    screen,
    fireEvent,
    waitFor,
    act,
} from "@testing-library/react";
import "@testing-library/jest-dom";
import TaskComponent from "./TaskComponent";
import { TaskService } from "./TaskService";
import * as authService from "./authService";

jest.mock("./TaskService");
jest.mock("./authService");

const mockTasks = [
    {
        id: 1,
        title: "Xây dựng API đăng nhập",
        issue_type: "Feature",
        priority: "HIGH",
        assignee_name: "Member 1",
        deadline: "2026-09-01",
        sync_status: "NOT_SYNCED",
        status: "TODO",
        jira_issue_key: null,
        acceptance_criteria: "Trả về JWT Token hợp lệ",
        description: "Chi tiết kỹ thuật API",
    },
    {
        id: 2,
        title: "Tối ưu truy vấn SQL",
        issue_type: "Refactor",
        priority: "MEDIUM",
        assignee_name: "Member 2",
        deadline: "2026-09-05",
        sync_status: "SYNCED",
        status: "IN_PROGRESS",
        jira_issue_key: "CNPM-65",
        acceptance_criteria: "Query chạy dưới 100ms",
        description: "Thêm index cho bảng task",
    },
    {
        id: 3,
        title: "Đồng bộ Jira thất bại",
        issue_type: "Bug",
        priority: "LOW",
        assignee_name: "Member 3",
        deadline: "2026-09-10",
        sync_status: "SYNC_FAILED",
        status: "BLOCKED",
        jira_issue_key: "CNPM-66",
        acceptance_criteria: "Lỗi kết nối",
        description: "",
    },
    {
        id: 4,
        title: "Đang đồng bộ Jira",
        issue_type: "Docs",
        priority: "LOWEST",
        assignee_name: "Member 4",
        deadline: "2026-09-12",
        sync_status: "SYNCING",
        status: "IN_REVIEW",
        jira_issue_key: "CNPM-67",
        acceptance_criteria: "Tài liệu",
        description: "",
    },
];

describe("TaskComponent Comprehensive Tests (CNPM-65)", () => {
    beforeEach(() => {
        jest.clearAllMocks();
        authService.currentUser.mockReturnValue({
            id: 1,
            username: "leader.user",
            role: "TEAM_LEADER",
        });
        TaskService.getTasks.mockResolvedValue(mockTasks);
        TaskService.getTaskMetadata.mockResolvedValue({
            assignees: [{ id: 1, username: "member1", full_name: "Member 1" }],
            sprints: [{ id: 1, name: "Sprint 1" }],
            features: [{ id: 1, title: "Auth Feature" }],
        });
    });

    test("1. Hiển thị danh sách task và tất cả các badge đồng bộ (NOT_SYNCED, SYNCED, SYNC_FAILED, SYNCING)", async () => {
        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(
                screen.getByText("Xây dựng API đăng nhập"),
            ).toBeInTheDocument();
            expect(screen.getByText("Tối ưu truy vấn SQL")).toBeInTheDocument();
            expect(screen.getByText("NOT_SYNCED")).toBeInTheDocument();
            expect(screen.getByText("SYNCED")).toBeInTheDocument();
            expect(screen.getByText("SYNC_FAILED")).toBeInTheDocument();
            expect(screen.getByText("SYNCING")).toBeInTheDocument();
        });
    });

    test("2. Kiểm tra phân quyền: TEAM_LEADER thấy nút tạo, TEAM_MEMBER không thấy", async () => {
        const { unmount } = render(<TaskComponent projectId={1} />);
        await waitFor(() => {
            expect(screen.getByTestId("create-task-btn")).toBeInTheDocument();
        });
        unmount();

        authService.currentUser.mockReturnValue({
            id: 2,
            username: "member",
            role: "TEAM_MEMBER",
        });
        render(<TaskComponent projectId={1} />);
        await waitFor(() => {
            expect(
                screen.queryByTestId("create-task-btn"),
            ).not.toBeInTheDocument();
        });
    });

    test("3. Mở modal chi tiết và hiển thị Acceptance Criteria, Jira Key", async () => {
        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(
                screen.getByText("Xây dựng API đăng nhập"),
            ).toBeInTheDocument();
        });

        const taskBtn = screen.getByText("Xây dựng API đăng nhập");
        await act(async () => {
            fireEvent.click(taskBtn);
        });

        expect(screen.getByTestId("detail-modal")).toBeInTheDocument();
        expect(screen.getByText("Trả về JWT Token hợp lệ")).toBeInTheDocument();

        const closeBtn = screen.getByTestId("close-modal-btn");
        await act(async () => {
            fireEvent.click(closeBtn);
        });
        expect(screen.queryByTestId("detail-modal")).not.toBeInTheDocument();
    });

    test("4. Form Validation: Báo lỗi khi thiếu Title, Acceptance Criteria hoặc Deadline", async () => {
        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        const createBtn = await screen.findByTestId("create-task-btn");
        await act(async () => {
            fireEvent.click(createBtn);
        });

        const submitBtn = screen.getByRole("button", { name: /Lưu Task/i });
        await act(async () => {
            fireEvent.click(submitBtn);
        });

        expect(screen.getByTestId("form-error")).toHaveTextContent(
            "Vui lòng điền đầy đủ: Tiêu đề, Acceptance Criteria và Deadline.",
        );
        expect(TaskService.createTask).not.toHaveBeenCalled();
    });

    test("5. Tạo Task thành công và chống submit hai lần (button disable)", async () => {
        const newTask = {
            id: 5,
            title: "Task mới tạo",
            issue_type: "Bug",
            priority: "MEDIUM",
            deadline: "2026-09-20",
            sync_status: "NOT_SYNCED",
            status: "TODO",
            acceptance_criteria: "Fix xong",
        };

        let resolvePromise;
        TaskService.createTask.mockReturnValue(
            new Promise((res) => {
                resolvePromise = res;
            }),
        );

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await act(async () => {
            fireEvent.click(screen.getByTestId("create-task-btn"));
        });

        fireEvent.change(screen.getByLabelText(/Tiêu đề/i), {
            target: { value: "Task mới tạo" },
        });
        fireEvent.change(screen.getByLabelText(/Tiêu chí nghiệm thu/i), {
            target: { value: "Fix xong" },
        });
        fireEvent.change(screen.getByLabelText(/Hạn chót/i), {
            target: { value: "2026-09-20" },
        });

        const submitBtn = screen.getByRole("button", { name: /Lưu Task/i });

        act(() => {
            fireEvent.click(submitBtn);
        });

        expect(
            screen.getByRole("button", { name: /Đang lưu.../i }),
        ).toBeDisabled();

        await act(async () => {
            resolvePromise(newTask);
        });

        await waitFor(() => {
            expect(screen.getByText("Task mới tạo")).toBeInTheDocument();
        });
    });

    test("6. Xử lý lỗi API khi tải danh sách 403 và nút Thử lại", async () => {
        TaskService.getTasks.mockRejectedValueOnce({
            response: { status: 403 },
        });

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(screen.getByTestId("error-message")).toHaveTextContent(
                "Bạn không có quyền truy cập danh sách Task của dự án này.",
            );
        });

        TaskService.getTasks.mockResolvedValueOnce(mockTasks);
        await act(async () => {
            fireEvent.click(screen.getByTestId("retry-btn"));
        });

        await waitFor(() => {
            expect(
                screen.getByText("Xây dựng API đăng nhập"),
            ).toBeInTheDocument();
        });
    });

    test("7. Cập nhật trạng thái Task (Status change TODO -> DONE)", async () => {
        TaskService.updateTaskStatus.mockResolvedValue({});

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(
                screen.getByText("Xây dựng API đăng nhập"),
            ).toBeInTheDocument();
        });

        const statusSelect = screen.getByLabelText("Trạng thái task 1");
        await act(async () => {
            fireEvent.change(statusSelect, { target: { value: "DONE" } });
        });

        expect(TaskService.updateTaskStatus).toHaveBeenCalledWith(1, 1, "DONE");
        expect(statusSelect.value).toBe("DONE");
    });

    test("8. Hiển thị loading khi đang tải danh sách Task", async () => {
        let resolveTasks;
        TaskService.getTasks.mockReturnValue(
            new Promise((resolve) => {
                resolveTasks = resolve;
            }),
        );

        render(<TaskComponent projectId={1} />);

        expect(screen.getByTestId("loading-state")).toBeInTheDocument();

        await act(async () => {
            resolveTasks(mockTasks);
        });

        await waitFor(() => {
            expect(
                screen.getByText("Xây dựng API đăng nhập"),
            ).toBeInTheDocument();
        });
    });

    test("9. Hiển thị empty state khi dự án chưa có Task", async () => {
        TaskService.getTasks.mockResolvedValueOnce([]);

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(screen.getByTestId("empty-state")).toBeInTheDocument();
        });

        expect(
            screen.getByText("Chưa có công việc nào trong dự án này."),
        ).toBeInTheDocument();
    });

    test("10. Xử lý lỗi 401 khi tải danh sách Task", async () => {
        TaskService.getTasks.mockRejectedValueOnce({
            response: { status: 401 },
        });

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(screen.getByTestId("error-message")).toHaveTextContent(
                "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.",
            );
        });
    });

    test("11. Xử lý lỗi 404 khi tải danh sách Task", async () => {
        TaskService.getTasks.mockRejectedValueOnce({
            response: { status: 404 },
        });

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(screen.getByTestId("error-message")).toHaveTextContent(
                "Không tìm thấy dữ liệu dự án hoặc danh sách Task.",
            );
        });
    });

    test("12. Hiển thị lỗi khi API tạo Task thất bại", async () => {
        TaskService.createTask.mockRejectedValueOnce({
            response: {
                status: 500,
                data: {
                    message: "Không thể tạo Task do lỗi server",
                },
            },
        });

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(
                screen.getByText("Xây dựng API đăng nhập"),
            ).toBeInTheDocument();
        });

        await act(async () => {
            fireEvent.click(screen.getByTestId("create-task-btn"));
        });

        fireEvent.change(screen.getByLabelText(/Tiêu đề/i), {
            target: { value: "Task lỗi" },
        });
        fireEvent.change(screen.getByLabelText(/Tiêu chí nghiệm thu/i), {
            target: { value: "Test lỗi" },
        });
        fireEvent.change(screen.getByLabelText(/Hạn chót/i), {
            target: { value: "2026-09-20" },
        });

        await act(async () => {
            fireEvent.click(screen.getByRole("button", { name: /Lưu Task/i }));
        });

        await waitFor(() => {
            expect(screen.getByTestId("form-error")).toHaveTextContent(
                "Không thể tạo Task do lỗi server",
            );
        });
    });

    test("13. Không có projectId thì không gọi API", async () => {
        await act(async () => {
            render(<TaskComponent projectId={null} />);
        });

        await waitFor(() => {
            expect(TaskService.getTasks).not.toHaveBeenCalled();
            expect(TaskService.getTaskMetadata).not.toHaveBeenCalled();
        });

        expect(screen.queryByTestId("loading-state")).not.toBeInTheDocument();
    });

    test("14. ADMIN không thấy nút tạo Task", async () => {
        authService.currentUser.mockReturnValue({
            id: 1,
            username: "admin",
            role: "ADMIN",
        });

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(
                screen.getByText("Xây dựng API đăng nhập"),
            ).toBeInTheDocument();
        });

        expect(screen.queryByTestId("create-task-btn")).not.toBeInTheDocument();
    });

    test("15. LECTURER không thấy nút tạo Task", async () => {
        authService.currentUser.mockReturnValue({
            id: 3,
            username: "lecturer",
            role: "LECTURER",
        });

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(
                screen.getByText("Xây dựng API đăng nhập"),
            ).toBeInTheDocument();
        });

        expect(screen.queryByTestId("create-task-btn")).not.toBeInTheDocument();
    });
});
