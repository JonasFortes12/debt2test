package io.github.jonasfortes12.context.extraction;

import java.util.List;

@FunctionalInterface
public interface ReferenceRule {
    List<ReferenceMatch> extract(String comment);
}
