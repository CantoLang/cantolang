/* Canto Compiler and Runtime Engine
 *
 * CantoBuilderTest.java
 *
 * Copyright (c) 2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class CantoBuilderTest {


    @ParameterizedTest
    @DisplayName("Builder should be able to load a site from a string")
    @ValueSource(strings = {
        "site a { int x = 1; public show_x() { x; } }",
        "site b { public func_1 { sub_func_1(x) { \"x = \"; x; } } public func_2() { 2; } }",
        "site c { public f() [| Hello |] named { name [/] } typed { named[] super_type = [] } named param [/] param,typed typed_param [/] }",
        "site d { table_parent { element{} = { \"g\": \"G\" } } }"
    })
    public void testBuildSiteFromString(String siteStr) {
        CantoBuilder builder;
        Site site = null;
        try {
            builder = new CantoBuilder(siteStr);
            site = builder.buildSite(new Core(true));
        } catch (IOException e) {
            e.printStackTrace();
            fail("IOException should not occur when building from string");
        }
        assert site != null;
    }

    @ParameterizedTest
    @DisplayName("Builder should be able to parse a string and return a ComplexName")
    @ValueSource(strings = {
        "a",
        "x.y.z"
    })
    public void testBuildComplexName(String nameStr) {
        CantoBuilder builder;
        ComplexName complexName = null;
        try {
            builder = new CantoBuilder(nameStr);
            complexName = builder.buildComplexName();
        } catch (IOException e) {
            e.printStackTrace();
            fail("IOException should not occur when building ComplexName from string");
        }
        assert complexName != null;

    }

}