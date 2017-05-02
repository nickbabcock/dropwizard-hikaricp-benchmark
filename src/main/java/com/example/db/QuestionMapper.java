package com.example.db;

import com.example.api.Question;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultColumnMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class QuestionMapper implements ResultSetMapper<Question> {
    @Override
    public Question map(int i, ResultSet r, StatementContext ctx) throws SQLException {
        final ResultColumnMapper<LocalDateTime> dtmapper = ctx.columnMapperFor(LocalDateTime.class);
        return Question.create(
                r.getInt(1),
                dtmapper.mapColumn(r, 2, ctx),
                dtmapper.mapColumn(r, 3, ctx),
                dtmapper.mapColumn(r, 4, ctx),
                r.getInt(5),
                r.getInt(6),
                r.getInt(7)
        );
    }
}
