import React, { useEffect, useState } from "react";
import { RequirementService } from "./RequirementService";

const PRIORITY_OPTIONS = [
    { value: "HIGHEST", label: "Highest" },
    { value: "HIGH", label: "High" },
    { value: "MEDIUM", label: "Medium" },
    { value: "LOW", label: "Low" },
    { value: "LOWEST", label: "Lowest" },
];

export const RequirementForm = ({
    projectId,
    requirementId = null,
    initialData = null,
    onCancel,
    onSuccess,
}) => {
    const editId = requirementId || initialData?.id || null;
    const isEditMode = Boolean(editId);

    const [formData, setFormData] = useState({
        title: initialData?.title || "",
        description: initialData?.description || "",
        actor: initialData?.actor || "",
        priority: initialData?.priority || "MEDIUM",
        precondition: initialData?.precondition || "",
        mainFlow: initialData?.mainFlow || "",
        alternativeFlow: initialData?.alternativeFlow || "",
        exceptionFlow: initialData?.exceptionFlow || "",
        postcondition: initialData?.postcondition || "",
    });

    const [errors, setErrors] = useState({});
    const [apiError, setApiError] = useState(null);
    const [isLoadingDetail, setIsLoadingDetail] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);

    useEffect(() => {
        let isMounted = true;
        if (isEditMode && editId && projectId) {
            setIsLoadingDetail(true);
            RequirementService.getRequirementById(projectId, editId)
                .then((detail) => {
                    if (isMounted && detail) {
                        setFormData({
                            title: detail.title || "",
                            description: detail.description || "",
                            actor: detail.actor || "",
                            priority: detail.priority || "MEDIUM",
                            precondition: detail.precondition || "",
                            mainFlow: detail.mainFlow || "",
                            alternativeFlow: detail.alternativeFlow || "",
                            exceptionFlow: detail.exceptionFlow || "",
                            postcondition: detail.postcondition || "",
                        });
                    }
                })
                .catch((err) => {
                    if (isMounted)
                        setApiError(
                            err.message ||
                                "Không thể tải chi tiết Requirement.",
                        );
                })
                .finally(() => {
                    if (isMounted) setIsLoadingDetail(false);
                });
        }
        return () => {
            isMounted = false;
        };
    }, [isEditMode, editId, projectId]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
        if (errors[name]) {
            setErrors((prev) => ({ ...prev, [name]: null }));
        }
    };

    const validate = () => {
        const newErrors = {};
        if (!formData.title.trim()) newErrors.title = "Vui lòng nhập Title";
        if (!formData.actor.trim()) newErrors.actor = "Vui lòng nhập Actor";
        if (!formData.description.trim())
            newErrors.description = "Vui lòng nhập Description";
        if (!formData.mainFlow.trim())
            newErrors.mainFlow = "Vui lòng nhập Main Flow";
        return newErrors;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (isSubmitting) return;

        const validationErrors = validate();
        if (Object.keys(validationErrors).length > 0) {
            setErrors(validationErrors);
            return;
        }

        setIsSubmitting(true);
        setApiError(null);
        setErrors({});

        try {
            if (isEditMode) {
                await RequirementService.updateRequirement(
                    projectId,
                    editId,
                    formData,
                );
            } else {
                await RequirementService.createRequirement(projectId, formData);
            }
            onSuccess?.();
        } catch (err) {
            setApiError(err.message || "Có lỗi xảy ra khi lưu Requirement.");
            // Hiển thị fieldErrors backend trả về
            if (err.fieldErrors && typeof err.fieldErrors === "object") {
                setErrors(err.fieldErrors);
            }
        } finally {
            setIsSubmitting(false);
        }
    };

    if (isLoadingDetail) {
        return (
            <div
                style={{
                    padding: "40px",
                    textAlign: "center",
                    backgroundColor: "#fff",
                    borderRadius: "6px",
                }}
            >
                Đang tải thông tin chi tiết Requirement...
            </div>
        );
    }

    return (
        <div
            style={{
                maxWidth: "960px",
                margin: "0 auto",
                backgroundColor: "#ffffff",
                border: "1px solid #cbd5e1",
                borderRadius: "6px",
                padding: "24px",
            }}
        >
            <div
                style={{
                    borderBottom: "1px solid #e2e8f0",
                    paddingBottom: "12px",
                    marginBottom: "20px",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                }}
            >
                <h2 style={{ margin: 0, fontSize: "18px", color: "#1e293b" }}>
                    {isEditMode ? "Edit Requirement" : "Create Requirement"}
                </h2>
                <button
                    type="button"
                    onClick={onCancel}
                    disabled={isSubmitting}
                    style={{
                        background: "#f1f5f9",
                        border: "1px solid #cbd5e1",
                        padding: "6px 12px",
                        borderRadius: "4px",
                        cursor: isSubmitting ? "not-allowed" : "pointer",
                        fontSize: "13px",
                    }}
                >
                    Back to List
                </button>
            </div>

            {apiError && (
                <div
                    role="alert"
                    style={{
                        backgroundColor: "#fee2e2",
                        color: "#b91c1c",
                        padding: "10px 14px",
                        borderRadius: "4px",
                        marginBottom: "16px",
                        fontSize: "13px",
                    }}
                >
                    {apiError}
                </div>
            )}

            <form onSubmit={handleSubmit}>
                {/* TITLE */}
                <div style={{ marginBottom: "14px" }}>
                    <label
                        style={{
                            display: "block",
                            fontSize: "13px",
                            fontWeight: "600",
                            color: "#334155",
                            marginBottom: "4px",
                        }}
                    >
                        Requirement Title{" "}
                        <span style={{ color: "#ef4444" }}>*</span>
                    </label>
                    <input
                        type="text"
                        name="title"
                        placeholder="Nhập tiêu đề yêu cầu"
                        value={formData.title}
                        onChange={handleChange}
                        disabled={isSubmitting}
                        style={{
                            width: "100%",
                            padding: "8px 10px",
                            borderRadius: "4px",
                            border: `1px solid ${errors.title ? "#ef4444" : "#cbd5e1"}`,
                            boxSizing: "border-box",
                        }}
                    />
                    {errors.title && (
                        <span
                            style={{
                                color: "#ef4444",
                                fontSize: "12px",
                                display: "block",
                                marginTop: "4px",
                            }}
                        >
                            {errors.title}
                        </span>
                    )}
                </div>

                <div
                    style={{
                        display: "grid",
                        gridTemplateColumns: "1fr 1fr",
                        gap: "16px",
                        marginBottom: "14px",
                    }}
                >
                    <div>
                        <label
                            style={{
                                display: "block",
                                fontSize: "13px",
                                fontWeight: "600",
                                color: "#334155",
                                marginBottom: "4px",
                            }}
                        >
                            Actor <span style={{ color: "#ef4444" }}>*</span>
                        </label>
                        <input
                            type="text"
                            name="actor"
                            placeholder="Ví dụ: Team Leader, Giảng viên"
                            value={formData.actor}
                            onChange={handleChange}
                            disabled={isSubmitting}
                            style={{
                                width: "100%",
                                padding: "8px 10px",
                                borderRadius: "4px",
                                border: `1px solid ${errors.actor ? "#ef4444" : "#cbd5e1"}`,
                                boxSizing: "border-box",
                            }}
                        />
                        {errors.actor && (
                            <span
                                style={{
                                    color: "#ef4444",
                                    fontSize: "12px",
                                    display: "block",
                                    marginTop: "4px",
                                }}
                            >
                                {errors.actor}
                            </span>
                        )}
                    </div>

                    <div>
                        <label
                            style={{
                                display: "block",
                                fontSize: "13px",
                                fontWeight: "600",
                                color: "#334155",
                                marginBottom: "4px",
                            }}
                        >
                            Priority
                        </label>
                        <select
                            name="priority"
                            aria-label="Priority"
                            value={formData.priority}
                            onChange={handleChange}
                            disabled={isSubmitting}
                            style={{
                                width: "100%",
                                padding: "8px 10px",
                                borderRadius: "4px",
                                border: "1px solid #cbd5e1",
                                boxSizing: "border-box",
                            }}
                        >
                            {PRIORITY_OPTIONS.map((opt) => (
                                <option key={opt.value} value={opt.value}>
                                    {opt.label}
                                </option>
                            ))}
                        </select>
                    </div>
                </div>

                <div style={{ marginBottom: "14px" }}>
                    <label
                        style={{
                            display: "block",
                            fontSize: "13px",
                            fontWeight: "600",
                            color: "#334155",
                            marginBottom: "4px",
                        }}
                    >
                        Description <span style={{ color: "#ef4444" }}>*</span>
                    </label>
                    <textarea
                        name="description"
                        placeholder="Mô tả chi tiết yêu cầu"
                        rows="3"
                        value={formData.description}
                        onChange={handleChange}
                        disabled={isSubmitting}
                        style={{
                            width: "100%",
                            padding: "8px 10px",
                            borderRadius: "4px",
                            border: `1px solid ${errors.description ? "#ef4444" : "#cbd5e1"}`,
                            boxSizing: "border-box",
                        }}
                    />
                    {errors.description && (
                        <span
                            style={{
                                color: "#ef4444",
                                fontSize: "12px",
                                display: "block",
                                marginTop: "4px",
                            }}
                        >
                            {errors.description}
                        </span>
                    )}
                </div>

                <div
                    style={{
                        display: "grid",
                        gridTemplateColumns: "1fr 1fr",
                        gap: "16px",
                        marginBottom: "14px",
                    }}
                >
                    <div>
                        <label
                            style={{
                                display: "block",
                                fontSize: "13px",
                                fontWeight: "600",
                                color: "#334155",
                                marginBottom: "4px",
                            }}
                        >
                            Precondition
                        </label>
                        <textarea
                            name="precondition"
                            placeholder="Điều kiện tiên quyết"
                            rows="2"
                            value={formData.precondition}
                            onChange={handleChange}
                            disabled={isSubmitting}
                            style={{
                                width: "100%",
                                padding: "8px 10px",
                                borderRadius: "4px",
                                border: "1px solid #cbd5e1",
                                boxSizing: "border-box",
                            }}
                        />
                    </div>
                    <div>
                        <label
                            style={{
                                display: "block",
                                fontSize: "13px",
                                fontWeight: "600",
                                color: "#334155",
                                marginBottom: "4px",
                            }}
                        >
                            Postcondition
                        </label>
                        <textarea
                            name="postcondition"
                            placeholder="Kết quả sau khi thực hiện"
                            rows="2"
                            value={formData.postcondition}
                            onChange={handleChange}
                            disabled={isSubmitting}
                            style={{
                                width: "100%",
                                padding: "8px 10px",
                                borderRadius: "4px",
                                border: "1px solid #cbd5e1",
                                boxSizing: "border-box",
                            }}
                        />
                    </div>
                </div>

                <div style={{ marginBottom: "14px" }}>
                    <label
                        style={{
                            display: "block",
                            fontSize: "13px",
                            fontWeight: "600",
                            color: "#334155",
                            marginBottom: "4px",
                        }}
                    >
                        Main Flow <span style={{ color: "#ef4444" }}>*</span>
                    </label>
                    <textarea
                        name="mainFlow"
                        placeholder="Luồng sự kiện chính"
                        rows="3"
                        value={formData.mainFlow}
                        onChange={handleChange}
                        disabled={isSubmitting}
                        style={{
                            width: "100%",
                            padding: "8px 10px",
                            borderRadius: "4px",
                            border: `1px solid ${errors.mainFlow ? "#ef4444" : "#cbd5e1"}`,
                            boxSizing: "border-box",
                        }}
                    />
                    {errors.mainFlow && (
                        <span
                            style={{
                                color: "#ef4444",
                                fontSize: "12px",
                                display: "block",
                                marginTop: "4px",
                            }}
                        >
                            {errors.mainFlow}
                        </span>
                    )}
                </div>

                <div
                    style={{
                        display: "grid",
                        gridTemplateColumns: "1fr 1fr",
                        gap: "16px",
                        marginBottom: "20px",
                    }}
                >
                    <div>
                        <label
                            style={{
                                display: "block",
                                fontSize: "13px",
                                fontWeight: "600",
                                color: "#334155",
                                marginBottom: "4px",
                            }}
                        >
                            Alternative Flow
                        </label>
                        <textarea
                            name="alternativeFlow"
                            placeholder="Luồng thay thế"
                            rows="2"
                            value={formData.alternativeFlow}
                            onChange={handleChange}
                            disabled={isSubmitting}
                            style={{
                                width: "100%",
                                padding: "8px 10px",
                                borderRadius: "4px",
                                border: "1px solid #cbd5e1",
                                boxSizing: "border-box",
                            }}
                        />
                    </div>
                    <div>
                        <label
                            style={{
                                display: "block",
                                fontSize: "13px",
                                fontWeight: "600",
                                color: "#334155",
                                marginBottom: "4px",
                            }}
                        >
                            Exception Flow
                        </label>
                        <textarea
                            name="exceptionFlow"
                            placeholder="Luồng ngoại lệ"
                            rows="2"
                            value={formData.exceptionFlow}
                            onChange={handleChange}
                            disabled={isSubmitting}
                            style={{
                                width: "100%",
                                padding: "8px 10px",
                                borderRadius: "4px",
                                border: "1px solid #cbd5e1",
                                boxSizing: "border-box",
                            }}
                        />
                    </div>
                </div>

                <div
                    style={{
                        display: "flex",
                        justifyContent: "flex-end",
                        gap: "10px",
                        paddingTop: "10px",
                        borderTop: "1px solid #e2e8f0",
                    }}
                >
                    <button
                        type="button"
                        onClick={onCancel}
                        disabled={isSubmitting}
                        style={{
                            padding: "8px 18px",
                            borderRadius: "4px",
                            border: "1px solid #cbd5e1",
                            background: "#fff",
                            cursor: isSubmitting ? "not-allowed" : "pointer",
                            fontSize: "13px",
                        }}
                    >
                        Cancel
                    </button>
                    <button
                        type="submit"
                        disabled={isSubmitting}
                        style={{
                            padding: "8px 22px",
                            borderRadius: "4px",
                            border: "none",
                            backgroundColor: isSubmitting
                                ? "#93c5fd"
                                : "#2563eb",
                            color: "#fff",
                            cursor: isSubmitting ? "not-allowed" : "pointer",
                            fontWeight: "600",
                            fontSize: "13px",
                        }}
                    >
                        {isSubmitting
                            ? "Saving..."
                            : isEditMode
                              ? "Update"
                              : "Save"}
                    </button>
                </div>
            </form>
        </div>
    );
};

export default RequirementForm;
