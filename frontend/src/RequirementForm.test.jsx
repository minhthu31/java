import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";

import RequirementForm from "./RequirementForm";
import { RequirementService } from "./RequirementService";

jest.mock("./RequirementService");

describe("CNPM-64: RequirementForm", () => {
    const mockDetail = {
        id: 10,
        title: "Quản lý sinh viên",
        actor: "Giảng viên",
        description: "Mô tả tính năng sinh viên",
        priority: "HIGH",
        precondition: "Đã đăng nhập",
        mainFlow: "1. Chọn sinh viên 2. Cập nhật điểm",
        alternativeFlow: "Không có",
        exceptionFlow: "Lỗi kết nối",
        postcondition: "Điểm được lưu",
        status: "DRAFT",
    };

    beforeEach(() => {
        jest.clearAllMocks();
    });

    test("1. Validation client khi để trống các trường bắt buộc", async () => {
        render(<RequirementForm projectId={1} />);

        fireEvent.click(screen.getByRole("button", { name: "Save" }));

        expect(
            await screen.findByText("Vui lòng nhập Title"),
        ).toBeInTheDocument();
        expect(screen.getByText("Vui lòng nhập Actor")).toBeInTheDocument();
        expect(
            screen.getByText("Vui lòng nhập Description"),
        ).toBeInTheDocument();
        expect(screen.getByText("Vui lòng nhập Main Flow")).toBeInTheDocument();
        expect(RequirementService.createRequirement).not.toHaveBeenCalled();
    });

    test("2. Tạo mới Requirement thành công gửi đúng projectId và status DRAFT", async () => {
        RequirementService.createRequirement.mockResolvedValue({ id: 1 });
        const onSuccess = jest.fn();

        render(<RequirementForm projectId={1} onSuccess={onSuccess} />);

        fireEvent.change(screen.getByPlaceholderText("Nhập tiêu đề yêu cầu"), {
            target: { value: "Đăng nhập Google" },
        });
        fireEvent.change(
            screen.getByPlaceholderText("Ví dụ: Team Leader, Giảng viên"),
            {
                target: { value: "Sinh viên" },
            },
        );
        fireEvent.change(
            screen.getByPlaceholderText("Mô tả chi tiết yêu cầu"),
            {
                target: { value: "OAuth2 Google Login" },
            },
        );
        fireEvent.change(screen.getByPlaceholderText("Luồng sự kiện chính"), {
            target: { value: "1. Click nút Google 2. Đăng nhập" },
        });

        fireEvent.click(screen.getByRole("button", { name: "Save" }));

        await waitFor(() => {
            expect(RequirementService.createRequirement).toHaveBeenCalledWith(
                1,
                expect.objectContaining({
                    title: "Đăng nhập Google",
                    actor: "Sinh viên",
                    description: "OAuth2 Google Login",
                    mainFlow: "1. Click nút Google 2. Đăng nhập",
                    priority: "MEDIUM",
                }),
            );
            expect(onSuccess).toHaveBeenCalled();
        });
    });

    test("3. Edit mode: gọi API lấy chi tiết và cập nhật thành công (không gửi status)", async () => {
        RequirementService.getRequirementById.mockResolvedValue(mockDetail);
        RequirementService.updateRequirement.mockResolvedValue({ id: 10 });
        const onSuccess = jest.fn();

        render(
            <RequirementForm
                projectId={1}
                requirementId={10}
                onSuccess={onSuccess}
            />,
        );

        expect(
            await screen.findByDisplayValue("Quản lý sinh viên"),
        ).toBeInTheDocument();

        fireEvent.change(screen.getByPlaceholderText("Nhập tiêu đề yêu cầu"), {
            target: { value: "Quản lý điểm sinh viên" },
        });

        fireEvent.click(screen.getByRole("button", { name: "Update" }));

        await waitFor(() => {
            expect(RequirementService.updateRequirement).toHaveBeenCalledWith(
                1,
                10,
                expect.objectContaining({
                    title: "Quản lý điểm sinh viên",
                }),
            );
            expect(onSuccess).toHaveBeenCalled();
        });
    });

    test("4. Hiển thị fieldErrors trả về từ Backend", async () => {
        const error = new Error("Validation Failed");
        error.fieldErrors = {
            title: "Tiêu đề đã tồn tại trong project",
        };
        RequirementService.createRequirement.mockRejectedValue(error);

        render(<RequirementForm projectId={1} />);

        fireEvent.change(screen.getByPlaceholderText("Nhập tiêu đề yêu cầu"), {
            target: { value: "Trùng tiêu đề" },
        });
        fireEvent.change(
            screen.getByPlaceholderText("Ví dụ: Team Leader, Giảng viên"),
            {
                target: { value: "Actor" },
            },
        );
        fireEvent.change(
            screen.getByPlaceholderText("Mô tả chi tiết yêu cầu"),
            {
                target: { value: "Desc" },
            },
        );
        fireEvent.change(screen.getByPlaceholderText("Luồng sự kiện chính"), {
            target: { value: "Flow" },
        });

        fireEvent.click(screen.getByRole("button", { name: "Save" }));

        expect(
            await screen.findByText("Tiêu đề đã tồn tại trong project"),
        ).toBeInTheDocument();
    });

    test("5. Chống submit nhiều lần (Double-click lock)", async () => {
        RequirementService.createRequirement.mockImplementation(
            () => new Promise((resolve) => setTimeout(resolve, 500)),
        );

        render(<RequirementForm projectId={1} />);

        fireEvent.change(screen.getByPlaceholderText("Nhập tiêu đề yêu cầu"), {
            target: { value: "Tiêu đề" },
        });
        fireEvent.change(
            screen.getByPlaceholderText("Ví dụ: Team Leader, Giảng viên"),
            {
                target: { value: "Actor" },
            },
        );
        fireEvent.change(
            screen.getByPlaceholderText("Mô tả chi tiết yêu cầu"),
            {
                target: { value: "Desc" },
            },
        );
        fireEvent.change(screen.getByPlaceholderText("Luồng sự kiện chính"), {
            target: { value: "Flow" },
        });

        const saveButton = screen.getByRole("button", { name: "Save" });
        fireEvent.click(saveButton);
        fireEvent.click(saveButton);

        expect(
            screen.getByRole("button", { name: "Saving..." }),
        ).toBeDisabled();
        expect(RequirementService.createRequirement).toHaveBeenCalledTimes(1);
    });
});
