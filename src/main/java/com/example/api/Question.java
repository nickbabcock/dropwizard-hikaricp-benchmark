package com.example.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.time.LocalDateTime;
import java.util.Optional;

@AutoValue
public abstract class Question {
    @JsonProperty
    public abstract int serial();

    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    public abstract LocalDateTime creation();

    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    public abstract Optional<LocalDateTime> closed();

    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    public abstract Optional<LocalDateTime> deletion();

    @JsonProperty
    public abstract int score();

    @JsonProperty
    public abstract int ownerUserId();

    @JsonProperty
    public abstract int answers();

    @JsonCreator
    public static Question create(
            int serial,
            LocalDateTime creation,
            LocalDateTime closed,
            LocalDateTime deletion,
            int score,
            int owner,
            int answers
    ) {
        return new AutoValue_Question(
                serial,
                creation,
                Optional.ofNullable(closed),
                Optional.ofNullable(deletion),
                score,
                owner,
                answers
        );
    }
}
