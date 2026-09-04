import React, { useState, useEffect, useMemo } from "react";
import { GitHubActivityService } from "./GitHubActivityService";

export const GitHubActivityComponent = ({ projectId }) => {
    const [commits, setCommits] = useState([]);
    const [pullRequests, setPullRequests] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const [selectedAuthor, setSelectedAuthor] = useState("ALL");
    const [selectedPrStatus, setSelectedPrStatus] = useState("ALL");
    const [activeTab, setActiveTab] = useState("commits");

    const fetchData = async () => {
        if (!projectId) {
            setLoading(false);
            return;
        }
        setLoading(true);
        setError(null);
        try {
            const data = await GitHubActivityService.getActivity(projectId);
            setCommits(data?.commits || []);
            setPullRequests(data?.pullRequests || []);
        } catch (err) {
            setError(
                "Không thể tải dữ liệu hoạt động GitHub. Vui lòng thử lại sau.",
            );
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, [projectId]);

    const authors = useMemo(() => {
        const authorSet = new Set();
        commits.forEach((c) => c.authorName && authorSet.add(c.authorName));
        pullRequests.forEach(
            (pr) => pr.authorName && authorSet.add(pr.authorName),
        );
        return Array.from(authorSet);
    }, [commits, pullRequests]);

    const filteredCommits = useMemo(() => {
        return commits.filter((commit) => {
            if (
                selectedAuthor !== "ALL" &&
                commit.authorName !== selectedAuthor
            ) {
                return false;
            }
            return true;
        });
    }, [commits, selectedAuthor]);

    const filteredPullRequests = useMemo(() => {
        return pullRequests.filter((pr) => {
            if (selectedAuthor !== "ALL" && pr.authorName !== selectedAuthor) {
                return false;
            }
            if (
                selectedPrStatus !== "ALL" &&
                pr.status?.toUpperCase() !== selectedPrStatus
            ) {
                return false;
            }
            return true;
        });
    }, [pullRequests, selectedAuthor, selectedPrStatus]);

    const getPrBadgeStyle = (status) => {
        switch (status?.toUpperCase()) {
            case "OPEN":
                return {
                    backgroundColor: "#e3fcef",
                    color: "#006644",
                    border: "1px solid #abf5d1",
                };
            case "MERGED":
                return {
                    backgroundColor: "#eae6ff",
                    color: "#403294",
                    border: "1px solid #c0b6f2",
                };
            case "CLOSED":
                return {
                    backgroundColor: "#ffebe6",
                    color: "#de350b",
                    border: "1px solid #ffbdad",
                };
            default:
                return {
                    backgroundColor: "#ebecf0",
                    color: "#42526e",
                    border: "1px solid #dfe1e6",
                };
        }
    };

    if (!projectId) {
        return (
            <div
                data-testid="no-project"
                style={{
                    padding: "48px 32px",
                    textAlign: "center",
                    color: "#6b778c",
                    fontSize: "14px",
                }}
            >
                Vui lòng chọn dự án để xem dữ liệu GitHub.
            </div>
        );
    }

    return (
        <div
            style={{
                width: "100%",
                padding: "20px 28px 32px 28px",
                boxSizing: "border-box",
                fontFamily: "inherit",
            }}
        >
            <div
                style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "flex-start",
                    flexWrap: "wrap",
                    gap: "16px",
                    marginBottom: "20px",
                }}
            >
                <div>
                    <h2
                        style={{
                            fontSize: "20px",
                            fontWeight: "700",
                            color: "#172b4d",
                            margin: "0 0 6px 0",
                        }}
                    >
                        Hoạt động GitHub đã đồng bộ
                    </h2>
                    <p
                        style={{
                            fontSize: "14px",
                            color: "#6b778c",
                            margin: 0,
                        }}
                    >
                        Theo dõi lịch sử commits, pull requests và các task liên
                        quan
                    </p>
                </div>

                <div
                    style={{
                        display: "flex",
                        gap: "12px",
                        alignItems: "center",
                    }}
                >
                    <select
                        aria-label="Filter by Author"
                        value={selectedAuthor}
                        onChange={(e) => setSelectedAuthor(e.target.value)}
                        style={{
                            height: "42px",
                            padding: "0 14px",
                            fontSize: "14px",
                            fontWeight: "500",
                            borderRadius: "6px",
                            border: "1px solid #dfe1e6",
                            backgroundColor: "#ffffff",
                            color: "#172b4d",
                            outline: "none",
                            cursor: "pointer",
                            minWidth: "180px",
                        }}
                    >
                        <option value="ALL">Tất cả thành viên</option>
                        {authors.map((author) => (
                            <option key={author} value={author}>
                                {author}
                            </option>
                        ))}
                    </select>

                    {activeTab === "pull_requests" && (
                        <select
                            aria-label="Filter by PR Status"
                            value={selectedPrStatus}
                            onChange={(e) =>
                                setSelectedPrStatus(e.target.value)
                            }
                            style={{
                                height: "42px",
                                padding: "0 14px",
                                fontSize: "14px",
                                fontWeight: "500",
                                borderRadius: "6px",
                                border: "1px solid #dfe1e6",
                                backgroundColor: "#ffffff",
                                color: "#172b4d",
                                outline: "none",
                                cursor: "pointer",
                                minWidth: "180px",
                            }}
                        >
                            <option value="ALL">Tất cả trạng thái PR</option>
                            <option value="OPEN">Open</option>
                            <option value="MERGED">Merged</option>
                            <option value="CLOSED">Closed</option>
                        </select>
                    )}
                </div>
            </div>

            {error && (
                <div
                    data-testid="error-banner"
                    style={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                        padding: "12px 18px",
                        backgroundColor: "#ffebe6",
                        border: "1px solid #ffbdad",
                        borderRadius: "6px",
                        marginBottom: "20px",
                    }}
                >
                    <span
                        style={{
                            fontSize: "14px",
                            color: "#de350b",
                            fontWeight: "500",
                        }}
                    >
                        {error}
                    </span>
                    <button
                        type="button"
                        onClick={fetchData}
                        style={{
                            padding: "6px 14px",
                            backgroundColor: "#de350b",
                            color: "#ffffff",
                            border: "none",
                            borderRadius: "4px",
                            fontSize: "13px",
                            fontWeight: "600",
                            cursor: "pointer",
                        }}
                    >
                        Thử lại
                    </button>
                </div>
            )}

            <div
                style={{
                    display: "flex",
                    gap: "10px",
                    marginBottom: "20px",
                    borderBottom: "1px solid #ebecf0",
                    paddingBottom: "12px",
                }}
            >
                <button
                    type="button"
                    onClick={() => setActiveTab("commits")}
                    style={{
                        height: "38px",
                        padding: "0 18px",
                        fontSize: "14px",
                        fontWeight: "600",
                        borderRadius: "5px",
                        border: "none",
                        cursor: "pointer",
                        backgroundColor:
                            activeTab === "commits" ? "#0052cc" : "#f4f5f7",
                        color: activeTab === "commits" ? "#ffffff" : "#42526e",
                        transition: "all 0.15s ease",
                    }}
                >
                    Commits ({filteredCommits.length})
                </button>
                <button
                    type="button"
                    onClick={() => setActiveTab("pull_requests")}
                    style={{
                        height: "38px",
                        padding: "0 18px",
                        fontSize: "14px",
                        fontWeight: "600",
                        borderRadius: "5px",
                        border: "none",
                        cursor: "pointer",
                        backgroundColor:
                            activeTab === "pull_requests"
                                ? "#0052cc"
                                : "#f4f5f7",
                        color:
                            activeTab === "pull_requests"
                                ? "#ffffff"
                                : "#42526e",
                        transition: "all 0.15s ease",
                    }}
                >
                    Pull Requests ({filteredPullRequests.length})
                </button>
            </div>

            {/* Loading */}
            {loading && (
                <div
                    data-testid="loading-spinner"
                    style={{
                        padding: "48px 0",
                        textAlign: "center",
                        color: "#6b778c",
                        fontSize: "14px",
                    }}
                >
                    Đang tải dữ liệu GitHub...
                </div>
            )}

            {!loading && !error && (
                <div>
                    {activeTab === "commits" &&
                        (filteredCommits.length === 0 ? (
                            <div
                                data-testid="empty-commits"
                                style={{
                                    padding: "48px 0",
                                    textAlign: "center",
                                    color: "#8993a4",
                                    fontSize: "14px",
                                }}
                            >
                                Không có commit nào phù hợp với bộ lọc.
                            </div>
                        ) : (
                            <div
                                style={{
                                    border: "1px solid #dfe1e6",
                                    borderRadius: "6px",
                                    backgroundColor: "#ffffff",
                                    overflow: "hidden",
                                }}
                            >
                                {filteredCommits.map((commit, index) => (
                                    <div
                                        key={commit.sha}
                                        style={{
                                            padding: "14px 18px",
                                            borderBottom:
                                                index <
                                                filteredCommits.length - 1
                                                    ? "1px solid #ebecf0"
                                                    : "none",
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
                                            <a
                                                href={commit.htmlUrl}
                                                target="_blank"
                                                rel="noopener noreferrer"
                                                style={{
                                                    fontFamily: "monospace",
                                                    fontSize: "12px",
                                                    fontWeight: "600",
                                                    color: "#0052cc",
                                                    textDecoration: "none",
                                                    backgroundColor: "#deebff",
                                                    padding: "3px 8px",
                                                    borderRadius: "4px",
                                                }}
                                            >
                                                {commit.sha
                                                    ? commit.sha.substring(0, 7)
                                                    : "N/A"}
                                            </a>
                                            <span
                                                style={{
                                                    fontSize: "14px",
                                                    fontWeight: "600",
                                                    color: "#172b4d",
                                                }}
                                            >
                                                {commit.message}
                                            </span>
                                            {commit.relatedTaskKey && (
                                                <span
                                                    style={{
                                                        fontSize: "11px",
                                                        fontWeight: "700",
                                                        backgroundColor:
                                                            "#f4f5f7",
                                                        color: "#42526e",
                                                        padding: "3px 8px",
                                                        borderRadius: "4px",
                                                        border: "1px solid #dfe1e6",
                                                    }}
                                                >
                                                    {commit.relatedTaskKey}
                                                </span>
                                            )}
                                        </div>
                                        <div
                                            style={{
                                                fontSize: "12px",
                                                color: "#6b778c",
                                            }}
                                        >
                                            <span>{commit.authorName}</span>
                                            <span style={{ margin: "0 8px" }}>
                                                •
                                            </span>
                                            <span>
                                                {new Date(
                                                    commit.committedAt,
                                                ).toLocaleString("vi-VN")}
                                            </span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ))}

                    {activeTab === "pull_requests" &&
                        (filteredPullRequests.length === 0 ? (
                            <div
                                data-testid="empty-prs"
                                style={{
                                    padding: "48px 0",
                                    textAlign: "center",
                                    color: "#8993a4",
                                    fontSize: "14px",
                                }}
                            >
                                Không có Pull Request nào phù hợp với bộ lọc.
                            </div>
                        ) : (
                            <div
                                style={{
                                    border: "1px solid #dfe1e6",
                                    borderRadius: "6px",
                                    backgroundColor: "#ffffff",
                                    overflow: "hidden",
                                }}
                            >
                                {filteredPullRequests.map((pr, index) => (
                                    <div
                                        key={pr.id || pr.number}
                                        style={{
                                            padding: "14px 18px",
                                            borderBottom:
                                                index <
                                                filteredPullRequests.length - 1
                                                    ? "1px solid #ebecf0"
                                                    : "none",
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
                                            <span
                                                style={{
                                                    fontSize: "11px",
                                                    fontWeight: "700",
                                                    padding: "3px 8px",
                                                    borderRadius: "4px",
                                                    ...getPrBadgeStyle(
                                                        pr.status,
                                                    ),
                                                }}
                                            >
                                                {pr.status}
                                            </span>
                                            <a
                                                href={pr.htmlUrl}
                                                target="_blank"
                                                rel="noopener noreferrer"
                                                style={{
                                                    fontSize: "14px",
                                                    fontWeight: "600",
                                                    color: "#0052cc",
                                                    textDecoration: "none",
                                                }}
                                            >
                                                #{pr.number} {pr.title}
                                            </a>
                                            {pr.relatedTaskKey && (
                                                <span
                                                    style={{
                                                        fontSize: "11px",
                                                        fontWeight: "700",
                                                        backgroundColor:
                                                            "#f4f5f7",
                                                        color: "#42526e",
                                                        padding: "3px 8px",
                                                        borderRadius: "4px",
                                                        border: "1px solid #dfe1e6",
                                                    }}
                                                >
                                                    {pr.relatedTaskKey}
                                                </span>
                                            )}
                                        </div>
                                        <div
                                            style={{
                                                fontSize: "12px",
                                                color: "#6b778c",
                                            }}
                                        >
                                            <span>Tạo bởi {pr.authorName}</span>
                                            <span style={{ margin: "0 8px" }}>
                                                •
                                            </span>
                                            <span>
                                                {new Date(
                                                    pr.createdAt,
                                                ).toLocaleString("vi-VN")}
                                            </span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ))}
                </div>
            )}
        </div>
    );
};

export default GitHubActivityComponent;
