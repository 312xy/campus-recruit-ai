package com.campusrecruit.ai.service;

import com.campusrecruit.ai.dto.JobResponse;

import java.util.List;

/**
 * 岗位业务接口。
 *
 * 人话讲解：
 * - 接口（interface）只声明"能做什么"，不写"怎么做"。
 *   真正干活的是 impl 包下的 JobServiceImpl。
 * - 为什么明明只有一个实现类，还要分接口 + 实现？
 *   ① Controller 依赖接口，将来换成别的实现（比如接第三方岗位库）不用改 Controller；
 *   ② Spring 事务/代理更适合基于接口来理解；
 *   ③ 团队协作时，接口就是"契约"，写接口的人定好签名，实现可以别人写。
 *   （小项目里确实有点啰嗦，但这是主流工程习惯，面试常规问题，知道就行。）
 */
public interface JobService {

    /**
     * 提交一份岗位 JD，让 AI 打标签并入库。
     *
     * @param userId  当前登录用户（从 token 里解析出来，绝不信前端传的）
     * @param rawText JD 原文
     */
    JobResponse parseAndSave(Long userId, String rawText);

    /** 我录入的岗位列表（按时间倒序） */
    List<JobResponse> listMine(Long userId);

    /** 我的某个岗位详情 */
    JobResponse getMine(Long userId, Long jobId);

    /** 删除我录入的某个岗位 */
    void deleteMine(Long userId, Long jobId);
}
