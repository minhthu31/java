import React from "react";
import {
    render,
    screen,
    fireEvent,
    waitFor,
    act,
    within,
} from "@testing-library/react";
import "@testing-library/jest-dom";
import TaskComponent from "./TaskComponent";
import { TaskService } from "./TaskService";
import * as authService from "./authService";

jest.mock("./TaskService");
jest.mock("./authService");

const mockComprehensiveTasks = {
    content: [
        {
            id: 1,
            title: "Task TO_DO",
            issueType: "TASK",
            classification: "FEATURE_RELATED",
            priority: "HIGH",
            status: "TO_DO",
            assignee: { id: 10, username: "dev1", displayName: "Dev One" },
            deadline: "2026-09-01T23:59:59.000Z",
            syncStatus: "NOT_SYNCED",
            jiraIssueKey: null,
            acceptanceCriteria: "Tiêu chí nghiệm thu 1",
            description: "Mô tả kỹ thuật 1",
        },
        {
            id: 2,
            title: "Task IN_PROGRESS",
            issueType: "TASK",
            classification: "FEATURE_RELATED",
            priority: "MEDIUM",
            status: "IN_PROGRESS",
            assignee: { id: 10, username: "dev1", displayName: "Dev One" },
            deadline: "2026-09-05T23:59:59.000Z",
            syncStatus: "SYNCED",
            jiraIssueKey: "CNPM-65",
            acceptanceCriteria: "Tiêu chí nghiệm thu 2",
            description: "Mô tả kỹ thuật 2",
        },
        {
            id: 3,
            title: "Task IN_REVIEW",
            issueType: "TASK",
            classification: "FEATURE_RELATED",
            priority: "MEDIUM",
            status: "IN_REVIEW",
            assignee: { id: 10, username: "dev1", displayName: "Dev One" },
            deadline: null,
            syncStatus: "SYNCED",
            jiraIssueKey: "CNPM-66",
            acceptanceCriteria: "Tiêu chí nghiệm thu 3",
            description: "",
        },
        {
            id: 4,
            title: "Task BLOCKED",
            issueType: "BUG",
            classification: "FEATURE_RELATED",
            priority: "LOW",
            status: "BLOCKED",
            assignee: { id: 11, username: "dev2", displayName: "Dev Two" },
            deadline: null,
            syncStatus: "SYNC_FAILED",
            jiraIssueKey: null,
            acceptanceCriteria: "Tiêu chí nghiệm thu 4",
            description: "",
        },
        {
            id: 5,
            title: "Task CANCELLED",
            issueType: "TASK",
            classification: "OTHER",
            priority: "LOW",
            status: "CANCELLED",
            assignee: null,
            deadline: null,
            syncStatus: "NOT_SYNCED",
            jiraIssueKey: null,
            acceptanceCriteria: "Tiêu chí nghiệm thu 5",
            description: "",
        },
        {
            id: 6,
            title: "Task DONE",
            issueType: "TASK",
            classification: "FEATURE_RELATED",
            priority: "HIGH",
            status: "DONE",
            assignee: { id: 10, username: "dev1", displayName: "Dev One" },
            deadline: null,
            syncStatus: "SYNCED",
            jiraIssueKey: "CNPM-67",
            acceptanceCriteria: "Tiêu chí nghiệm thu 6",
            description: "",
        },
    ],
    page: 0,
    size: 20,
    totalPages: 1,
    totalElements: 6,
    first: true,
    last: true,
};

describe("TaskComponent Full Scope Tests (CNPM-65)", () => {
    beforeEach(() => {
        jest.clearAllMocks();
        authService.currentUser.mockReturnValue({
            id: 1,
            username: "leader.user",
            role: "TEAM_LEADER",
        });
        TaskService.getActiveMembers.mockResolvedValue([
            { id: 4, username: "member.test", fullName: "Test Team Member" },
        ]);
        TaskService.getTasks.mockResolvedValue(mockComprehensiveTasks);
        TaskService.getTaskById.mockImplementation((projId, taskId) => {
            const found = mockComprehensiveTasks.content.find(
                (t) => t.id === taskId,
            );
            return Promise.resolve(found);
        });
    });

    test("1. Hiển thị danh sách task thành công khi tải trang", async () => {
        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(screen.getByText("Task TO_DO")).toBeInTheDocument();
            expect(screen.getAllByText("Dev One").length).toBeGreaterThan(0);
            expect(screen.getByText("CNPM-65")).toBeInTheDocument();
        });
    });

    test("2. Hiển thị lỗi API và hỗ trợ bấm nút 'Thử lại' để gọi lại đúng trang", async () => {
        TaskService.getTasks.mockRejectedValueOnce({
            response: { status: 500, data: { message: "Lỗi kết nối máy chủ" } },
        });

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(screen.getByTestId("error-message")).toHaveTextContent(
                "Lỗi kết nối máy chủ",
            );
        });

        TaskService.getTasks.mockResolvedValueOnce(mockComprehensiveTasks);
        await act(async () => {
            fireEvent.click(screen.getByTestId("retry-btn"));
        });

        await waitFor(() => {
            expect(screen.getByText("Task TO_DO")).toBeInTheDocument();
        });
    });

    test("3a. Phân quyền: TEAM_LEADER thấy nút 'Tạo Task mới'", async () => {
        authService.currentUser.mockReturnValue({
            id: 1,
            username: "leader.user",
            role: "TEAM_LEADER",
        });

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        expect(screen.getByTestId("create-task-btn")).toBeInTheDocument();
    });

    test("3b. Phân quyền: ADMIN và role không hợp lệ không thấy nút tạo Task và không có control mutation", async () => {
        authService.currentUser.mockReturnValue({
            id: 99,
            username: "admin.user",
            role: "ROLE_ADMIN",
        });

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        expect(screen.queryByTestId("create-task-btn")).not.toBeInTheDocument();
        expect(screen.queryByRole("combobox")).not.toBeInTheDocument();
        expect(screen.getByTestId("error-message")).toHaveTextContent(
            "Bạn không có quyền truy cập module Task của dự án này.",
        );
        expect(TaskService.getTasks).not.toHaveBeenCalled();
        expect(TaskService.createTask).not.toHaveBeenCalled();
        expect(TaskService.updateTaskStatus).not.toHaveBeenCalled();
    });

    test("3c. Phân quyền: LECTURER chỉ đọc (read-only), không thấy nút tạo và không có dropdown trạng thái", async () => {
        authService.currentUser.mockReturnValue({
            id: 88,
            username: "lecturer.user",
            role: "LECTURER",
        });

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(screen.getByText("Task TO_DO")).toBeInTheDocument();
        });

        expect(screen.queryByTestId("create-task-btn")).not.toBeInTheDocument();
        expect(screen.queryByRole("combobox")).not.toBeInTheDocument();
    });

    test("3d. Phân quyền: TEAM_MEMBER chỉ được thao tác dropdown với Task được phân công cho mình", async () => {
        authService.currentUser.mockReturnValue({
            id: 10,
            username: "dev1",
            role: "TEAM_MEMBER",
        });

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(screen.getByText("Task TO_DO")).toBeInTheDocument();
        });

        const row1 = screen.getByTestId("task-row-1");
        expect(within(row1).getByRole("combobox")).toBeInTheDocument();

        const row4 = screen.getByTestId("task-row-4");
        expect(within(row4).queryByRole("combobox")).not.toBeInTheDocument();
        expect(within(row4).getByText("BLOCKED")).toBeInTheDocument();
    });

    test("4. Form tạo task loại trừ SUBTASK và validate các trường bắt buộc", async () => {
        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await act(async () => {
            fireEvent.click(screen.getByTestId("create-task-btn"));
        });

        const issueTypeSelect = screen.getByLabelText(/Loại công việc/i);
        const options = Array.from(issueTypeSelect.options).map((o) => o.value);
        expect(options).not.toContain("SUBTASK");
        expect(options).toEqual(["TASK", "EPIC", "STORY", "BUG"]);

        await act(async () => {
            fireEvent.click(screen.getByRole("button", { name: /Lưu Task/i }));
        });

        expect(screen.getByTestId("form-error")).toHaveTextContent(
            "Vui lòng điền đầy đủ: Tiêu đề, Tiêu chí nghiệm thu, Issue Type và Priority.",
        );
    });

    test("4b. Validate title tối đa 255 ký tự và deadline không ở quá khứ", async () => {
        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await act(async () => {
            fireEvent.click(screen.getByTestId("create-task-btn"));
        });

        const longTitle = "A".repeat(256);
        fireEvent.change(screen.getByLabelText(/Tiêu đề/i), {
            target: { value: longTitle },
        });
        fireEvent.change(screen.getByLabelText(/Tiêu chí nghiệm thu/i), {
            target: { value: "Criteria test" },
        });

        await act(async () => {
            fireEvent.click(screen.getByRole("button", { name: /Lưu Task/i }));
        });

        expect(screen.getByTestId("form-error")).toHaveTextContent(
            "Tiêu đề không được vượt quá 255 ký tự.",
        );

        fireEvent.change(screen.getByLabelText(/Tiêu đề/i), {
            target: { value: "Task title hợp lệ" },
        });
        fireEvent.change(screen.getByLabelText(/Hạn chót/i), {
            target: { value: "2020-01-01" },
        });

        await act(async () => {
            fireEvent.click(screen.getByRole("button", { name: /Lưu Task/i }));
        });

        expect(screen.getByTestId("form-error")).toHaveTextContent(
            "Hạn chót (Deadline) không được ở trong quá khứ.",
        );
    });

    test("5. Tạo Task thành công với payload camelCase chuẩn contract", async () => {
        TaskService.createTask.mockResolvedValueOnce({
            id: 100,
            title: "Task mới tạo",
        });

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
            target: { value: "Phải pass test" },
        });
        fireEvent.change(screen.getByLabelText(/Mô tả/i), {
            target: { value: "Mô tả chi tiết" },
        });
        fireEvent.change(screen.getByLabelText(/Người thực hiện/i), {
            target: { value: "4" },
        });

        await act(async () => {
            fireEvent.click(screen.getByRole("button", { name: /Lưu Task/i }));
        });

        expect(TaskService.createTask).toHaveBeenCalledWith(
            1,
            expect.objectContaining({
                title: "Task mới tạo",
                acceptanceCriteria: "Phải pass test",
                description: "Mô tả chi tiết",
                issueType: "TASK",
                priority: "MEDIUM",
                classification: "FEATURE_RELATED",
                assigneeUserId: 4,
            }),
        );
    });

    test("6. Mở Modal chi tiết Task gọi đúng getTaskById API và đóng modal", async () => {
        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(screen.getByText("Task TO_DO")).toBeInTheDocument();
        });

        await act(async () => {
            fireEvent.click(screen.getByText("Task TO_DO"));
        });

        expect(TaskService.getTaskById).toHaveBeenCalledWith(1, 1);
        expect(screen.getByTestId("detail-modal")).toBeInTheDocument();
        expect(screen.getByText("Tiêu chí nghiệm thu 1")).toBeInTheDocument();

        await act(async () => {
            fireEvent.click(screen.getByTestId("close-modal-btn"));
        });

        expect(screen.queryByTestId("detail-modal")).not.toBeInTheDocument();
    });

    test("7. Phân trang: Điều hướng trang tiếp theo gọi API với params page: 1", async () => {
        TaskService.getTasks
            .mockResolvedValueOnce({
                content: [
                    {
                        id: 1,
                        title: "Page 1 Task",
                        issueType: "TASK",
                        status: "TO_DO",
                    },
                ],
                page: 0,
                size: 20,
                totalPages: 2,
                totalElements: 25,
                first: true,
                last: false,
            })
            .mockResolvedValueOnce({
                content: [
                    {
                        id: 21,
                        title: "Page 2 Task",
                        issueType: "TASK",
                        status: "TO_DO",
                    },
                ],
                page: 1,
                size: 20,
                totalPages: 2,
                totalElements: 25,
                first: false,
                last: true,
            });

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(screen.getByText("Page 1 Task")).toBeInTheDocument();
        });

        const nextBtn = screen.getByTestId("next-page-btn");
        expect(nextBtn).toBeEnabled();

        await act(async () => {
            fireEvent.click(nextBtn);
        });

        await waitFor(() => {
            expect(TaskService.getTasks).toHaveBeenCalledWith(1, {
                page: 1,
                size: 20,
            });
            expect(screen.getByText("Page 2 Task")).toBeInTheDocument();
        });
    });

    test("8. Trạng thái kết thúc CANCELLED bị khóa hoàn toàn, không thể chuyển đổi và không gọi API", async () => {
        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(screen.getByText("Task CANCELLED")).toBeInTheDocument();
        });

        const row5 = screen.getByTestId("task-row-5");
        expect(within(row5).queryByRole("combobox")).not.toBeInTheDocument();
        expect(within(row5).getByText("CANCELLED")).toBeInTheDocument();
        expect(TaskService.updateTaskStatus).not.toHaveBeenCalled();
    });

    test("9. Trạng thái kết thúc DONE bị khóa hoàn toàn, không thể chuyển đổi và không gọi API", async () => {
        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(screen.getByText("Task DONE")).toBeInTheDocument();
        });

        const row6 = screen.getByTestId("task-row-6");
        expect(within(row6).queryByRole("combobox")).not.toBeInTheDocument();
        expect(within(row6).getByText("DONE")).toBeInTheDocument();
        expect(TaskService.updateTaskStatus).not.toHaveBeenCalled();
    });

    test("10. Chuyển đổi hợp lệ: TO_DO -> IN_PROGRESS", async () => {
        TaskService.updateTaskStatus.mockResolvedValue({});

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(screen.getByText("Task TO_DO")).toBeInTheDocument();
        });

        const statusSelect = screen.getByLabelText("Trạng thái task 1");
        await act(async () => {
            fireEvent.change(statusSelect, {
                target: { value: "IN_PROGRESS" },
            });
        });

        expect(TaskService.updateTaskStatus).toHaveBeenCalledWith(
            1,
            1,
            "IN_PROGRESS",
            "",
        );
    });

    test("11. Chuyển đổi hợp lệ: IN_PROGRESS -> IN_REVIEW", async () => {
        TaskService.updateTaskStatus.mockResolvedValue({});

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(screen.getByText("Task IN_PROGRESS")).toBeInTheDocument();
        });

        const statusSelect = screen.getByLabelText("Trạng thái task 2");
        await act(async () => {
            fireEvent.change(statusSelect, { target: { value: "IN_REVIEW" } });
        });

        expect(TaskService.updateTaskStatus).toHaveBeenCalledWith(
            1,
            2,
            "IN_REVIEW",
            "",
        );
    });

    test("12. Chuyển đổi hợp lệ: IN_REVIEW -> DONE", async () => {
        TaskService.updateTaskStatus.mockResolvedValue({});

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(screen.getByText("Task IN_REVIEW")).toBeInTheDocument();
        });

        const statusSelect = screen.getByLabelText("Trạng thái task 3");
        await act(async () => {
            fireEvent.change(statusSelect, { target: { value: "DONE" } });
        });

        expect(TaskService.updateTaskStatus).toHaveBeenCalledWith(
            1,
            3,
            "DONE",
            "",
        );
    });

    test("13. Chuyển đổi hợp lệ: BLOCKED -> IN_PROGRESS", async () => {
        TaskService.updateTaskStatus.mockResolvedValue({});

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(screen.getByText("Task BLOCKED")).toBeInTheDocument();
        });

        const statusSelect = screen.getByLabelText("Trạng thái task 4");
        await act(async () => {
            fireEvent.change(statusSelect, {
                target: { value: "IN_PROGRESS" },
            });
        });

        expect(TaskService.updateTaskStatus).toHaveBeenCalledWith(
            1,
            4,
            "IN_PROGRESS",
            "",
        );
    });

    test("14. Team Leader chuyển sang CANCELLED bắt buộc nhập lý do (reason)", async () => {
        TaskService.updateTaskStatus.mockResolvedValue({});

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(screen.getByText("Task TO_DO")).toBeInTheDocument();
        });

        const statusSelect = screen.getByLabelText("Trạng thái task 1");
        await act(async () => {
            fireEvent.change(statusSelect, { target: { value: "CANCELLED" } });
        });

        expect(screen.getByTestId("reason-modal")).toBeInTheDocument();

        fireEvent.change(screen.getByPlaceholderText(/Vui lòng nhập lý do/i), {
            target: { value: "Yêu cầu thay đổi từ khách hàng" },
        });

        await act(async () => {
            fireEvent.click(screen.getByRole("button", { name: /Xác nhận/i }));
        });

        expect(TaskService.updateTaskStatus).toHaveBeenCalledWith(
            1,
            1,
            "CANCELLED",
            "Yêu cầu thay đổi từ khách hàng",
        );
    });

    test("15. Chặn chuyển trực tiếp TO_DO -> DONE cho cả Leader và không gọi API", async () => {
        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(screen.getByText("Task TO_DO")).toBeInTheDocument();
        });

        const statusSelect = screen.getByLabelText("Trạng thái task 1");
        const options = Array.from(statusSelect.options).map((o) => o.value);
        expect(options).not.toContain("DONE");
        expect(TaskService.updateTaskStatus).not.toHaveBeenCalled();
    });

    test("16. Hiển thị trạng thái Loading khi getTasks đang pending", () => {
        TaskService.getTasks.mockReturnValueOnce(new Promise(() => {}));

        render(<TaskComponent projectId={1} />);

        expect(screen.getByTestId("loading-state")).toBeInTheDocument();
        expect(
            screen.getByText("Đang tải danh sách công việc..."),
        ).toBeInTheDocument();
    });

    test("17. Hiển thị thông báo lỗi khi createTask API gặp sự cố (403 Forbidden)", async () => {
        TaskService.createTask.mockRejectedValueOnce({
            response: {
                status: 403,
                data: {
                    message: "Chỉ Trưởng nhóm (Leader) mới có quyền tạo Task.",
                },
            },
        });

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await act(async () => {
            fireEvent.click(screen.getByTestId("create-task-btn"));
        });

        fireEvent.change(screen.getByLabelText(/Tiêu đề/i), {
            target: { value: "Task Test Lỗi" },
        });
        fireEvent.change(screen.getByLabelText(/Tiêu chí nghiệm thu/i), {
            target: { value: "Tiêu chí test" },
        });

        await act(async () => {
            fireEvent.click(screen.getByRole("button", { name: /Lưu Task/i }));
        });

        expect(screen.getByTestId("form-error")).toHaveTextContent(
            "Chỉ Trưởng nhóm (Leader) mới có quyền tạo Task.",
        );
    });

    test("18. getTaskById gặp lỗi API sẽ hiển thị cảnh báo lỗi rõ ràng trên modal chi tiết", async () => {
        TaskService.getTaskById.mockRejectedValueOnce({
            response: {
                status: 500,
                data: { message: "Máy chủ không thể phản hồi chi tiết task." },
            },
        });

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(screen.getByText("Task TO_DO")).toBeInTheDocument();
        });

        await act(async () => {
            fireEvent.click(screen.getByText("Task TO_DO"));
        });

        expect(screen.getByTestId("detail-modal")).toBeInTheDocument();
        expect(screen.getByTestId("detail-error")).toHaveTextContent(
            "Máy chủ không thể phản hồi chi tiết task.",
        );
    });
});
