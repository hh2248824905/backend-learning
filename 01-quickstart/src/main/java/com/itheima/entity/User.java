package com.itheima.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
