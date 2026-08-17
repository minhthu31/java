package vn.edu.cnpm.projectsupport.project;

import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;

    @Override
    @Transactional(readOnly = true)
    public void validateProjectExists(String projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Dự án không tồn tại với ID: " + projectId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getGroupIdByProjectId(String projectId) {
        return projectRepository.findGroupIdById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin nhóm liên kết với dự án ID: " + projectId));
    }
}
