package jigit.indexer;

import jigit.indexer.api.CommitAdapter;
import jigit.indexer.api.CommitFileAdapter;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;

public abstract class PersistStrategy {
    @NotNull
    private static final Logger log = Logger.getLogger(PersistStrategy.class);
    @NotNull
    public static final PersistStrategy DO_NOTHING = new PersistStrategy() {
        @NotNull
        @Override
        protected Collection<String> persistThrowing(@Nullable String repoGroupName,
                                                     @NotNull String repoName,
                                                     @NotNull String branch,
                                                     @NotNull CommitAdapter commitAdapter,
                                                     @NotNull Collection<String> issueKeys,
                                                     @NotNull Collection<CommitFileAdapter> commitFileAdapters) {
            return Collections.emptyList();
        }
    };

    public final @NotNull
    Collection<String> persist(@Nullable String repoGroupName,
                               @NotNull String repoName,
                               @NotNull String branch,
                               @NotNull CommitAdapter commitAdapter,
                               @NotNull Collection<String> issueKeys,
                               @NotNull Collection<CommitFileAdapter> commitFileAdapters) throws ParseException {
        try {
            return persistThrowing(repoGroupName, repoName, branch, commitAdapter, issueKeys, commitFileAdapters);
        } catch (Throwable t) {
            log.error("Couldn't persist commit data. Commit sha1=" + commitAdapter.getCommitSha1() + ", repository group=" + repoGroupName
                    + ", repository=" + repoName + ", branch=" + branch, t);
            throw t;
        }
    }

    @NotNull
    protected abstract Collection<String> persistThrowing(@Nullable String repoGroupName,
                                                          @NotNull String repoName,
                                                          @NotNull String branch,
                                                          @NotNull CommitAdapter commitAdapter,
                                                          @NotNull Collection<String> issueKeys,
                                                          @NotNull Collection<CommitFileAdapter> commitFileAdapters) throws ParseException;
}
