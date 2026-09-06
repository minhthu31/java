import React, { useState, useEffect } from "react";
import { GitHubActivityService } from "./GitHubActivityService";

export function GitHubActivityComponent({ projectId }) {
    const [activities, setActivities] = useState([]);
    const [activeTab, setActiveTab] = useState("COMMIT");
    const [selectedActor, setSelectedActor] = useState("ALL");
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let isMounted = true;
        const loadActivities = async () => {
            try {
                setLoading(true);
                setError(null);
                const data = await GitHubActivityService.getActivity(projectId);
                if (isMounted) {
                    setActivities(data?.content || []);
                }
            } catch (err) {
                if (isMounted) {
                    setError(
                        "Không thể tải dữ liệu hoạt động GitHub từ hệ thống.",
                    );
                }
            } finally {
                if (isMounted) {
                    setLoading(false);
                }
            }
        };

        if (projectId) {
            loadActivities();
        }
        return () => {
            isMounted = false;
        };
    }, [projectId]);

    const authors = [
        ...new Set(activities.map((act) => act.actorLogin).filter(Boolean)),
    ];

    const filteredActivities = activities.filter((act) => {
        const matchType = act.type === activeTab;
        const matchActor =
            selectedActor === "ALL" || act.actorLogin === selectedActor;
        return matchType && matchActor;
    });

    return (
        <div style={{ padding: "24px" }}>
            {error && (
                <div
                    data-testid="error-banner"
                    style={{
                        display: "flex",
                        alignItems: "center",
                        padding: "12px 16px",
                        marginBottom: "20px",
                        backgroundColor: "#ffebe6",
                        border: "1px solid #ffbdad",
                        borderRadius: "6px",
                        color: "#bf2600",
                        fontSize: "14px",
                        fontWeight: 500,
                    }}
                >
                    <span style={{ marginRight: "8px", fontSize: "16px" }}>
                        ⚠️
                    </span>
                    <span>{error}</span>
                </div>
            )}

            <div
                style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    flexWrap: "wrap",
                    gap: "16px",
                    marginBottom: "20px",
                    paddingBottom: "16px",
                    borderBottom: "1px solid #ebecf0",
                }}
            >
                <div style={{ display: "flex", gap: "8px" }}>
                    <button
                        type="button"
                        onClick={() => setActiveTab("COMMIT")}
                        style={{
                            padding: "8px 16px",
                            borderRadius: "5px",
                            fontSize: "14px",
                            fontWeight: 600,
                            cursor: "pointer",
                            border: "none",
                            backgroundColor:
                                activeTab === "COMMIT" ? "#0052cc" : "#ebecf0",
                            color:
                                activeTab === "COMMIT" ? "#ffffff" : "#42526e",
                        }}
                    >
                        Commits
                    </button>
                    <button
                        type="button"
                        onClick={() => setActiveTab("PULL_REQUEST")}
                        style={{
                            padding: "8px 16px",
                            borderRadius: "5px",
                            fontSize: "14px",
                            fontWeight: 600,
                            cursor: "pointer",
                            border: "none",
                            backgroundColor:
                                activeTab === "PULL_REQUEST"
                                    ? "#0052cc"
                                    : "#ebecf0",
                            color:
                                activeTab === "PULL_REQUEST"
                                    ? "#ffffff"
                                    : "#42526e",
                        }}
                    >
                        Pull Requests
                    </button>
                </div>

                <div
                    style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "8px",
                    }}
                >
                    <label
                        htmlFor="author-filter"
                        style={{
                            fontSize: "13px",
                            fontWeight: 600,
                            color: "#5e6c84",
                        }}
                    >
                        Filter by Author:
                    </label>
                    <select
                        id="author-filter"
                        aria-label="Filter by Author"
                        value={selectedActor}
                        onChange={(e) => setSelectedActor(e.target.value)}
                        style={{
                            padding: "6px 12px",
                            borderRadius: "4px",
                            border: "1px solid #dfe1e6",
                            backgroundColor: "#fafbfc",
                            fontSize: "13px",
                            color: "#172b4d",
                            outline: "none",
                            cursor: "pointer",
                        }}
                    >
                        <option value="ALL">Tất cả</option>
                        {authors.map((actor) => (
                            <option key={actor} value={actor}>
                                {actor}
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            {loading ? (
                <div
                    style={{
                        padding: "40px",
                        textAlign: "center",
                        color: "#6b778c",
                        fontSize: "14px",
                    }}
                >
                    Đang tải dữ liệu hoạt động...
                </div>
            ) : filteredActivities.length === 0 ? (
                <div
                    data-testid={
                        activeTab === "COMMIT" ? "empty-commits" : "empty-prs"
                    }
                    style={{
                        padding: "40px 20px",
                        textAlign: "center",
                        backgroundColor: "#fafbfc",
                        border: "1px dashed #dfe1e6",
                        borderRadius: "6px",
                        color: "#6b778c",
                        fontSize: "14px",
                    }}
                >
                    {activeTab === "COMMIT"
                        ? "Không có Commit nào phù hợp với bộ lọc."
                        : "Không có Pull Request nào phù hợp với bộ lọc."}
                </div>
            ) : (
                <div
                    style={{
                        display: "flex",
                        flexDirection: "column",
                        gap: "10px",
                    }}
                >
                    {filteredActivities.map((act) => {
                        const shortId =
                            act.type === "COMMIT" && act.externalId
                                ? act.externalId.substring(0, 7)
                                : "";

                        const jiraTasks = Array.isArray(act.issueKeys)
                            ? act.issueKeys.join(", ")
                            : act.issueKeys || "";

                        return (
                            <div
                                key={act.externalId}
                                style={{
                                    padding: "14px 18px",
                                    borderRadius: "6px",
                                    border: "1px solid #ebecf0",
                                    backgroundColor: "#ffffff",
                                    display: "flex",
                                    justifyContent: "space-between",
                                    alignItems: "center",
                                    boxShadow:
                                        "0 1px 2px rgba(9, 30, 66, 0.04)",
                                }}
                            >
                                <div
                                    style={{
                                        display: "flex",
                                        flexDirection: "column",
                                        gap: "6px",
                                    }}
                                >
                                    <div
                                        style={{
                                            display: "flex",
                                            alignItems: "center",
                                            gap: "10px",
                                            flexWrap: "wrap",
                                        }}
                                    >
                                        {shortId && (
                                            <a
                                                href={act.htmlUrl}
                                                target="_blank"
                                                rel="noopener noreferrer"
                                                style={{
                                                    fontFamily: "monospace",
                                                    fontSize: "12px",
                                                    fontWeight: 700,
                                                    color: "#0052cc",
                                                    backgroundColor: "#deebff",
                                                    padding: "2px 8px",
                                                    borderRadius: "4px",
                                                    textDecoration: "none",
                                                }}
                                            >
                                                {shortId}
                                            </a>
                                        )}

                                        <a
                                            href={act.htmlUrl}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            style={{
                                                fontSize: "14px",
                                                fontWeight: 600,
                                                color: "#172b4d",
                                                textDecoration: "none",
                                            }}
                                        >
                                            {act.title}
                                        </a>

                                        {jiraTasks && (
                                            <span
                                                style={{
                                                    fontSize: "11px",
                                                    fontWeight: 700,
                                                    padding: "2px 6px",
                                                    backgroundColor: "#dfe1e6",
                                                    color: "#172b4d",
                                                    borderRadius: "3px",
                                                }}
                                            >
                                                {jiraTasks}
                                            </span>
                                        )}
                                    </div>

                                    <div
                                        style={{
                                            fontSize: "12px",
                                            color: "#6b778c",
                                        }}
                                    >
                                        <span>
                                            Tác giả:{" "}
                                            <strong>{act.actorLogin}</strong>
                                        </span>
                                        {act.occurredAt && (
                                            <span style={{ marginLeft: "8px" }}>
                                                ·{" "}
                                                {new Date(
                                                    act.occurredAt,
                                                ).toLocaleString()}
                                            </span>
                                        )}
                                    </div>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}

export default GitHubActivityComponent;
