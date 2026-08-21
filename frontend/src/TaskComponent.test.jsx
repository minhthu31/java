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
            syncStatus: "NOT_SYNCED",
        },
        {
            id: 2,
            title: "Task IN_PROGRESS",
            issueType: "TASK",
            classification: "FEATURE_RELATED",
            priority: "MEDIUM",
            status: "IN_PROGRESS",
            assignee: { id: 10, username: "dev1", displayName: "Dev One" },
            syncStatus: "SYNCED",
        },
        {
            id: 3,
            title: "Task IN_REVIEW",
            issueType: "TASK",
            classification: "FEATURE_RELATED",
            priority: "MEDIUM",
            status: "IN_REVIEW",
            assignee: { id: 10, username: "dev1", displayName: "Dev One" },
            syncStatus: "SYNCED",
        },
        {
            id: 4,
            title: "Task BLOCKED",
            issueType: "BUG",
            classification: "FEATURE_RELATED",
            priority: "LOW",
            status: "BLOCKED",
            assignee: { id: 11, username: "dev2", displayName: "Dev Two" },
            syncStatus: "FAILED",
        },
        {
            id: 5,
            title: "Task CANCELLED",
            issueType: "TASK",
            classification: "OTHER",
            priority: "LOW",
            status: "CANCELLED",
            assignee: null,
            syncStatus: "NOT_SYNCED",
        },
        {
            id: 6,
            title: "Task DONE",
            issueType: "TASK",
            classification: "FEATURE_RELATED",
            priority: "HIGH",
            status: "DONE",
            assignee: { id: 10, username: "dev1", displayName: "Dev One" },
            syncStatus: "SYNCED",
        },
    ],
    page: 0,
    size: 20,
    totalPages: 1,
    totalElements: 6,
    first: true,
    last: true,
};

describe("TaskComponent Complete State Transition Tests (CNPM-52 / Section 3.3)", () => {
    beforeEach(() => {
        jest.clearAllMocks();
        authService.currentUser.mockReturnValue({
            id: 1,
            username: "leader.user",
            role: "TEAM_LEADER",
        });
        TaskService.getTasks.mockResolvedValue(mockComprehensiveTasks);
        TaskService.getTaskById.mockImplementation((projId, taskId) => {
            const found = mockComprehensiveTasks.content.find(
                (t) => t.id === taskId,
            );
            return Promise.resolve(found);
        });
    });

    test("1. Trạng thái kết thúc CANCELLED bị khóa hoàn toàn, không thể chuyển sang bất kỳ trạng thái nào và không gọi API", async () => {
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

    test("2. Trạng thái kết thúc DONE bị khóa hoàn toàn, không thể chuyển sang bất kỳ trạng thái nào và không gọi API", async () => {
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

    test("3. Chuyển đổi hợp lệ: TO_DO -> IN_PROGRESS", async () => {
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

    test("4. Chuyển đổi hợp lệ: IN_PROGRESS -> IN_REVIEW", async () => {
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

    test("5. Chuyển đổi hợp lệ: IN_REVIEW -> DONE", async () => {
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

    test("6. Chuyển đổi hợp lệ: BLOCKED -> IN_PROGRESS", async () => {
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

    test("7. Team Leader chuyển sang CANCELLED bắt buộc nhập lý do (reason) và gọi updateTaskStatus kèm reason", async () => {
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

    test("8. Chặn chuyển trực tiếp TO_DO -> DONE cho cả Leader và không gọi API", async () => {
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

    test("9. Chuyển sang trang tiếp theo khi click Trang sau và gọi đúng API params page: 1", async () => {
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
                totalElements: 21,
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
                totalElements: 21,
                first: false,
                last: true,
            });

        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(screen.getByText("Page 1 Task")).toBeInTheDocument();
        });

        const nextButton = screen.getByTestId("next-page-btn");
        expect(nextButton).not.toBeDisabled();

        await act(async () => {
            fireEvent.click(nextButton);
        });

        await waitFor(() => {
            expect(TaskService.getTasks).toHaveBeenCalledWith(1, {
                page: 1,
                size: 20,
            });
            expect(screen.getByText("Page 2 Task")).toBeInTheDocument();
        });
    });
});
