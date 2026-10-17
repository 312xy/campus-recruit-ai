package com.campusrecruit.ai.dto;

import com.campusrecruit.ai.entity.InterviewSession;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 给前端看的面试会话详情。
 *
 * 字段含义（前端最关心的）：
 * - questions 题目列表
 * - answers 用户在每道题的作答 + AI 评分（按 question_index 对齐）
 * - summary 终评（所有题答完才生成，否则为 null）
 * - finished 是否已经结束
 */
@Data
@Slf4j
public class InterviewSessionResponse {

    private Long id;
    private Long jobId;
    private String jobTitle;
    private String status;
    private Integer totalScore;
    private List<InterviewQuestion> questions;
    private List<AnswerView> answers;
    private InterviewSummary summary;
    private LocalDateTime createdAt;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Data
    public static class AnswerView {
        private Integer questionIndex;
        private String questionText;
        private String userAnswer;
        private Integer score;
        private String feedback;
    }

    public static InterviewSessionResponse from(InterviewSession s, List<com.campusrecruit.ai.entity.InterviewAnswer> answerRows) {
        InterviewSessionResponse r = new InterviewSessionResponse();
        r.id = s.getId();
        r.jobId = s.getJobId();
        r.jobTitle = s.getJobTitle();
        r.status = s.getStatus();
        r.totalScore = s.getTotalScore();
        r.createdAt = s.getCreatedAt();

        // 反序列化 questions
        if (s.getQuestionsJson() != null) {
            QuestionsWrapper q = parseQuestions(s.getQuestionsJson());
            r.questions = q != null ? q.questions : new ArrayList<>();
        } else {
            r.questions = new ArrayList<>();
        }

        // 把每题作答转成 AnswerView 列表（按 questionIndex 对齐）
        List<AnswerView> list = new ArrayList<>();
        if (answerRows != null) {
            for (com.campusrecruit.ai.entity.InterviewAnswer a : answerRows) {
                AnswerView v = new AnswerView();
                v.setQuestionIndex(a.getQuestionIndex());
                v.setQuestionText(a.getQuestionText());
                v.setUserAnswer(a.getUserAnswer());
                v.setScore(a.getScore());
                v.setFeedback(a.getFeedback());
                list.add(v);
            }
        }
        r.answers = list;

        if (s.getSummaryJson() != null) {
            try { r.summary = MAPPER.readValue(s.getSummaryJson(), InterviewSummary.class); }
            catch (Exception e) { log.warn("summary 反序列化失败: {}", e.getMessage()); }
        }
        return r;
    }

    /** AI 返回的 JSON 结构里问题数组包在 questions 字段里 */
    @Data
    public static class QuestionsWrapper {
        private List<InterviewQuestion> questions;
    }

    private static QuestionsWrapper parseQuestions(String json) {
        try { return MAPPER.readValue(json, QuestionsWrapper.class); }
        catch (Exception e) {
            // 容错：也可能是直接的数组
            try {
                List<InterviewQuestion> qs = MAPPER.readValue(json, new TypeReference<List<InterviewQuestion>>() {});
                QuestionsWrapper w = new QuestionsWrapper();
                w.setQuestions(qs);
                return w;
            } catch (Exception ex) {
                log.warn("questions 反序列化失败: {}", ex.getMessage());
                return null;
            }
        }
    }
}