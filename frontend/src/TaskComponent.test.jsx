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

const mockContractTasks = [
    {
        id: 1,
        title: "Xây dựng API đăng nhập",
        issueType: "TASK",
        classification: "FEATURE_RELATED",
        priority: "HIGH",
        assignee: {
            id: 10,
            username: "member1",
            displayName: "Nguyễn Văn A",
        },
        deadline: "2026-09-01T23:59:59.000Z",
        syncStatus: "NOT_SYNCED",
        status: "TO_DO",
        jiraIssueKey: null,
        acceptanceCriteria: "Trả về JWT Token hợp lệ",
        description: "Chi tiết kỹ thuật API",
    },
    {
        id: 2,
        title: "Tối ưu truy vấn SQL",
        issueType: "BUG",
        classification: "AUTO_TEST",
        priority: "MEDIUM",
        assignee: {
            id: 11,
            username: "member2",
            displayName: "Trần Thị B",
        },
        deadline: "2026-09-05T23:59:59.000Z",
        syncStatus: "SYNCED",
        status: "IN_PROGRESS",
        jiraIssueKey: "CNPM-65",
        acceptanceCriteria: "Query chạy dưới 100ms",
        description: "Thêm index cho bảng task",
    },
    {
        id: 3,
        title: "Đồng bộ Jira thất bại",
        issueType: "STORY",
        classification: "NEW_FEATURE",
        priority: "LOW",
        assignee: null,
        deadline: null,
        syncStatus: "FAILED",
        status: "BLOCKED",
        jiraIssueKey: "CNPM-66",
        acceptanceCriteria: "Lỗi kết nối Jira",
        description: "",
    },
    {
        id: 4,
        title: "Đang chờ đồng bộ Jira",
        issueType: "SUBTASK",
        classification: "OTHER",
        priority: "LOWEST",
        assignee: { id: 12, username: "member3", displayName: "Lê Văn C" },
        deadline: "2026-09-12T23:59:59.000Z",
        syncStatus: "PENDING",
        status: "IN_REVIEW",
        jiraIssueKey: "CNPM-67",
        acceptanceCriteria: "Tài liệu kỹ thuật",
        description: "",
    },
];

describe("TaskComponent Sprint 2 Contract Tests (CNPM-52 / CNPM-65)", () => {
    beforeEach(() => {
        jest.clearAllMocks();
        authService.currentUser.mockReturnValue({
            id: 1,
            username: "leader.user",
            role: "TEAM_LEADER",
        });
        TaskService.getTasks.mockResolvedValue(mockContractTasks);
        TaskService.getTaskById.mockImplementation((projId, taskId) => {
            const found = mockContractTasks.find((t) => t.id === taskId);
            return Promise.resolve(found);
        });
    });

    test("1. Hiển thị danh sách task chuẩn camelCase, nested assignee.displayName và các enum (NOT_SYNCED, SYNCED, FAILED, PENDING)", async () => {
        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(
                screen.getByText("Xây dựng API đăng nhập"),
            ).toBeInTheDocument();
            expect(screen.getByText("Nguyễn Văn A")).toBeInTheDocument();
            expect(screen.getByText("Trần Thị B")).toBeInTheDocument();
            expect(screen.getByText("NOT_SYNCED")).toBeInTheDocument();
            expect(screen.getByText("SYNCED")).toBeInTheDocument();
            expect(screen.getByText("FAILED")).toBeInTheDocument();
            expect(screen.getByText("PENDING")).toBeInTheDocument();
        });
    });

    test("2. Phân quyền: TEAM_LEADER thấy nút tạo, TEAM_MEMBER và LECTURER không thấy", async () => {
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
        const { unmount: unmountMember } = render(
            <TaskComponent projectId={1} />,
        );
        await waitFor(() => {
            expect(
                screen.queryByTestId("create-task-btn"),
            ).not.toBeInTheDocument();
        });
        unmountMember();

        authService.currentUser.mockReturnValue({
            id: 3,
            username: "teacher",
            role: "LECTURER",
        });
        render(<TaskComponent projectId={1} />);
        await waitFor(() => {
            expect(
                screen.queryByTestId("create-task-btn"),
            ).not.toBeInTheDocument();
        });
    });

    test("3. Mở modal chi tiết gọi TaskService.getTaskById và hiển thị thông tin", async () => {
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

        expect(TaskService.getTaskById).toHaveBeenCalledWith(1, 1);
        expect(screen.getByTestId("detail-modal")).toBeInTheDocument();
        expect(screen.getByText("Trả về JWT Token hợp lệ")).toBeInTheDocument();

        const closeBtn = screen.getByTestId("close-modal-btn");
        await act(async () => {
            fireEvent.click(closeBtn);
        });
        expect(screen.queryByTestId("detail-modal")).not.toBeInTheDocument();
    });

    test("4. Form Validation: Báo lỗi khi thiếu Title hoặc Acceptance Criteria", async () => {
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

        expect(screen.getByTestId("form-error")).toBeInTheDocument();
        expect(TaskService.createTask).not.toHaveBeenCalled();
    });

    test("5. Tạo Task thành công gửi payload assigneeUserId, classification và deadline ISO-8601", async () => {
        const createdTask = {
            id: 5,
            title: "Task mới chuẩn contract",
            issueType: "TASK",
            classification: "FEATURE_RELATED",
            priority: "HIGH",
            deadline: "2026-09-20T23:59:59.000Z",
            syncStatus: "NOT_SYNCED",
            status: "TO_DO",
            acceptanceCriteria: "Tiêu chí hợp lệ",
        };

        TaskService.createTask.mockResolvedValue(createdTask);

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await act(async () => {
            fireEvent.click(screen.getByTestId("create-task-btn"));
        });

        fireEvent.change(screen.getByLabelText(/Tiêu đề/i), {
            target: { value: "Task mới chuẩn contract" },
        });
        fireEvent.change(screen.getByLabelText(/Tiêu chí nghiệm thu/i), {
            target: { value: "Tiêu chí hợp lệ" },
        });
        fireEvent.change(screen.getByLabelText(/Hạn chót/i), {
            target: { value: "2026-09-20" },
        });
        fireEvent.change(screen.getByLabelText(/ID Người thực hiện/i), {
            target: { value: "4" },
        });

        const submitBtn = screen.getByRole("button", { name: /Lưu Task/i });
        await act(async () => {
            fireEvent.click(submitBtn);
        });

        expect(TaskService.createTask).toHaveBeenCalledWith(
            1,
            expect.objectContaining({
                title: "Task mới chuẩn contract",
                acceptanceCriteria: "Tiêu chí hợp lệ",
                assigneeUserId: 4,
                deadline: "2026-09-20T23:59:59.000Z",
            }),
        );
        await waitFor(() => {
            expect(
                screen.getByText("Task mới chuẩn contract"),
            ).toBeInTheDocument();
        });
    });

    test("6. Chuyển trạng thái sang BLOCKED yêu cầu nhập lý do và gửi reason lên API", async () => {
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
            fireEvent.change(statusSelect, { target: { value: "BLOCKED" } });
        });

        expect(screen.getByTestId("reason-modal")).toBeInTheDocument();

        fireEvent.change(screen.getByPlaceholderText(/Vui lòng nhập lý do/i), {
            target: { value: "Chờ duyệt API" },
        });

        await act(async () => {
            fireEvent.click(screen.getByRole("button", { name: /Xác nhận/i }));
        });

        expect(TaskService.updateTaskStatus).toHaveBeenCalledWith(
            1,
            1,
            "BLOCKED",
            "Chờ duyệt API",
        );
    });

    test("7. Thành viên nhóm không thể chuyển trực tiếp từ TO_DO sang DONE và không có CANCELLED", async () => {
        authService.currentUser.mockReturnValue({
            id: 2,
            username: "member",
            role: "TEAM_MEMBER",
        });

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(
                screen.getByText("Xây dựng API đăng nhập"),
            ).toBeInTheDocument();
        });

        const statusSelect = screen.getByLabelText("Trạng thái task 1");
        const options = Array.from(statusSelect.options).map((o) => o.value);
        expect(options).not.toContain("DONE");
        expect(options).not.toContain("CANCELLED");
    });

    test("8. LECTURER không thể chỉnh sửa trạng thái Task (chỉ hiển thị text)", async () => {
        authService.currentUser.mockReturnValue({
            id: 3,
            username: "teacher",
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

        expect(
            screen.queryByLabelText("Trạng thái task 1"),
        ).not.toBeInTheDocument();
        expect(screen.getByText("TO_DO")).toBeInTheDocument();
    });

    test("9. Xử lý lỗi API khi tải danh sách: 401, 403, 404", async () => {
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
    });

    test("10. Không có projectId thì không gọi API", async () => {
        await act(async () => {
            render(<TaskComponent projectId={null} />);
        });

        await waitFor(() => {
            expect(TaskService.getTasks).not.toHaveBeenCalled();
        });
        expect(screen.queryByTestId("loading-state")).not.toBeInTheDocument();
    });
});
