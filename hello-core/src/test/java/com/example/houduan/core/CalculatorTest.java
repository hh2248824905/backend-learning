package com.example.houduan.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Calculator 的单元测试。
 * 覆盖：正常路径 + 边界条件（除零异常）。
 */
class CalculatorTest {

    private final Calculator calc = new Calculator();

    @Test
    void add_returnsSum() {
        assertEquals(5, calc.add(2, 3));
        assertEquals(0, calc.add(-1, 1));
        assertEquals(-5, calc.add(-2, -3));
    }

    @Test
    void subtract_returnsDifference() {
        assertEquals(2, calc.subtract(5, 3));
        assertEquals(-2, calc.subtract(3, 5));
    }

    @Test
    void multiply_returnsProduct() {
        assertEquals(6, calc.multiply(2, 3));
        assertEquals(0, calc.multiply(0, 100));
        assertEquals(-6, calc.multiply(2, -3));
    }

    @Test
    void divide_returnsQuotient() {
        assertEquals(2, calc.divide(6, 3));
        assertEquals(-2, calc.divide(6, -3));
    }

    @Test
    void divide_byZero_throws() {
        assertThrows(IllegalArgumentException.class, () -> calc.divide(10, 0));
    }
}
