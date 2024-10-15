package com.thunder.base.diff;

import java.util.Collection;

record ResultNode(String fieldName,
                         Object oldValue,
                         Object newValue,
                         ResultNodeState state,
                         Collection<ResultNode> children) {

}
