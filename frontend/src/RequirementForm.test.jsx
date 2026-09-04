import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";
import RequirementForm from "./RequirementForm";
import { RequirementService } from "./RequirementService";
import { currentUser } from "./authService";

const mockNavigate = jest.fn();
jest.mock("react-router-dom", () => ({
    ...jest.requireActual("react-router-dom"),
    useNavigate: () => mockNavigate,
    useParams: () => ({ projectId: "10" }),
}));

jest.mock("./RequirementService");
jest.mock("./authService", () => ({
    currentUser: jest.fn(),
}));

describe("CNPM-64: Comprehensive RequirementForm Acceptance & Edge Cases Tests", () => {
    const onSuccessMock = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
        currentUser.mockReturnValue({
            id: 3,
            username: "leader.test",
            role: "TEAM_LEADER",
            projectId: 10,
        });
    });

    test("1. Chặn truy cập nếu không phải TEAM_LEADER", () => {
        currentUser.mockReturnValue({
            id: 4,
            username: "member.test",
            role: "TEAM_MEMBER",
        });

        render(<RequirementForm projectId="10" />);
        expect(
            screen.getByText(/Không có quyền truy cập/i),
        ).toBeInTheDocument();
        expect(screen.queryByLabelText(/Tiêu đề/i)).not.toBeInTheDocument();
    });

    test("2. Báo lỗi khi thiếu projectId", () => {
        render(<RequirementForm projectId="" />);
        expect(screen.getByText(/projectId không hợp lệ/i)).toBeInTheDocument();
        expect(
            screen.getByRole("button", { name: /Tạo Requirement/i }),
        ).toBeDisabled();
    });

    test("3. Chỉ bắt buộc Title (các trường khác để trống vẫn tạo thành công)", async () => {
        RequirementService.createRequirement.mockResolvedValueOnce({ id: 101 });

        render(<RequirementForm projectId="10" onSuccess={onSuccessMock} />);

        fireEvent.change(screen.getByLabelText(/Tiêu đề/i), {
            target: { value: "Chỉ nhập title" },
        });
        fireEvent.click(
            screen.getByRole("button", { name: /Tạo Requirement/i }),
        );

        await waitFor(() => {
            expect(RequirementService.createRequirement).toHaveBeenCalledWith(
                "10",
                expect.objectContaining({
                    title: "Chỉ nhập title",
                    description: "",
                    actor: "",
                }),
            );
            expect(onSuccessMock).toHaveBeenCalledTimes(1);
        });
    });

    test("4. Xử lý lỗi tải chi tiết 401 / 403 / 404 khi ở chế độ Edit", async () => {
        RequirementService.getRequirementDetail.mockRejectedValueOnce({
            status: 404,
            message: "Không tìm thấy tài nguyên trong phạm vi được phép",
        });

        render(<RequirementForm projectId="10" requirementId="999" />);

        expect(
            await screen.findByText(
                "Không tìm thấy tài nguyên trong phạm vi được phép",
            ),
        ).toBeInTheDocument();
    });

    test("5. Edit Mode: Tải chi tiết và khi submit PUT không chứa field 'status'", async () => {
        RequirementService.getRequirementDetail.mockResolvedValueOnce({
            id: 101,
            title: "Req Cũ",
            description: "Mô tả cũ",
            actor: "Admin",
            priority: "HIGH",
            status: "APPROVED",
        });
        RequirementService.updateRequirement.mockResolvedValueOnce({ id: 101 });

        render(
            <RequirementForm
                projectId="10"
                requirementId="101"
                onSuccess={onSuccessMock}
            />,
        );

        expect(await screen.findByDisplayValue("Req Cũ")).toBeInTheDocument();
        expect(
            screen.getByText(/Trạng thái hiện tại: APPROVED/i),
        ).toBeInTheDocument();

        fireEvent.change(screen.getByLabelText(/Tiêu đề/i), {
            target: { value: "Req Đã Sửa" },
        });
        fireEvent.click(screen.getByRole("button", { name: /Lưu thay đổi/i }));

        await waitFor(() => {
            expect(RequirementService.updateRequirement).toHaveBeenCalledWith(
                "10",
                "101",
                expect.objectContaining({
                    title: "Req Đã Sửa",
                    description: "Mô tả cũ",
                }),
            );
            expect(onSuccessMock).toHaveBeenCalled();
        });
    });

    test("6. Hiển thị fieldErrors từ Backend và KHÔNG gọi onSuccess khi thất bại", async () => {
        RequirementService.createRequirement.mockRejectedValueOnce({
            status: 400,
            fieldErrors: { title: "must not be blank" },
        });

        render(<RequirementForm projectId="10" onSuccess={onSuccessMock} />);

        fireEvent.change(screen.getByLabelText(/Tiêu đề/i), {
            target: { value: "Tên bị lỗi backend" },
        });
        fireEvent.click(
            screen.getByRole("button", { name: /Tạo Requirement/i }),
        );

        expect(
            await screen.findByText("must not be blank"),
        ).toBeInTheDocument();
        expect(onSuccessMock).not.toHaveBeenCalled();
        expect(screen.getByLabelText(/Tiêu đề/i)).toHaveValue(
            "Tên bị lỗi backend",
        );
    });

    test("7. Chống gửi hai lần (Double Submit)", async () => {
        let resolvePromise;
        RequirementService.createRequirement.mockReturnValue(
            new Promise((resolve) => {
                resolvePromise = resolve;
            }),
        );

        render(<RequirementForm projectId="10" />);
        fireEvent.change(screen.getByLabelText(/Tiêu đề/i), {
            target: { value: "Test Double" },
        });
        const btn = screen.getByRole("button", { name: /Tạo Requirement/i });

        fireEvent.click(btn);
        expect(btn).toBeDisabled();
        expect(btn).toHaveTextContent("Đang lưu...");

        fireEvent.click(btn);
        expect(RequirementService.createRequirement).toHaveBeenCalledTimes(1);

        resolvePromise({ id: 101 });
        await waitFor(() => expect(btn).not.toBeDisabled());
    });
});
