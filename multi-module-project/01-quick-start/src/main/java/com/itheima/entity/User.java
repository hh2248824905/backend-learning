package com.itheima.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 用户实体类。
 *
 * 规范要点（来自培训）：
 * 1. 用 Lombok @Data 替代手写 Getter/Setter；
 * 2. @NoArgsConstructor + @AllArgsConstructor 自动生成构造器；
 * 3. 属性使用包装类（Long、Integer）而非基本类型（long、int），
 *    避免反射操作中的潜在问题；
 * 4. 日期统一用 JDK 8 的 LocalDate，而不是 java.util.Date / java.sql.Date。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /** 用户 ID（包装类型 Long） */
    private Long id;

    /** 用户名 */
    private String username;

    /** 年龄（包装类型 Integer） */
    private Integer age;

    /** 生日（JDK 8 时间 API） */
    private LocalDate birthday;
}
