package dev.codedsakura.blossom.lib.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.stream.Collector;

public class TextSuperJoiner {
    private final MutableComponent prepend;
    private final MutableComponent append;
    private final MutableComponent join;

    private MutableComponent result;

    private TextSuperJoiner(Component prepend, Component append, Component join) {
        this.prepend = prepend.copy();
        this.append = append.copy();
        this.join = join.copy();
    }

    private TextSuperJoiner(Component prepend, Component append, Component join, MutableComponent result) {
        this.prepend = prepend.copy();
        this.append = append.copy();
        this.join = join.copy();
        this.result = result;
    }

    private void accumulator(Component text) {
        if (result == null) {
            result = prepend.copy()
                    .append(text)
                    .append(append);
        } else {
            result = result
                    .append(join)
                    .append(prepend)
                    .append(text)
                    .append(append);
        }
    }

    private TextSuperJoiner combiner(TextSuperJoiner b) {
        return new TextSuperJoiner(
                prepend,
                append,
                join,
                result.append(join).append(b.result)
        );
    }

    private MutableComponent finisher() {
        return result;
    }

    public static Collector<Component, ?, MutableComponent> collector(Component prepend, Component append, Component join) {
        return Collector.of(
                () -> new TextSuperJoiner(prepend, append, join),
                TextSuperJoiner::accumulator,
                TextSuperJoiner::combiner,
                TextSuperJoiner::finisher
        );
    }

    public static Collector<Component, ?, MutableComponent> joiner(Component join) {
        return Collector.of(
                () -> new TextSuperJoiner(Component.empty(), Component.empty(), join),
                TextSuperJoiner::accumulator,
                TextSuperJoiner::combiner,
                TextSuperJoiner::finisher
        );
    }
}
