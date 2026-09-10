import React from "react";
import "@testing-library/jest-dom";
import {
    render,
    screen,
    waitFor,
    fireEvent,
    act,
} from "@testing-library/react";
import { ProjectProgressComponent } from "./ProjectProgressComponent";
import { progressService } from "./progressService";

jest.mock("./progressService");

describe("ProjectProgressComponent - Task 106", () => {
    const mockSprints = [
        { id: 1, name: "Sprint 1" },
        { id: 2, name: "Sprint 2" },
    ];

    const mockSummaryResponse = {
        projectId: 1,
        sprintId: 1,
        taskMetrics: {
            totalTasks: 10,
            completedTasks: 4,
            overdueTasks: 2,
            unassignedTasks: 1,
            tasksByStatus: {
                TO_DO: 3,
                IN_PROGRESS: 3,
                DONE: 4,
                IN_REVIEW: 0,
                BLOCKED: 0,
                CANCELLED: 0,
            },
        },
    };

    beforeEach(() => {
        jest.clearAllMocks();
    });

    test("AC: Người không có quyền (TEAM_MEMBER) bị chặn không truy cập được", () => {
        render(
            <ProjectProgressComponent
                projectId={1}
                currentUserRole="TEAM_MEMBER"
            />,
        );

        expect(
            screen.getByTestId("unauthorized-progress-message"),
        ).toBeInTheDocument();
        expect(progressService.getProjectSummary).not.toHaveBeenCalled();
    });

    test("AC: TEAM_LEADER truy cập thành công, render đúng 4 thẻ chỉ số và phân bổ trạng thái", async () => {
        progressService.getSprints.mockResolvedValue(mockSprints);
        progressService.getProjectSummary.mockResolvedValue(
            mockSummaryResponse,
        );

        await act(async () => {
            render(
                <ProjectProgressComponent
                    projectId={1}
                    currentUserRole="TEAM_LEADER"
                />,
            );
        });

        await waitFor(() => {
            expect(screen.getByTestId("metric-total-tasks")).toHaveTextContent(
                "10",
            );
            expect(
                screen.getByTestId("metric-completed-tasks"),
            ).toHaveTextContent("4");
            expect(
                screen.getByTestId("metric-overdue-tasks"),
            ).toHaveTextContent("2");
            expect(
                screen.getByTestId("metric-unassigned-tasks"),
            ).toHaveTextContent("1");
            expect(screen.getByTestId("status-count-TO_DO")).toHaveTextContent(
                "3",
            );
            expect(screen.getByTestId("status-count-DONE")).toHaveTextContent(
                "4",
            );
        });
    });

    test("AC: Hiển thị empty state khi không có task nào", async () => {
        progressService.getSprints.mockResolvedValue(mockSprints);
        progressService.getProjectSummary.mockResolvedValue({
            projectId: 1,
            taskMetrics: {
                totalTasks: 0,
                completedTasks: 0,
                overdueTasks: 0,
                unassignedTasks: 0,
                tasksByStatus: {},
            },
        });

        await act(async () => {
            render(
                <ProjectProgressComponent
                    projectId={1}
                    currentUserRole="LECTURER"
                />,
            );
        });

        await waitFor(() => {
            expect(
                screen.getByTestId("progress-empty-state"),
            ).toBeInTheDocument();
        });
    });

    test("AC: Hiển thị thông báo lỗi khi API thất bại", async () => {
        progressService.getSprints.mockResolvedValue(mockSprints);
        progressService.getProjectSummary.mockRejectedValue(
            new Error("Network Error"),
        );

        await act(async () => {
            render(
                <ProjectProgressComponent
                    projectId={1}
                    currentUserRole="TEAM_LEADER"
                />,
            );
        });

        await waitFor(() => {
            expect(
                screen.getByTestId("progress-error-banner"),
            ).toBeInTheDocument();
        });
    });

    test("AC: Hoạt động của bộ lọc theo Sprint và Thời gian", async () => {
        progressService.getSprints.mockResolvedValue(mockSprints);
        progressService.getProjectSummary.mockResolvedValue(
            mockSummaryResponse,
        );

        await act(async () => {
            render(
                <ProjectProgressComponent
                    projectId={1}
                    currentUserRole="TEAM_LEADER"
                />,
            );
        });

        await waitFor(() => {
            expect(screen.getByText("Sprint 1")).toBeInTheDocument();
        });

        await act(async () => {
            fireEvent.change(screen.getByLabelText("Sprint:"), {
                target: { value: "1" },
            });
            fireEvent.change(screen.getByLabelText("Từ:"), {
                target: { value: "2026-09-01" },
            });
            fireEvent.change(screen.getByLabelText("Đến:"), {
                target: { value: "2026-09-09" },
            });
            fireEvent.click(screen.getByRole("button", { name: "Áp dụng" }));
        });

        await waitFor(() => {
            expect(progressService.getProjectSummary).toHaveBeenCalledWith(1, {
                sprintId: "1",
                from: "2026-09-01T00:00:00Z",
                to: "2026-09-09T23:59:59Z",
            });
        });
    });
});
