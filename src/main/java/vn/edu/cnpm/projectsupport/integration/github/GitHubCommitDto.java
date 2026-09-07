package vn.edu.cnpm.projectsupport.integration.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class GitHubCommitDto {
    private String sha;
    private CommitDetail commit;
    private Author author;
    @JsonProperty("html_url")
    private String htmlUrl;

    public static class CommitDetail {
        private String message;
        private Author author;
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Author getAuthor() { return author; }
        public void setAuthor(Author author) { this.author = author; }
    }

    public static class Author {
        private String name;
        private String email;
        private Instant date;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Instant getDate() { return date; }
        public void setDate(Instant date) { this.date = date; }
    }

    public String getSha() { return sha; }
    public void setSha(String sha) { this.sha = sha; }
    public CommitDetail getCommit() { return commit; }
    public void setCommit(CommitDetail commit) { this.commit = commit; }
    public Author getAuthor() { return author; }
    public void setAuthor(Author author) { this.author = author; }
    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
}