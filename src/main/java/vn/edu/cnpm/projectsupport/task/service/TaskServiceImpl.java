package vn.edu.cnpm.projectsupport.task.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.task.dto.CreateTaskRequest;
import vn.edu.cnpm.projectsupport.task.entity.Task;
import vn.edu.cnpm.projectsupport.task.enums.SyncStatus;
import vn.edu.cnpm.projectsupport.task.enums.TaskStatus;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional
    public Task createTask(String leaderId, CreateTaskRequest request) {
        if (request.getAcceptanceCriteria() == null || request.getAcceptanceCriteria().isBlank()) {
            throw new IllegalArgumentException("Acceptance Criteria không được để trống.");
        }

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setAcceptanceCriteria(request.getAcceptanceCriteria());
        task.setType(request.getType());
        task.setProjectId(request.getProjectId());
        task.setFeatureId(request.getFeatureId());
        task.setCreatedById(leaderId);
        task.setAssigneeId(request.getAssigneeId());
        task.setStatus(TaskStatus.TODO);
        task.setSyncStatus(SyncStatus.NOT_SYNCED);

        return taskRepository.save(task);
    }

    @Override
    @Transactional
    public Task updateTaskStatusByMember(String memberId, String taskId, TaskStatus status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Task với ID: " + taskId));

        if (!memberId.equals(task.getAssigneeId())) {
            throw new IllegalStateException("Bạn không có quyền cập nhật Task của người khác.");
        }

        task.setStatus(status);
        return taskRepository.save(task);
    }
}