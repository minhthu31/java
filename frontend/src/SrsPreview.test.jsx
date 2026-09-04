import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import SrsPreview from "./SrsPreview";
import { RequirementService } from "./RequirementService";

jest.mock("./RequirementService");

describe("CNPM-72: SRS Preview integration", () => {
    beforeEach(() => {
        jest.clearAllMocks();
        window.print = jest.fn();
    });

    test("Leader xem được dữ liệu Requirement dưới dạng SRS", async () => {
        RequirementService.getRequirements.mockResolvedValue({
            content: [
                {
                    id: 11,
                    jiraIssueKey: "CNPM-101",
                    title: "Quản lý yêu cầu",
                    actor: "Team Leader",
                    status: "DRAFT",
                    priority: "HIGH",
                    description: "Tạo và cập nhật yêu cầu",
                    mainFlow: "Leader nhập thông tin và lưu",
                },
            ],
        });

        render(
            <SrsPreview projectId={1} currentUserRole="TEAM_LEADER" />,
        );

        expect(
            await screen.findByRole("heading", { name: /Quản lý yêu cầu/ }),
        ).toBeInTheDocument();
        expect(screen.getByText(/CNPM-101/)).toBeInTheDocument();
        expect(RequirementService.getRequirements).toHaveBeenCalledWith(1, {
            page: 0,
            size: 100,
            sort: "updatedAt,asc",
        });

        fireEvent.click(screen.getByText("In / Lưu PDF"));
        expect(window.print).toHaveBeenCalledTimes(1);
    });

    test("Member không được gọi API SRS", () => {
        render(<SrsPreview projectId={1} currentUserRole="TEAM_MEMBER" />);

        expect(screen.getByRole("alert")).toHaveTextContent(
            "không có quyền xem SRS",
        );
        expect(RequirementService.getRequirements).not.toHaveBeenCalled();
    });

    test("Hiển thị lỗi và cho phép tải lại", async () => {
        RequirementService.getRequirements
            .mockRejectedValueOnce(new Error("API tạm thời gián đoạn"))
            .mockResolvedValueOnce({ content: [] });

        render(<SrsPreview projectId={1} currentUserRole="LECTURER" />);

        expect(
            await screen.findByText("API tạm thời gián đoạn"),
        ).toBeInTheDocument();
        fireEvent.click(screen.getByText("Thử lại"));
        expect(
            await screen.findByText("Chưa có Requirement để tạo bản xem trước SRS."),
        ).toBeInTheDocument();
    });
});
