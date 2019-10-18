package jigit.indexer;

import jigit.indexer.api.CommitAdapter;
import jigit.indexer.api.CommitFileAdapter;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.Collection;

public abstract class PersistStrategy {
    @NotNull
    private static final Logger log = Logger.getLogger(PersistStrategy.class);
    @NotNull
    public static final PersistStrategy DO_NOTHING = new PersistStrategy() {
        @Override
        public void persistThrowing(@Nullable String repoGroupName,
                                    @NotNull String repoName,
                                    @NotNull String branch,
                                    @NotNull CommitAdapter commitAdapter,
                                    @NotNull Collection<String> issueKeys,
                                    @NotNull Collection<CommitFileAdapter> commitFileAdapters) {
            //do nothing
        }
    };

    public final void persist(@Nullable String repoGroupName,
                              @NotNull String repoName,
                              @NotNull String branch,
                              @NotNull CommitAdapter commitAdapter,
                              @NotNull Collection<String> issueKeys,
                              @NotNull Collection<CommitFileAdapter> commitFileAdapters) throws ParseException{
        try {
            persistThrowing(repoGroupName, repoName, branch, commitAdapter, issueKeys, commitFileAdapters);
        } catch (Throwable t) {
            log.error("Couldn't persist commit data. Commit sha1=" + commitAdapter.getCommitSha1() + ", repository group=" + repoGroupName
                    + ", repository=" + repoName + ", branch=" + branch, t);
            throw t;
        }
    }

    public abstract void persistThrowing(@Nullable String repoGroupName,
                                         @NotNull String repoName,
                                         @NotNull String branch,
                                         @NotNull CommitAdapter commitAdapter,
                                         @NotNull Collection<String> issueKeys,
                                         @NotNull Collection<CommitFileAdapter> commitFileAdapters) throws ParseException;
}
