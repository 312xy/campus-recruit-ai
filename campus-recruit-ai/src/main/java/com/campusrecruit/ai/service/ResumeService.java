package com.campusrecruit.ai.service;

import com.campusrecruit.ai.dto.ResumeResponse;

import java.util.List;

/**
 * 简历业务接口。
 *
 * 人话讲解：
 * - 为什么要"接口 + 实现类"分开写（ResumeService + ResumeServiceImpl）？
 *   ① 面向接口编程：Controller 只依赖接口，将来换实现（比如换成别的 AI 厂商）不用改 Controller；
 *   ② 便于测试：可以写一个假的实现类来跑单元测试；
 *   ③ 这是 Spring 项目的标准分层写法，也是一眼能看出的规范度。
 */
public interface ResumeService {

    /** 解析并保存一份简历（文本形式） */
    ResumeResponse parseAndSave(Long userId, String fileName, String rawText);

    /** 我的简历列表（按时间倒序） */
    List<ResumeResponse> listMine(Long userId);

    /** 查我的某一份简历详情 */
    ResumeResponse getMine(Long userId, Long resumeId);

    /** 删除我的某一份简历 */
    void deleteMine(Long userId, Long resumeId);
}
