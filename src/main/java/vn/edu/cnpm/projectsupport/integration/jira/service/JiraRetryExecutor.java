package vn.edu.cnpm.projectsupport.integration.jira.service;

import java.util.concurrent.Callable;

import org.springframework.http.HttpStatus;

import vn.edu.cnpm.projectsupport.integration.jira.JiraConnectionException;
import vn.edu.cnpm.projectsupport.integration.jira.exception.JiraApiException;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLog;

/** Executes Jira operations with bounded exponential backoff. */
public class JiraRetryExecutor {

    static final int MAX_ATTEMPTS = 3;
    static final long INITIAL_BACKOFF_MILLIS = 200L;
    static final long MAX_BACKOFF_MILLIS = 5_000L;

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private final Sleeper sleeper;

    public JiraRetryExecutor() {
        this(Thread::sleep);
    }

    JiraRetryExecutor(Sleeper sleeper) {
        this.sleeper = sleeper;
    }

    public <T> T execute(Callable<T> operation, SyncLog log) {
        return executeInternal(operation, log);
    }

    public void executeVoid(ThrowingRunnable operation, SyncLog log) {
        executeInternal(() -> {
            operation.run();
            return null;
        }, log);
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run();
    }

    private <T> T executeInternal(Callable<T> operation, SyncLog log) {
        RuntimeException last = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return operation.call();
            } catch (Exception ex) {
                RuntimeException runtime = toRuntime(ex);
                last = runtime;

                if (!shouldRetry(runtime) || attempt == MAX_ATTEMPTS) {
                    throw runtime;
                }

                if (log != null) {
                    log.setRetryCount(attempt);
                }

                long delay = backoffMillis(attempt, runtime);
                try {
                    sleeper.sleep(delay);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new JiraConnectionException("Jira retry bị gián đoạn", interrupted);
                }
            }
        }

        throw last == null
                ? new JiraConnectionException("Jira operation failed")
                : last;
    }

    boolean shouldRetry(RuntimeException exception) {
        if (exception instanceof JiraApiException jira) {
            return jira.isRetryable();
        }
        return false;
    }

    long backoffMillis(int retryNumber, RuntimeException exception) {
        if (exception instanceof JiraApiException jira
                && jira.getStatus() == HttpStatus.TOO_MANY_REQUESTS
                && jira.getRetryAfterSeconds() != null) {
            return Math.min(MAX_BACKOFF_MILLIS,
                    Math.max(0L, jira.getRetryAfterSeconds() * 1000L));
        }

        long delay = INITIAL_BACKOFF_MILLIS * (1L << Math.max(0, retryNumber - 1));
        return Math.min(MAX_BACKOFF_MILLIS, delay);
    }

    private RuntimeException toRuntime(Exception ex) {
        if (ex instanceof RuntimeException runtime) {
            return runtime;
        }
        return new JiraConnectionException("Không thể kết nối Jira", ex);
    }
}
