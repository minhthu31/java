import React, { useEffect, useState } from "react";
import { GitHubActivityService } from "./GitHubActivityService";

export function GitHubTaskActivityPanel({ projectId, taskId }) {
    const [activities, setActivities] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        let active = true;
        setLoading(true);
        setError(null);
        GitHubActivityService.getTaskActivities(projectId, taskId, {
            page: 0,
            size: 20,
        })
            .then((result) => {
                if (active) {
                    setActivities(Array.isArray(result?.content) ? result.content : []);
                }
            })
            .catch((requestError) => {
                if (active) {
                    setActivities([]);
                    setError(
                        requestError.response?.data?.message ||
                            "Không thể tải hoạt động GitHub của Task.",
                    );
                }
            })
            .finally(() => {
                if (active) setLoading(false);
            });
        return () => {
            active = false;
        };
    }, [projectId, taskId]);

    return (
        <section
            aria-label="Hoạt động GitHub liên quan"
            style={{ marginBottom: "20px" }}
        >
            <div
                style={{
                    fontWeight: 600,
                    fontSize: "13px",
                    marginBottom: "6px",
                }}
            >
                Hoạt động GitHub liên quan:
            </div>
            <div
                style={{
                    background: "#f8fafc",
                    border: "1px solid #e2e8f0",
                    padding: "10px",
                    borderRadius: "6px",
                    fontSize: "12px",
                }}
            >
                {loading && <span>Đang tải hoạt động GitHub...</span>}
                {!loading && error && (
                    <span role="alert" style={{ color: "#991b1b" }}>
                        {error}
                    </span>
                )}
                {!loading && !error && activities.length === 0 && (
                    <span>Chưa có commit hoặc Pull Request liên kết.</span>
                )}
                {!loading && !error && activities.length > 0 && (
                    <ul style={{ margin: 0, paddingLeft: "18px" }}>
                        {activities.map((activity) => (
                            <li key={`${activity.type}-${activity.key}`} style={{ marginBottom: "6px" }}>
                                <strong>{activity.type === "PULL_REQUEST" ? "PR" : "Commit"}</strong>{" "}
                                {activity.url ? (
                                    <a
                                        href={activity.url}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        style={{ color: "#0052cc" }}
                                    >
                                        {activity.summary}
                                    </a>
                                ) : (
                                    activity.summary
                                )}
                                {activity.actorLogin ? ` — ${activity.actorLogin}` : ""}
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        </section>
    );
}

export default GitHubTaskActivityPanel;
