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

    const mockProgressResponse = {
        projectId: 1,
        totalRequirements: 12,
        totalFeatures: 8,
        totalSprints: 3,
        totalTasks: 20,
        completedTasks: 14,
        overdueTasks: 2,
        progressPercent: 70.0,
        sprints: [
            {
                sprintId: 1,
                sprintName: "Sprint 1",
                totalTasks: 10,
                completedTasks: 8,
                overdueTasks: 1,
                progressPercent: 80.0,
            },
            {
                sprintId: 2,
                sprintName: "Sprint 2",
                totalTasks: 10,
                completedTasks: 6,
                overdueTasks: 1,
                progressPercent: 60.0,
            },
        ],
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
        expect(progressService.getProjectProgress).not.toHaveBeenCalled();
    });

    test("AC: TEAM_LEADER render đủ Requirement, Feature, Sprint, phần trăm và tiến độ từng sprint", async () => {
        progressService.getSprints.mockResolvedValue(mockSprints);
        progressService.getProjectProgress.mockResolvedValue(
            mockProgressResponse,
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
                screen.getByTestId("metric-total-requirements"),
            ).toHaveTextContent("12");
            expect(
                screen.getByTestId("metric-total-features"),
            ).toHaveTextContent("8");
            expect(
                screen.getByTestId("metric-total-sprints"),
            ).toHaveTextContent("3");
            expect(screen.getByTestId("metric-total-tasks")).toHaveTextContent(
                "20",
            );
            expect(
                screen.getByTestId("metric-completed-tasks"),
            ).toHaveTextContent("14");
            expect(
                screen.getByTestId("metric-overdue-tasks"),
            ).toHaveTextContent("2");
            expect(
                screen.getByTestId("metric-progress-percent"),
            ).toHaveTextContent("70%");
            expect(screen.getByTestId("sprint-percent-1")).toHaveTextContent(
                "80%",
            );
            expect(screen.getByTestId("sprint-percent-2")).toHaveTextContent(
                "60%",
            );
        });
    });

    test("AC: Hiển thị empty state khi không có task và requirement nào", async () => {
        progressService.getSprints.mockResolvedValue(mockSprints);
        progressService.getProjectProgress.mockResolvedValue({
            projectId: 1,
            totalRequirements: 0,
            totalFeatures: 0,
            totalSprints: 0,
            totalTasks: 0,
            completedTasks: 0,
            overdueTasks: 0,
            progressPercent: 0.0,
            sprints: [],
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
        progressService.getProjectProgress.mockRejectedValue(
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

    test("AC: Bộ lọc ngày chuyển đổi đúng mốc [from, to) sang đầu ngày kế tiếp", async () => {
        progressService.getSprints.mockResolvedValue(mockSprints);
        progressService.getProjectProgress.mockResolvedValue(
            mockProgressResponse,
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
                screen.getByRole("option", { name: "Sprint 1" }),
            ).toBeInTheDocument();
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
            expect(progressService.getProjectProgress).toHaveBeenCalledWith(1, {
                sprintId: "1",
                from: "2026-09-01T00:00:00Z",
                to: "2026-09-10T00:00:00Z",
            });
        });
    });
});
