package vn.edu.cnpm.projectsupport.requirement;

import vn.edu.cnpm.projectsupport.common.exception.AccessDeniedException;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RequirementService {

    private final RequirementRepository requirementRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public RequirementResponse create(RequirementCreateRequest request, String currentUserId) {
        String projectId = request.getProjectId();
        
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Không tìm thấy Project với ID: " + projectId);
        }

        Requirement entity = Requirement.builder()
                .projectId(projectId)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .status(RequirementStatus.OPEN)
                .createdBy(currentUserId)
                .build();

        return mapToResponse(requirementRepository.save(entity));
    }

    @Transactional
    public RequirementResponse update(String id, RequirementUpdateRequest request, String currentUserId) {
        Requirement entity = getEntity(id);

        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setPriority(request.getPriority());

        return mapToResponse(requirementRepository.save(entity));
    }

    @Transactional
    public void delete(String id, boolean hardDelete, String currentUserId) {
        Requirement entity = getEntity(id);

        if (hardDelete) {
            requirementRepository.delete(entity);
        } else {
            entity.setIsDeleted(true);
            requirementRepository.save(entity);
        }
    }

    @Transactional(readOnly = true)
    public Page<RequirementResponse> getList(RequirementFilterRequest filter, Pageable pageable, String currentUserId) {
        if (filter == null || !StringUtils.hasText(filter.getProjectId())) {
            throw new AccessDeniedException("Yêu cầu cung cấp projectId để xác thực quyền truy cập.");
        }

        String projectId = filter.getProjectId();
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Không tìm thấy Project với ID: " + projectId);
        }

        return requirementRepository.findAll(RequirementSpecification.filterRequirements(filter), pageable)
                .map(this::mapToResponse);
    }

    private Requirement getEntity(String id) {
        return requirementRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Requirement với ID: " + id));
    }

    private RequirementResponse mapToResponse(Requirement entity) {
        return RequirementResponse.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .priority(entity.getPriority())
                .status(entity.getStatus())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}