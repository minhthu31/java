package vn.edu.cnpm.projectsupport.requirement;

import jakarta.validation.constraints.NotNull;

public class RequirementStatusUpdateRequest {

    @NotNull(message = "Trạng thái là bắt buộc")
    private RequirementStatus status;

    public RequirementStatusUpdateRequest() {
    }

    public RequirementStatusUpdateRequest(RequirementStatus status) {
        this.status = status;
    }

    public RequirementStatus getStatus() {
        return status;
    }

    public void setStatus(RequirementStatus status) {
        this.status = status;
    }
}