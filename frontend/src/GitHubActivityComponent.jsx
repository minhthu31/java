import React, { useState, useEffect, useCallback } from "react";
import { GitHubActivityService } from "./GitHubActivityService";

export function GitHubActivityComponent({ projectId }) {
    const [activities, setActivities] = useState([]);
    const [activeTab, setActiveTab] = useState("COMMIT");

    const [issueKeyInput, setIssueKeyInput] = useState("");
    const [actorUserIdInput, setActorUserIdInput] = useState("");
    const [fromInput, setFromInput] = useState("");
    const [toInput, setToInput] = useState("");

    const [appliedFilters, setAppliedFilters] = useState({
        issueKey: "",
        actorUserId: "",
        from: "",
        to: "",
    });

    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [isFirst, setIsFirst] = useState(true);
    const [isLast, setIsLast] = useState(true);
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(true);

    const fetchActivities = useCallback(async () => {
        if (!projectId) return;
        try {
            setLoading(true);
            setError(null);

            const params = {
                type: activeTab,
                page,
                size: 10,
            };

            if (appliedFilters.issueKey.trim()) {
                params.issueKey = appliedFilters.issueKey.trim();
            }
            if (appliedFilters.actorUserId.trim()) {
                params.actorUserId = Number(appliedFilters.actorUserId);
            }
            if (appliedFilters.from) {
                params.from = `${appliedFilters.from}T00:00:00.000Z`;
            }
            if (appliedFilters.to) {
                params.to = `${appliedFilters.to}T23:59:59.999Z`;
            }

            const data = await GitHubActivityService.getActivity(
                projectId,
                params,
            );
            setActivities(data?.content || []);
            setTotalPages(data?.totalPages || 0);
            setIsFirst(data?.first !== undefined ? data.first : page === 0);
            setIsLast(
                data?.last !== undefined
                    ? data.last
                    : page >= (data?.totalPages || 1) - 1,
            );
        } catch (err) {
            setError("Không thể tải dữ liệu hoạt động GitHub từ hệ thống.");
        } finally {
            setLoading(false);
        }
    }, [projectId, activeTab, page, appliedFilters]);

    useEffect(() => {
        fetchActivities();
    }, [fetchActivities]);

    const handleTabChange = (type) => {
        setActiveTab(type);
        setPage(0);
    };

    const handleSearch = (e) => {
        e.preventDefault();
        setAppliedFilters({
            issueKey: issueKeyInput,
            actorUserId: actorUserIdInput,
            from: fromInput,
            to: toInput,
        });
        setPage(0);
    };

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

            <div style={{ display: "flex", gap: "8px", marginBottom: "16px" }}>
                <button
                    type="button"
                    onClick={() => handleTabChange("COMMIT")}
                    style={{
                        padding: "8px 16px",
                        borderRadius: "5px",
                        fontSize: "14px",
                        fontWeight: 600,
                        cursor: "pointer",
                        border: "none",
                        backgroundColor:
                            activeTab === "COMMIT" ? "#0052cc" : "#ebecf0",
                        color: activeTab === "COMMIT" ? "#ffffff" : "#42526e",
                    }}
                >
                    Commits
                </button>
                <button
                    type="button"
                    onClick={() => handleTabChange("PULL_REQUEST")}
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

            <form
                onSubmit={handleSearch}
                style={{
                    display: "flex",
                    gap: "12px",
                    alignItems: "center",
                    flexWrap: "wrap",
                    marginBottom: "20px",
                    padding: "12px",
                    backgroundColor: "#f4f5f7",
                    borderRadius: "6px",
                }}
            >
                <div>
                    <label
                        htmlFor="filter-issue-key"
                        style={{
                            fontSize: "13px",
                            fontWeight: 600,
                            color: "#5e6c84",
                            marginRight: "6px",
                        }}
                    >
                        Mã Jira:
                    </label>
                    <input
                        id="filter-issue-key"
                        type="text"
                        placeholder="VD: CNPM-98"
                        value={issueKeyInput}
                        onChange={(e) => setIssueKeyInput(e.target.value)}
                        style={{
                            padding: "6px 10px",
                            borderRadius: "4px",
                            border: "1px solid #dfe1e6",
                            fontSize: "13px",
                        }}
                    />
                </div>

                <div>
                    <label
                        htmlFor="filter-actor-id"
                        style={{
                            fontSize: "13px",
                            fontWeight: 600,
                            color: "#5e6c84",
                            marginRight: "6px",
                        }}
                    >
                        User ID:
                    </label>
                    <input
                        id="filter-actor-id"
                        type="number"
                        placeholder="VD: 1"
                        value={actorUserIdInput}
                        onChange={(e) => setActorUserIdInput(e.target.value)}
                        style={{
                            width: "80px",
                            padding: "6px 10px",
                            borderRadius: "4px",
                            border: "1px solid #dfe1e6",
                            fontSize: "13px",
                        }}
                    />
                </div>

                <div>
                    <label
                        htmlFor="filter-from"
                        style={{
                            fontSize: "13px",
                            fontWeight: 600,
                            color: "#5e6c84",
                            marginRight: "6px",
                        }}
                    >
                        Từ:
                    </label>
                    <input
                        id="filter-from"
                        type="date"
                        value={fromInput}
                        onChange={(e) => setFromInput(e.target.value)}
                        style={{
                            padding: "5px 8px",
                            borderRadius: "4px",
                            border: "1px solid #dfe1e6",
                            fontSize: "13px",
                        }}
                    />
                </div>

                <div>
                    <label
                        htmlFor="filter-to"
                        style={{
                            fontSize: "13px",
                            fontWeight: 600,
                            color: "#5e6c84",
                            marginRight: "6px",
                        }}
                    >
                        Đến:
                    </label>
                    <input
                        id="filter-to"
                        type="date"
                        value={toInput}
                        onChange={(e) => setToInput(e.target.value)}
                        style={{
                            padding: "5px 8px",
                            borderRadius: "4px",
                            border: "1px solid #dfe1e6",
                            fontSize: "13px",
                        }}
                    />
                </div>

                <button
                    type="submit"
                    style={{
                        padding: "6px 16px",
                        backgroundColor: "#0052cc",
                        color: "#fff",
                        border: "none",
                        borderRadius: "4px",
                        cursor: "pointer",
                        fontWeight: 600,
                        fontSize: "13px",
                    }}
                >
                    Lọc
                </button>
            </form>

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
            ) : activities.length === 0 ? (
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
                    {activities.map((act) => {
                        const shortKey =
                            act.type === "COMMIT" && act.key
                                ? act.key.substring(0, 7)
                                : act.key;
                        const jiraTasks = Array.isArray(act.issueKeys)
                            ? act.issueKeys.join(", ")
                            : act.issueKeys || "";

                        return (
                            <div
                                key={act.key || act.url}
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
                                        {shortKey && (
                                            <a
                                                href={act.url}
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
                                                {act.type === "PULL_REQUEST"
                                                    ? `#${shortKey}`
                                                    : shortKey}
                                            </a>
                                        )}

                                        <a
                                            href={act.url}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            style={{
                                                fontSize: "14px",
                                                fontWeight: 600,
                                                color: "#172b4d",
                                                textDecoration: "none",
                                            }}
                                        >
                                            {act.summary}
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
                                        {act.timestamp && (
                                            <span style={{ marginLeft: "8px" }}>
                                                ·{" "}
                                                {new Date(
                                                    act.timestamp,
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

            {totalPages > 1 && (
                <div
                    style={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        marginTop: "20px",
                        paddingTop: "16px",
                        borderTop: "1px solid #ebecf0",
                    }}
                >
                    <button
                        type="button"
                        disabled={isFirst}
                        onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                        style={{
                            padding: "6px 14px",
                            borderRadius: "4px",
                            border: "1px solid #dfe1e6",
                            backgroundColor: isFirst ? "#f4f5f7" : "#fff",
                            color: isFirst ? "#a5b2c6" : "#172b4d",
                            cursor: isFirst ? "not-allowed" : "pointer",
                        }}
                    >
                        Trang trước
                    </button>
                    <span style={{ fontSize: "13px", color: "#6b778c" }}>
                        Trang {page + 1} / {totalPages}
                    </span>
                    <button
                        type="button"
                        disabled={isLast}
                        onClick={() => setPage((prev) => prev + 1)}
                        style={{
                            padding: "6px 14px",
                            borderRadius: "4px",
                            border: "1px solid #dfe1e6",
                            backgroundColor: isLast ? "#f4f5f7" : "#fff",
                            color: isLast ? "#a5b2c6" : "#172b4d",
                            cursor: isLast ? "not-allowed" : "pointer",
                        }}
                    >
                        Trang sau
                    </button>
                </div>
            )}
        </div>
    );
}

export default GitHubActivityComponent;
