package vn.edu.cnpm.projectsupport.requirement;

import vn.edu.cnpm.projectsupport.common.api.PageResponse;

public interface RequirementService {

    PageResponse<RequirementResponse> getRequirements(Long projectId, RequirementFilterRequest filter);

    RequirementResponse createRequirement(Long projectId, RequirementCreateRequest request);

    RequirementResponse getRequirementById(Long projectId, Long requirementId);

    RequirementResponse updateRequirement(Long projectId, Long requirementId, RequirementUpdateRequest request);

    RequirementResponse updateStatus(Long projectId, Long requirementId, RequirementStatusUpdateRequest request);

    void deleteRequirement(Long projectId, Long requirementId);
}