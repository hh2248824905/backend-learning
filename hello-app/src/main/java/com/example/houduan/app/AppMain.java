package com.example.houduan.app;

import com.example.houduan.core.Calculator;

/**
 * 程序入口：演示 hello-app 如何调用 hello-core。
 * 父工程通过 maven-jar-plugin 把这个类配置成了 main-class，
 * 所以可以直接 java -jar hello-app.jar 运行。
 */
public class AppMain {
    public static void main(String[] args) {
        Calculator calc = new Calculator();

        int a = 10;
        int b = 3;

        System.out.println("=== 后端父子工程演示 ===");
        System.out.printf("%d + %d = %d%n", a, b, calc.add(a, b));
        System.out.printf("%d - %d = %d%n", a, b, calc.subtract(a, b));
        System.out.printf("%d * %d = %d%n", a, b, calc.multiply(a, b));
        System.out.printf("%d / %d = %d%n", a, b, calc.divide(a, b));

        // 故意触发除零异常，验证父工程里 hello-core 写的校验
        try {
            calc.divide(a, 0);
        } catch (IllegalArgumentException e) {
            System.out.println("异常被正确抛出：" + e.getMessage());
        }
    }
}
