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

jest.mock("./TaskService");
jest.mock("./authService", () => ({
    currentUser: () => ({
        id: 1,
        username: "leader.test",
        role: "TEAM_LEADER",
    }),
}));

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
];

describe("TaskComponent Tests", () => {
    beforeEach(() => {
        jest.clearAllMocks();
        TaskService.getTasks.mockResolvedValue(mockTasks);
        TaskService.getTaskMetadata.mockResolvedValue({
            assignees: [{ id: 1, username: "member1", full_name: "Member 1" }],
            sprints: [{ id: 1, name: "Sprint 1" }],
            features: [{ id: 1, title: "Auth" }],
        });
    });

    test("Hiển thị danh sách task và badge NOT_SYNCED", async () => {
        await act(async () => {
            render(<TaskComponent projectId={1} />);
        });

        await waitFor(() => {
            expect(
                screen.getByText("Xây dựng API đăng nhập"),
            ).toBeInTheDocument();
            expect(screen.getByText("NOT_SYNCED")).toBeInTheDocument();
        });
    });

    test("Mở modal chi tiết khi click vào task", async () => {
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

        expect(screen.getByText(/Tiêu chí nghiệm thu/i)).toBeInTheDocument();
        expect(screen.getByText("Trả về JWT Token hợp lệ")).toBeInTheDocument();
    });
});
