package com.example.db;

import com.example.api.Question;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;

import java.util.List;

public interface QuestionQuery {
    @SqlQuery("SELECT id, creationDate, closedDate, deletionDate, score, ownerUserId, answerCount\n" +
            "FROM questions WHERE ownerUserId = :user")
    List<Question> findQuestionsFromUser(@Bind("user") int user);
}
