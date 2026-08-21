import { TaskService } from "./TaskService";
import api from "./api";

jest.mock("./api");

describe("TaskService Contract Unit Tests (CNPM-52)", () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test("1. getTasks gọi đúng URL /projects/:id/tasks (không lặp /api/v1) và giữ nguyên thông tin phân trang (content, totalPages, page)", async () => {
        const mockPageResponse = {
            data: {
                data: {
                    content: [{ id: 1, title: "Task 1" }],
                    page: 0,
                    size: 20,
                    totalPages: 2,
                    totalElements: 25,
                    first: true,
                    last: false,
                },
            },
        };
        api.get.mockResolvedValue(mockPageResponse);

        const result = await TaskService.getTasks(10, { page: 0, size: 20 });

        expect(api.get).toHaveBeenCalledWith("/projects/10/tasks", {
            params: { page: 0, size: 20 },
        });
        expect(result.content).toEqual([{ id: 1, title: "Task 1" }]);
        expect(result.totalPages).toBe(2);
        expect(result.totalElements).toBe(25);
    });

    test("2. createTask gửi payload camelCase và assigneeUserId đúng contract Section 5.3", async () => {
        const payload = {
            title: "Xây dựng Login API",
            acceptanceCriteria: "Đăng nhập hợp lệ trả token",
            issueType: "TASK",
            classification: "FEATURE_RELATED",
            priority: "HIGH",
            assigneeUserId: 4,
        };
        api.post.mockResolvedValue({ data: { data: { id: 501, ...payload } } });

        const result = await TaskService.createTask(10, payload);

        expect(api.post).toHaveBeenCalledWith("/projects/10/tasks", payload);
        expect(result.id).toBe(501);
    });

    test("3. updateTaskStatus gửi status kèm reason khi chuyển sang BLOCKED", async () => {
        api.patch.mockResolvedValue({
            data: { data: { id: 501, status: "BLOCKED" } },
        });

        await TaskService.updateTaskStatus(10, 501, "BLOCKED", "Chờ Jira auth");

        expect(api.patch).toHaveBeenCalledWith(
            "/projects/10/tasks/501/status",
            {
                status: "BLOCKED",
                reason: "Chờ Jira auth",
            },
        );
    });
});
