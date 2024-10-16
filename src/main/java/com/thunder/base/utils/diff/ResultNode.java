package com.thunder.base.utils.diff;

import java.util.Collection;

record ResultNode(String fieldName,
                         Object oldValue,
                         Object newValue,
                         ResultNodeState state,
                         Collection<ResultNode> children) {

    public enum ResultNodeState {
        ADDED, CHANGED, UNTOUCHED, REMOVED
    }

}
