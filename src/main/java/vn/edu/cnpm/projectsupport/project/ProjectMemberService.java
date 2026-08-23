package vn.edu.cnpm.projectsupport.project;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;

@Service
@Transactional(readOnly = true)
public class ProjectMemberService {

    private final ProjectRepository projectRepository;

    public ProjectMemberService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @PreAuthorize("@projectAuthorization.canManageTasks(#projectId)")
    public List<ProjectMemberResponse> getActiveMembers(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Không tìm thấy Project với ID: " + projectId);
        }
        return projectRepository.findActiveMembers(projectId).stream()
                .map(member -> new ProjectMemberResponse(
                        member.getId(), member.getUsername(), member.getFullName()))
                .toList();
    }
}
