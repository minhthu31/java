import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { RequirementService } from "./RequirementService";
import { currentUser } from "./authService";

const PRIORITY_OPTIONS = [
    { value: "HIGHEST", label: "HIGHEST" },
    { value: "HIGH", label: "HIGH" },
    { value: "MEDIUM", label: "MEDIUM" },
    { value: "LOW", label: "LOW" },
    { value: "LOWEST", label: "LOWEST" },
];

export default function RequirementForm({
    projectId: propProjectId,
    requirementId: propRequirementId,
    onSuccess,
    onCancel,
}) {
    const navigate = useNavigate();
    const params = useParams();

    const projectId =
        propProjectId !== undefined ? propProjectId : params.projectId;
    const requirementId = propRequirementId || params.requirementId;
    const isEditMode = Boolean(requirementId);

    const user = typeof currentUser === "function" ? currentUser() : null;
    const isTeamLeader = user?.role === "TEAM_LEADER";

    const [formData, setFormData] = useState({
        title: "",
        priority: "MEDIUM",
        actor: "",
        description: "",
        precondition: "",
        postcondition: "",
        mainFlow: "",
        alternativeFlow: "",
        exceptionFlow: "",
        jiraIssueKey: "",
    });

    const [currentStatus, setCurrentStatus] = useState("DRAFT");
    const [submitting, setSubmitting] = useState(false);
    const [loadingDetail, setLoadingDetail] = useState(false);
    const [fieldErrors, setFieldErrors] = useState({});
    const [generalError, setGeneralError] = useState(
        !projectId || Number(projectId) <= 0
            ? "Không tìm thấy thông tin dự án (projectId không hợp lệ)."
            : "",
    );

    useEffect(() => {
        if (!isTeamLeader) return;

        if (isEditMode && projectId) {
            setLoadingDetail(true);
            RequirementService.getRequirementDetail(projectId, requirementId)
                .then((data) => {
                    setFormData({
                        title: data.title || "",
                        priority: data.priority || "MEDIUM",
                        actor: data.actor || "",
                        description: data.description || "",
                        precondition: data.precondition || "",
                        postcondition: data.postcondition || "",
                        mainFlow: data.mainFlow || "",
                        alternativeFlow: data.alternativeFlow || "",
                        exceptionFlow: data.exceptionFlow || "",
                        jiraIssueKey: data.jiraIssueKey || "",
                    });
                    if (data.status) {
                        setCurrentStatus(data.status);
                    }
                })
                .catch((err) => {
                    setGeneralError(
                        err.message || "Không thể tải chi tiết Requirement.",
                    );
                })
                .finally(() => {
                    setLoadingDetail(false);
                });
        }
    }, [isEditMode, projectId, requirementId, isTeamLeader]);

    if (!isTeamLeader) {
        return (
            <div
                style={{
                    padding: "32px",
                    textAlign: "center",
                    color: "#bf2600",
                    backgroundColor: "#ffebe6",
                    borderRadius: "6px",
                    margin: "20px auto",
                    maxWidth: "800px",
                }}
            >
                Bạn không có quyền truy cập hoặc thao tác với biểu mẫu
                Requirement.
            </div>
        );
    }

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
        if (fieldErrors[name]) {
            setFieldErrors((prev) => ({ ...prev, [name]: undefined }));
        }
    };

    const validateForm = () => {
        const errors = {};
        if (!formData.title.trim()) {
            errors.title = "Tiêu đề không được để trống";
        }
        return errors;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setGeneralError("");

        const validationErrors = validateForm();
        if (Object.keys(validationErrors).length > 0) {
            setFieldErrors(validationErrors);
            return;
        }

        setSubmitting(true);

        try {
            if (isEditMode) {
                // PUT: Không gửi trường `status`, chuẩn hóa rỗng -> null
                const updatePayload = {
                    title: formData.title.trim(),
                    priority: formData.priority,
                    actor: formData.actor ? formData.actor.trim() : "",
                    description: formData.description
                        ? formData.description.trim()
                        : "",
                    precondition: formData.precondition
                        ? formData.precondition.trim()
                        : "",
                    postcondition: formData.postcondition
                        ? formData.postcondition.trim()
                        : "",
                    mainFlow: formData.mainFlow ? formData.mainFlow.trim() : "",
                    alternativeFlow: formData.alternativeFlow
                        ? formData.alternativeFlow.trim()
                        : "",
                    exceptionFlow: formData.exceptionFlow
                        ? formData.exceptionFlow.trim()
                        : "",
                    jiraIssueKey: formData.jiraIssueKey
                        ? formData.jiraIssueKey.trim()
                        : null,
                };
                await RequirementService.updateRequirement(
                    projectId,
                    requirementId,
                    updatePayload,
                );
            } else {
                // POST: Luôn khởi tạo DRAFT, không gửi jiraIssueKey rỗng
                const createPayload = {
                    title: formData.title.trim(),
                    priority: formData.priority,
                    actor: formData.actor ? formData.actor.trim() : "",
                    description: formData.description
                        ? formData.description.trim()
                        : "",
                    precondition: formData.precondition
                        ? formData.precondition.trim()
                        : "",
                    postcondition: formData.postcondition
                        ? formData.postcondition.trim()
                        : "",
                    mainFlow: formData.mainFlow ? formData.mainFlow.trim() : "",
                    alternativeFlow: formData.alternativeFlow
                        ? formData.alternativeFlow.trim()
                        : "",
                    exceptionFlow: formData.exceptionFlow
                        ? formData.exceptionFlow.trim()
                        : "",
                    status: "DRAFT",
                };
                await RequirementService.createRequirement(
                    projectId,
                    createPayload,
                );
            }

            if (typeof onSuccess === "function") {
                onSuccess();
            } else {
                navigate(-1);
            }
        } catch (error) {
            if (error?.fieldErrors) {
                setFieldErrors(error.fieldErrors);
            }
            setGeneralError(
                error?.message || "Đã xảy ra lỗi khi lưu Requirement.",
            );
        } finally {
            setSubmitting(false);
        }
    };

    const handleCancel = () => {
        if (typeof onCancel === "function") {
            onCancel();
        } else {
            navigate(-1);
        }
    };

    const inputStyle = (hasError) => ({
        width: "100%",
        boxSizing: "border-box",
        padding: "8px 12px",
        border: `1px solid ${hasError ? "#de350b" : "#dfe1e6"}`,
        borderRadius: "4px",
        fontSize: "14px",
        backgroundColor: "#fff",
        color: "#172b4d",
        outline: "none",
    });

    const labelStyle = {
        display: "block",
        marginBottom: "6px",
        fontSize: "13px",
        fontWeight: 600,
        color: "#172b4d",
    };

    const errorTextStyle = {
        color: "#de350b",
        fontSize: "12px",
        marginTop: "4px",
    };

    if (loadingDetail) {
        return (
            <div
                style={{
                    padding: "40px",
                    textAlign: "center",
                    color: "#6b778c",
                }}
            >
                Đang tải dữ liệu Requirement...
            </div>
        );
    }

    return (
        <div
            style={{
                maxWidth: "1000px",
                margin: "20px auto",
                backgroundColor: "#fff",
                border: "1px solid #ebecf0",
                borderRadius: "8px",
                padding: "24px",
                boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
            }}
        >
            <div
                style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    borderBottom: "1px solid #ebecf0",
                    paddingBottom: "16px",
                    marginBottom: "20px",
                }}
            >
                <h2 style={{ margin: 0, fontSize: "20px", color: "#172b4d" }}>
                    {isEditMode
                        ? "Chỉnh sửa Requirement"
                        : "Tạo mới Requirement"}
                </h2>
                {isEditMode && (
                    <span
                        style={{
                            backgroundColor: "#deebff",
                            color: "#0747a6",
                            padding: "4px 10px",
                            borderRadius: "12px",
                            fontSize: "12px",
                            fontWeight: 600,
                        }}
                    >
                        Trạng thái hiện tại: {currentStatus}
                    </span>
                )}
            </div>

            {generalError && (
                <div
                    role="alert"
                    style={{
                        padding: "12px 16px",
                        backgroundColor: "#ffebe6",
                        border: "1px solid #ffbdad",
                        borderRadius: "4px",
                        color: "#bf2600",
                        marginBottom: "20px",
                        fontSize: "14px",
                    }}
                >
                    {generalError}
                </div>
            )}

            <form onSubmit={handleSubmit} noValidate>
                <div style={{ marginBottom: "16px" }}>
                    <label htmlFor="title" style={labelStyle}>
                        Tiêu đề (Title){" "}
                        <span style={{ color: "#de350b" }}>*</span>
                    </label>
                    <input
                        type="text"
                        id="title"
                        name="title"
                        placeholder="Nhập tiêu đề Requirement..."
                        value={formData.title}
                        onChange={handleChange}
                        style={inputStyle(!!fieldErrors.title)}
                    />
                    {fieldErrors.title && (
                        <div style={errorTextStyle}>{fieldErrors.title}</div>
                    )}
                </div>

                <div
                    style={{
                        display: "grid",
                        gridTemplateColumns: "1fr 1fr",
                        gap: "16px",
                        marginBottom: "16px",
                    }}
                >
                    <div>
                        <label htmlFor="priority" style={labelStyle}>
                            Độ ưu tiên (Priority)
                        </label>
                        <select
                            id="priority"
                            name="priority"
                            value={formData.priority}
                            onChange={handleChange}
                            style={inputStyle(false)}
                        >
                            {PRIORITY_OPTIONS.map((opt) => (
                                <option key={opt.value} value={opt.value}>
                                    {opt.label}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div>
                        <label htmlFor="actor" style={labelStyle}>
                            Tác nhân (Actor)
                        </label>
                        <input
                            type="text"
                            id="actor"
                            name="actor"
                            placeholder="Ví dụ: Team Leader, Lecturer..."
                            value={formData.actor}
                            onChange={handleChange}
                            style={inputStyle(false)}
                        />
                    </div>
                </div>

                <div style={{ marginBottom: "16px" }}>
                    <label htmlFor="description" style={labelStyle}>
                        Mô tả (Description)
                    </label>
                    <textarea
                        id="description"
                        name="description"
                        rows="3"
                        placeholder="Mô tả tóm tắt..."
                        value={formData.description}
                        onChange={handleChange}
                        style={inputStyle(false)}
                    />
                </div>

                <div
                    style={{
                        display: "grid",
                        gridTemplateColumns: "1fr 1fr",
                        gap: "16px",
                        marginBottom: "16px",
                    }}
                >
                    <div>
                        <label htmlFor="precondition" style={labelStyle}>
                            Tiền điều kiện (Precondition)
                        </label>
                        <textarea
                            id="precondition"
                            name="precondition"
                            rows="2"
                            placeholder="Điều kiện ban đầu..."
                            value={formData.precondition}
                            onChange={handleChange}
                            style={inputStyle(false)}
                        />
                    </div>
                    <div>
                        <label htmlFor="postcondition" style={labelStyle}>
                            Hậu điều kiện (Postcondition)
                        </label>
                        <textarea
                            id="postcondition"
                            name="postcondition"
                            rows="2"
                            placeholder="Kết quả nhận được..."
                            value={formData.postcondition}
                            onChange={handleChange}
                            style={inputStyle(false)}
                        />
                    </div>
                </div>

                <div style={{ marginBottom: "16px" }}>
                    <label htmlFor="mainFlow" style={labelStyle}>
                        Luồng chính (Main Flow)
                    </label>
                    <textarea
                        id="mainFlow"
                        name="mainFlow"
                        rows="4"
                        placeholder={"1. Bước 1\n2. Bước 2\n3. Bước 3..."}
                        value={formData.mainFlow}
                        onChange={handleChange}
                        style={{ ...inputStyle(false), fontFamily: "inherit" }}
                    />
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
                        <label htmlFor="alternativeFlow" style={labelStyle}>
                            Luồng rẽ nhánh (Alternative Flow)
                        </label>
                        <textarea
                            id="alternativeFlow"
                            name="alternativeFlow"
                            rows="3"
                            placeholder="Nhánh xử lý khác..."
                            value={formData.alternativeFlow}
                            onChange={handleChange}
                            style={{
                                ...inputStyle(false),
                                fontFamily: "inherit",
                            }}
                        />
                    </div>
                    <div>
                        <label htmlFor="exceptionFlow" style={labelStyle}>
                            Luồng ngoại lệ (Exception Flow)
                        </label>
                        <textarea
                            id="exceptionFlow"
                            name="exceptionFlow"
                            rows="3"
                            placeholder="Xử lý lỗi hoặc ngoại lệ..."
                            value={formData.exceptionFlow}
                            onChange={handleChange}
                            style={{
                                ...inputStyle(false),
                                fontFamily: "inherit",
                            }}
                        />
                    </div>
                </div>

                <div
                    style={{
                        display: "flex",
                        justifyContent: "flex-end",
                        gap: "10px",
                        borderTop: "1px solid #ebecf0",
                        paddingTop: "16px",
                    }}
                >
                    <button
                        type="button"
                        onClick={handleCancel}
                        disabled={submitting}
                        style={{
                            padding: "8px 16px",
                            backgroundColor: "#fff",
                            color: "#172b4d",
                            border: "1px solid #dfe1e6",
                            borderRadius: "4px",
                            cursor: submitting ? "not-allowed" : "pointer",
                            fontSize: "14px",
                            fontWeight: 500,
                        }}
                    >
                        Hủy bỏ
                    </button>
                    <button
                        type="submit"
                        disabled={
                            submitting || Boolean(generalError && !isEditMode)
                        }
                        style={{
                            padding: "8px 18px",
                            backgroundColor:
                                submitting || (generalError && !isEditMode)
                                    ? "#a5b2c6"
                                    : "#0052cc",
                            color: "#fff",
                            border: "none",
                            borderRadius: "4px",
                            cursor:
                                submitting || (generalError && !isEditMode)
                                    ? "not-allowed"
                                    : "pointer",
                            fontSize: "14px",
                            fontWeight: 600,
                        }}
                    >
                        {submitting
                            ? "Đang lưu..."
                            : isEditMode
                              ? "Lưu thay đổi"
                              : "Tạo Requirement"}
                    </button>
                </div>
            </form>
        </div>
    );
}
