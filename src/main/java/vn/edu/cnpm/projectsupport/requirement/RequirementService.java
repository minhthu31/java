package vn.edu.cnpm.projectsupport.requirement;

import vn.edu.cnpm.projectsupport.common.exception.AccessDeniedException;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.group.GroupService;
import vn.edu.cnpm.projectsupport.project.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequirementService {

    private final RequirementRepository requirementRepository;
    private final ProjectService projectService;
    private final GroupService groupService;

    @Transactional
    public RequirementResponse create(RequirementCreateRequest request, String currentUserId) {
        String projectIdStr = String.valueOf(request.getProjectId());
        projectService.validateProjectExists(projectIdStr);
        String groupId = projectService.getGroupIdByProjectId(projectIdStr);

        if (!groupService.isLeader(groupId, currentUserId)) {
            throw new AccessDeniedException("Chỉ Leader mới có quyền tạo Requirement.");
        }

        Requirement entity = Requirement.builder()
                .projectId(projectIdStr)
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
        String groupId = projectService.getGroupIdByProjectId(entity.getProjectId());

        if (!groupService.isLeader(groupId, currentUserId)) {
            throw new AccessDeniedException("Chỉ Leader mới có quyền sửa Requirement.");
        }

        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setPriority(request.getPriority());

        return mapToResponse(requirementRepository.save(entity));
    }

    @Transactional
    public RequirementResponse updateStatus(String id, RequirementStatusUpdateRequest request, String currentUserId) {
        Requirement entity = getEntity(id);
        String groupId = projectService.getGroupIdByProjectId(entity.getProjectId());

        if (!groupService.hasAccess(groupId, currentUserId)) {
            throw new AccessDeniedException("Người ngoài nhóm không có quyền cập nhật trạng thái.");
        }

        entity.setStatus(request.getStatus());
        return mapToResponse(requirementRepository.save(entity));
    }

    @Transactional
    public void delete(String id, boolean hardDelete, String currentUserId) {
        Requirement entity = getEntity(id);
        String groupId = projectService.getGroupIdByProjectId(entity.getProjectId());

        if (!groupService.isLeader(groupId, currentUserId)) {
            throw new AccessDeniedException("Chỉ Leader mới có quyền xóa Requirement.");
        }

        if (hardDelete) {
            requirementRepository.delete(entity);
        } else {
            entity.setIsDeleted(true);
            requirementRepository.save(entity);
        }
    }

    @Transactional(readOnly = true)
    public RequirementResponse getDetail(String id, String currentUserId) {
        Requirement entity = getEntity(id);
        String groupId = projectService.getGroupIdByProjectId(entity.getProjectId());

        if (!groupService.hasAccess(groupId, currentUserId)) {
            throw new AccessDeniedException("Bạn không có quyền xem thông tin Requirement này.");
        }

        return mapToResponse(entity);
    }

    @Transactional(readOnly = true)
    public Page<RequirementResponse> getList(RequirementFilterRequest filter, Pageable pageable, String currentUserId) {
        if (filter.getProjectId() != null) {
            String projectIdStr = String.valueOf(filter.getProjectId());
            projectService.validateProjectExists(projectIdStr);
            String groupId = projectService.getGroupIdByProjectId(projectIdStr);

            if (!groupService.hasAccess(groupId, currentUserId)) {
                throw new AccessDeniedException("Bạn không có quyền xem danh sách Requirement của nhóm này.");
            }
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