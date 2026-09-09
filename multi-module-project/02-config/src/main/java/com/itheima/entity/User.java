package com.itheima.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 用户实体，对应培训演示的 Lombok 实体类封装。
 *
 * 规范要点：
 * - @Data 自动生成 getter/setter/toString/equals/hashCode
 * - @NoArgsConstructor + @AllArgsConstructor 自动生成无参 / 全参构造器
 * - 属性使用包装类（Long / Integer）而非基本类型（long / int），避免反射问题
 * - 日期使用 LocalDate 替代 Date，消除 java.util.Date / java.sql.Date 二义性
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;

    private String username;

    private Integer age;

    private LocalDate birthday;
}
