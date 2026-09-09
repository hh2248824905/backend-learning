package com.example.houduan.core;

/**
 * 一个最简单的可测试计算器。
 * 设计目的：演示父子工程结构、纯逻辑代码 + 单元测试的写法。
 *
 * 注意：这是教学示例代码，不处理 BigDecimal 精度、生产级校验等真实工程问题。
 */
public class Calculator {

    /** 两数相加 */
    public int add(int a, int b) {
        return a + b;
    }

    /** 两数相减 */
    public int subtract(int a, int b) {
        return a - b;
    }

    /** 两数相乘 */
    public int multiply(int a, int b) {
        return a * b;
    }

    /**
     * 两数相除。
     * @throws IllegalArgumentException 当除数为 0 时
     */
    public int divide(int a, int b) {
        if (b == 0) {
            throw new IllegalArgumentException("除数不能为 0");
        }
        return a / b;
    }
}
