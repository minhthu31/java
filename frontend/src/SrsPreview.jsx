import React, { useCallback, useEffect, useState } from "react";
import { RequirementService } from "./RequirementService";

const valueOrDash = (value) =>
    value === undefined || value === null || value === "" ? "—" : value;

export default function SrsPreview({ projectId, currentUserRole }) {
    const canView =
        currentUserRole === "TEAM_LEADER" || currentUserRole === "LECTURER";
    const [requirements, setRequirements] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const loadPreview = useCallback(async () => {
        if (!canView || !projectId) return;
        setLoading(true);
        setError("");
        try {
            const data = await RequirementService.getRequirements(projectId, {
                page: 0,
                size: 100,
                sort: "updatedAt,asc",
            });
            setRequirements(data.content || []);
        } catch (requestError) {
            setRequirements([]);
            setError(
                requestError?.message ||
                    "Không thể tải dữ liệu để xem trước SRS.",
            );
        } finally {
            setLoading(false);
        }
    }, [canView, projectId]);

    useEffect(() => {
        loadPreview();
    }, [loadPreview]);

    if (!canView) {
        return (
            <div role="alert" style={{ padding: "32px", color: "#bf2600" }}>
                Bạn không có quyền xem SRS của dự án này.
            </div>
        );
    }

    return (
        <article style={{ padding: "28px", color: "#172b4d" }}>
            <header
                style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "flex-start",
                    gap: "16px",
                    borderBottom: "2px solid #172b4d",
                    paddingBottom: "16px",
                    marginBottom: "20px",
                }}
            >
                <div>
                    <p style={{ margin: 0, color: "#6b778c" }}>
                        CNPM Project Management Tool
                    </p>
                    <h2 style={{ margin: "4px 0" }}>SRS Preview</h2>
                    <span style={{ fontSize: "13px", color: "#6b778c" }}>
                        Bản xem trước được tổng hợp trực tiếp từ Requirement của
                        Project #{projectId}
                    </span>
                </div>
                <button type="button" onClick={() => window.print()}>
                    In / Lưu PDF
                </button>
            </header>

            {loading && <p>Đang tạo bản xem trước SRS...</p>}
            {error && (
                <div role="alert">
                    <p>{error}</p>
                    <button type="button" onClick={loadPreview}>
                        Thử lại
                    </button>
                </div>
            )}
            {!loading && !error && requirements.length === 0 && (
                <p>Chưa có Requirement để tạo bản xem trước SRS.</p>
            )}
            {!loading && !error && requirements.length > 0 && (
                <div>
                    <h3>Danh sách yêu cầu ({requirements.length})</h3>
                    {requirements.map((requirement, index) => (
                        <section
                            key={requirement.id || index}
                            style={{
                                border: "1px solid #dfe1e6",
                                borderRadius: "6px",
                                padding: "18px",
                                marginBottom: "14px",
                            }}
                        >
                            <h4 style={{ marginTop: 0 }}>
                                {index + 1}. {requirement.title}
                            </h4>
                            <p>
                                <strong>Mã Jira:</strong>{" "}
                                {valueOrDash(requirement.jiraIssueKey)} ·{" "}
                                <strong>Trạng thái:</strong>{" "}
                                {valueOrDash(requirement.status)} ·{" "}
                                <strong>Ưu tiên:</strong>{" "}
                                {valueOrDash(requirement.priority)}
                            </p>
                            <p>
                                <strong>Actor:</strong>{" "}
                                {valueOrDash(requirement.actor)}
                            </p>
                            <p>
                                <strong>Mô tả:</strong>{" "}
                                {valueOrDash(requirement.description)}
                            </p>
                            <p>
                                <strong>Tiền điều kiện:</strong>{" "}
                                {valueOrDash(requirement.precondition)}
                            </p>
                            <p>
                                <strong>Luồng chính:</strong>{" "}
                                {valueOrDash(requirement.mainFlow)}
                            </p>
                            <p>
                                <strong>Luồng thay thế:</strong>{" "}
                                {valueOrDash(requirement.alternativeFlow)}
                            </p>
                            <p>
                                <strong>Luồng ngoại lệ:</strong>{" "}
                                {valueOrDash(requirement.exceptionFlow)}
                            </p>
                            <p>
                                <strong>Hậu điều kiện:</strong>{" "}
                                {valueOrDash(requirement.postcondition)}
                            </p>
                        </section>
                    ))}
                </div>
            )}
        </article>
    );
}
