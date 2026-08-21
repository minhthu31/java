package vn.edu.cnpm.projectsupport.requirement;

import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.common.api.PageResponse;
import vn.edu.cnpm.projectsupport.common.exception.InvalidStatusTransitionException;
import vn.edu.cnpm.projectsupport.common.exception.ResourceInUseException;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@Service
public class RequirementService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORTS = Set.of("title", "priority", "status", "createdAt", "updatedAt");

    private final RequirementRepository requirementRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public RequirementService(
            RequirementRepository requirementRepository,
            ProjectRepository projectRepository,
            TaskRepository taskRepository) {
        this.requirementRepository = requirementRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    @PreAuthorize("@projectAuthorization.canManageRequirements(#projectId)")
    public RequirementResponse createRequirement(Long projectId, RequirementCreateRequest request) {
        requireProject(projectId);

        Requirement requirement = new Requirement(projectId, request.getTitle().trim());
        applyContent(requirement, request.getDescription(), request.getActor(), request.getPriority(),
                request.getPrecondition(), request.getMainFlow(), request.getAlternativeFlow(),
                request.getExceptionFlow(), request.getPostcondition());
        requirement.setStatus(request.getStatus() == null ? RequirementStatus.DRAFT : request.getStatus());
        return toResponse(requirementRepository.save(requirement));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@projectAuthorization.canViewRequirements(#projectId)")
    public PageResponse<RequirementResponse> getRequirements(Long projectId, RequirementFilterRequest filter) {
        requireProject(projectId);
        RequirementFilterRequest safeFilter = filter == null ? new RequirementFilterRequest() : filter;
        PageRequest pageRequest = pageRequest(safeFilter);
        Page<RequirementResponse> page = requirementRepository
                .findAll(RequirementSpecification.matches(projectId, safeFilter), pageRequest)
                .map(this::toResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@projectAuthorization.canViewRequirements(#projectId)")
    public RequirementResponse getRequirementById(Long projectId, Long requirementId) {
        return toResponse(requireRequirement(projectId, requirementId));
    }

    @Transactional
    @PreAuthorize("@projectAuthorization.canManageRequirements(#projectId)")
    public RequirementResponse updateRequirement(
            Long projectId, Long requirementId, RequirementUpdateRequest request) {
        Requirement requirement = requireRequirement(projectId, requirementId);
        requirement.setTitle(request.getTitle().trim());
        applyContent(requirement, request.getDescription(), request.getActor(), request.getPriority(),
                request.getPrecondition(), request.getMainFlow(), request.getAlternativeFlow(),
                request.getExceptionFlow(), request.getPostcondition());
        return toResponse(requirementRepository.save(requirement));
    }

    @Transactional
    @PreAuthorize("@projectAuthorization.canManageRequirements(#projectId)")
    public RequirementResponse updateStatus(
            Long projectId, Long requirementId, RequirementStatusUpdateRequest request) {
        Requirement requirement = requireRequirement(projectId, requirementId);
        validateTransition(requirement.getStatus(), request.getStatus());
        requirement.setStatus(request.getStatus());
        return toResponse(requirementRepository.save(requirement));
    }

    @Transactional
    @PreAuthorize("@projectAuthorization.canManageRequirements(#projectId)")
    public void deleteRequirement(Long projectId, Long requirementId) {
        Requirement requirement = requireRequirement(projectId, requirementId);
        if (requirement.getStatus() != RequirementStatus.DRAFT) {
            throw new ResourceInUseException("Chỉ được xóa Requirement ở trạng thái DRAFT");
        }
        if (taskRepository.existsByRequirementId(requirementId)) {
            throw new ResourceInUseException("Requirement đang được Task tham chiếu; hãy chuyển sang ARCHIVED");
        }
        requirementRepository.delete(requirement);
    }

    private void requireProject(Long projectId) {
        if (projectId == null || !projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Không tìm thấy Project với ID: " + projectId);
        }
    }

    private Requirement requireRequirement(Long projectId, Long requirementId) {
        requireProject(projectId);
        return requirementRepository.findByIdAndProjectId(requirementId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy Requirement " + requirementId + " trong Project " + projectId));
    }

    private PageRequest pageRequest(RequirementFilterRequest filter) {
        int page = filter.getPage() == null ? 0 : Math.max(filter.getPage(), 0);
        int size = filter.getSize() == null ? 20 : Math.min(Math.max(filter.getSize(), 1), MAX_PAGE_SIZE);
        String[] parts = filter.getSort() == null ? new String[0] : filter.getSort().split(",", 2);
        String property = parts.length > 0 && ALLOWED_SORTS.contains(parts[0]) ? parts[0] : "updatedAt";
        Sort.Direction direction = parts.length == 2 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, property));
    }

    private void validateTransition(RequirementStatus from, RequirementStatus to) {
        if (to == null || from == to) {
            throw new InvalidStatusTransitionException("Trạng thái Requirement mới không hợp lệ");
        }
        boolean valid = switch (from) {
            case DRAFT -> to == RequirementStatus.APPROVED || to == RequirementStatus.ARCHIVED;
            case APPROVED -> to == RequirementStatus.DRAFT
                    || to == RequirementStatus.SYNCED
                    || to == RequirementStatus.ARCHIVED;
            case SYNCED -> to == RequirementStatus.APPROVED || to == RequirementStatus.ARCHIVED;
            case ARCHIVED -> false;
        };
        if (!valid) {
            throw new InvalidStatusTransitionException(
                    "Chuyển trạng thái Requirement không hợp lệ: " + from + " -> " + to);
        }
    }

    private void applyContent(
            Requirement target,
            String description,
            String actor,
            Priority priority,
            String precondition,
            String mainFlow,
            String alternativeFlow,
            String exceptionFlow,
            String postcondition) {
        target.setDescription(description);
        target.setActor(actor);
        target.setPriority(priority);
        target.setPrecondition(precondition);
        target.setMainFlow(mainFlow);
        target.setAlternativeFlow(alternativeFlow);
        target.setExceptionFlow(exceptionFlow);
        target.setPostcondition(postcondition);
    }

    private RequirementResponse toResponse(Requirement requirement) {
        RequirementResponse response = new RequirementResponse();
        response.setId(requirement.getId());
        response.setProjectId(requirement.getProjectId());
        response.setJiraIssueKey(requirement.getJiraIssueKey());
        response.setTitle(requirement.getTitle());
        response.setDescription(requirement.getDescription());
        response.setActor(requirement.getActor());
        response.setPriority(requirement.getPriority());
        response.setPrecondition(requirement.getPrecondition());
        response.setMainFlow(requirement.getMainFlow());
        response.setAlternativeFlow(requirement.getAlternativeFlow());
        response.setExceptionFlow(requirement.getExceptionFlow());
        response.setPostcondition(requirement.getPostcondition());
        response.setStatus(requirement.getStatus());
        response.setCreatedAt(requirement.getCreatedAt());
        response.setUpdatedAt(requirement.getUpdatedAt());
        return response;
    }
}
