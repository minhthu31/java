package vn.edu.cnpm.projectsupport.sprint.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.sprint.domain.Sprint;
import vn.edu.cnpm.projectsupport.sprint.dto.SprintResponse;
import vn.edu.cnpm.projectsupport.sprint.repository.SprintRepository;

@Service
@Transactional(readOnly = true)
public class SprintService {

    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;

    public SprintService(
            ProjectRepository projectRepository,
            SprintRepository sprintRepository) {
        this.projectRepository = projectRepository;
        this.sprintRepository = sprintRepository;
    }

    public List<SprintResponse> getProjectSprints(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Không tìm thấy Project " + projectId);
        }

        return sprintRepository.findByProjectIdOrderByIdAsc(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    private SprintResponse toResponse(Sprint sprint) {
        return new SprintResponse(
                sprint.getId(),
                sprint.getProjectId(),
                sprint.getJiraSprintId(),
                sprint.getName(),
                sprint.getState(),
                sprint.getGoal(),
                sprint.getStartDate(),
                sprint.getEndDate(),
                sprint.getLastSyncedAt());
    }
}
