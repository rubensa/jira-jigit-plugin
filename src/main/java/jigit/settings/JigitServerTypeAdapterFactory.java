package jigit.settings;

import jigit.client.github.GitHub;
import jigit.indexer.repository.ServiceType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public final class JigitServerTypeAdapterFactory extends PostProcessAdapterFactory {
    protected void postProcess(@NotNull Object obj) {
        if (!(obj instanceof JigitRepo)) {
            return;
        }
        try {
            final JigitRepo jigitRepo = (JigitRepo) obj;
            final Field[] declaredFields = JigitRepo.class.getDeclaredFields();
            for (final Field field : declaredFields) {
                if (field.getType() == ServiceType.class) {
                    field.setAccessible(true);
                    if (field.get(obj) == null) {
                        field.set(obj, getServiceType(jigitRepo));
                    }
                }
            }
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private ServiceType getServiceType(@NotNull JigitRepo jigitRepo) {
        return GitHub.isGitHubSite(jigitRepo.getServerUrl()) ? ServiceType.GitHub : ServiceType.GitLab;
    }
}
