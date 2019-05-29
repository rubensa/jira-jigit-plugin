package jigit.common;

import org.jetbrains.annotations.Nullable;

public enum CommitActionHelper {
    Instance;

    @SuppressWarnings("unused")
    //used in velocity
    @Nullable
    public CommitAction parse(int id) {
        for (CommitAction action : CommitAction.values) {
            if (action.getId() == id) {
                return action;
            }
        }

        return null;
    }
}
