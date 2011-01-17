/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dalvik.system;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import libcore.base.Streams;
import junit.framework.TestCase;

/**
 * Tests for the class {@link DexClassLoader}.
 */
public class DexClassLoaderTest extends TestCase {
    private static final File TMP_DIR =
        new File(System.getProperty("java.io.tmpdir"), "loading-test");
    private static final String PACKAGE_PATH = "dalvik/system/";
    private static final String JAR_NAME = "loading-test.jar";
    private static final String DEX_NAME = "loading-test.dex";
    private static final String JAR2_NAME = "loading-test2.jar";
    private static final String DEX2_NAME = "loading-test2.dex";
    private static final File JAR_FILE = new File(TMP_DIR, JAR_NAME);
    private static final File DEX_FILE = new File(TMP_DIR, DEX_NAME);
    private static final File JAR2_FILE = new File(TMP_DIR, JAR2_NAME);
    private static final File DEX2_FILE = new File(TMP_DIR, DEX2_NAME);

    private static enum Configuration {
        /** just one classpath element, a raw dex file */
        ONE_DEX,

        /** just one classpath element, a jar file */
        ONE_JAR,

        /** two classpath elements, both raw dex files */
        TWO_DEX,

        /** two classpath elements, both jar files */
        TWO_JAR;
    }

    protected void setUp() throws IOException {
        TMP_DIR.mkdirs();

        ClassLoader cl = DexClassLoaderTest.class.getClassLoader();
        copyResource(cl, JAR_NAME, JAR_FILE);
        copyResource(cl, DEX_NAME, DEX_FILE);
        copyResource(cl, JAR2_NAME, JAR2_FILE);
        copyResource(cl, DEX2_NAME, DEX2_FILE);
    }

    /**
     * Copy a resource in the package directory to the indicated file.
     */
    private static void copyResource(ClassLoader loader, String resourceName,
            File destination) throws IOException {
        InputStream in =
            loader.getResourceAsStream(PACKAGE_PATH + resourceName);
        FileOutputStream out = new FileOutputStream(destination);
        Streams.copy(in, out);
        in.close();
        out.close();
    }

    /**
     * Helper to construct an instance to test.
     *
     * @param config how to configure the classpath
     */
    private static DexClassLoader createInstance(Configuration config) {
        File file1;
        File file2;

        switch (config) {
            case ONE_DEX: file1 = DEX_FILE; file2 = null;      break;
            case ONE_JAR: file1 = JAR_FILE; file2 = null;      break;
            case TWO_DEX: file1 = DEX_FILE; file2 = DEX2_FILE; break;
            case TWO_JAR: file1 = JAR_FILE; file2 = JAR2_FILE; break;
            default: throw new AssertionError("shouldn't happen");
        }

        String path = file1.getAbsolutePath();
        if (file2 != null) {
            path += File.pathSeparator + file2.getAbsolutePath();
        }

        return new DexClassLoader(
            path, TMP_DIR.getAbsolutePath(), null,
            ClassLoader.getSystemClassLoader());
    }

    /**
     * Helper to construct an instance to test, using the jar file as
     * the source, and call a named no-argument static method on a
     * named class.
     *
     * @param config how to configure the classpath
     */
    public static Object createInstanceAndCallStaticMethod(
            Configuration config, String className, String methodName)
            throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        DexClassLoader dcl = createInstance(config);
        Class c = dcl.loadClass(className);
        Method m = c.getMethod(methodName, (Class[]) null);
        return m.invoke(null, (Object[]) null);
    }

    /*
     * Tests that are parametric with respect to whether to use a jar
     * file or a dex file as the source of the code
     */

    /**
     * Just a trivial test of construction. This one merely makes
     * sure that a valid construction doesn't fail; it doesn't try
     * to verify anything about the constructed instance. (Other
     * tests will do that.)
     */
    public static void test_init(Configuration config) {
        createInstance(config);
    }

    /**
     * Check that a class in the jar/dex file may be used successfully. In this
     * case, a trivial static method is called.
     */
    public static void test_simpleUse(Configuration config) throws Exception {
        String result = (String)
            createInstanceAndCallStaticMethod(config, "test.Test1", "test");

        assertSame("blort", result);
    }

    /*
     * All the following tests are just pass-throughs to test code
     * that lives inside the loading-test dex/jar file.
     */

    public static void test_callStaticMethod(Configuration config)
            throws Exception {
        createInstanceAndCallStaticMethod(
            config, "test.TestMethods", "test_callStaticMethod");
    }

    public static void test_getStaticVariable(Configuration config)
            throws Exception {
        createInstanceAndCallStaticMethod(
            config, "test.TestMethods", "test_getStaticVariable");
    }

    public static void test_callInstanceMethod(Configuration config)
            throws Exception {
        createInstanceAndCallStaticMethod(
            config, "test.TestMethods", "test_callInstanceMethod");
    }

    public static void test_getInstanceVariable(Configuration config)
            throws Exception {
        createInstanceAndCallStaticMethod(
            config, "test.TestMethods", "test_getInstanceVariable");
    }

    /*
     * These methods are all essentially just calls to the
     * parametrically-defined tests above. As a mnemonic name
     * differentiator, all the tests that use a two-element classpath
     * have a "2" suffix in their name (even though some such tests
     * don't have an equivalent single-element version).
     */

    public void test_jar_init() throws Exception {
        test_init(Configuration.ONE_JAR);
    }

    public void test_jar_simpleUse() throws Exception {
        test_simpleUse(Configuration.ONE_JAR);
    }

    public void test_jar_callStaticMethod() throws Exception {
        test_callStaticMethod(Configuration.ONE_JAR);
    }

    public void test_jar_getStaticVariable() throws Exception {
        test_getStaticVariable(Configuration.ONE_JAR);
    }

    public void test_jar_callInstanceMethod() throws Exception {
        test_callInstanceMethod(Configuration.ONE_JAR);
    }

    public void test_jar_getInstanceVariable() throws Exception {
        test_getInstanceVariable(Configuration.ONE_JAR);
    }

    public void test_dex_init() throws Exception {
        test_init(Configuration.ONE_DEX);
    }

    public void test_dex_simpleUse() throws Exception {
        test_simpleUse(Configuration.ONE_DEX);
    }

    public void test_dex_callStaticMethod() throws Exception {
        test_callStaticMethod(Configuration.ONE_DEX);
    }

    public void test_dex_getStaticVariable() throws Exception {
        test_getStaticVariable(Configuration.ONE_DEX);
    }

    public void test_dex_callInstanceMethod() throws Exception {
        test_callInstanceMethod(Configuration.ONE_DEX);
    }

    public void test_dex_getInstanceVariable() throws Exception {
        test_getInstanceVariable(Configuration.ONE_DEX);
    }

    /*
     * Tests specifically for resource-related functionality.  Since
     * raw dex files don't contain resources, these test only work
     * with jar files. The first couple methods here are helpers,
     * and they are followed by the tests per se.
     */

    /**
     * Check that a given resource (by name) is retrievable and contains
     * the given expected contents.
     */
    public static void test_directGetResourceAsStream(Configuration config,
            String resourceName, String expectedContents)
            throws Exception {
        DexClassLoader dcl = createInstance(config);
        InputStream in = dcl.getResourceAsStream(resourceName);
        byte[] contents = Streams.readFully(in);
        String s = new String(contents, "UTF-8");

        assertEquals(expectedContents, s);
    }

    /**
     * Check that a resource in the jar file is retrievable and contains
     * the expected contents.
     */
    public static void test_directGetResourceAsStream(Configuration config)
            throws Exception {
        test_directGetResourceAsStream(
            config, "test/Resource1.txt", "Muffins are tasty!\n");
    }

    /**
     * Check that a resource in the jar file can be retrieved from
     * a class within that jar file.
     */
    public static void test_getResourceAsStream(Configuration config)
            throws Exception {
        createInstanceAndCallStaticMethod(
            config, "test.TestMethods", "test_getResourceAsStream");
    }

    public void test_directGetResourceAsStream() throws Exception {
        test_directGetResourceAsStream(Configuration.ONE_JAR);
    }

    public void test_getResourceAsStream() throws Exception {
        test_getResourceAsStream(Configuration.ONE_JAR);
    }

    public void test_directGetResourceAsStream2() throws Exception {
        test_directGetResourceAsStream(Configuration.TWO_JAR);
    }

    public void test_getResourceAsStream2() throws Exception {
        test_getResourceAsStream(Configuration.TWO_JAR);
    }

    /**
     * Check that a resource in the second jar file is retrievable and
     * contains the expected contents.
     */
    public static void test_diff_directGetResourceAsStream2()
            throws Exception {
        test_directGetResourceAsStream(
            Configuration.TWO_JAR, "test2/Resource2.txt",
            "Who doesn't like a good biscuit?\n");
    }

    /**
     * Check that a resource in a jar file can be retrieved from
     * a class within the other jar file.
     */
    public static void test_diff_getResourceAsStream2()
            throws Exception {
        createInstanceAndCallStaticMethod(
            Configuration.TWO_JAR, "test.TestMethods",
            "test_diff_getResourceAsStream");
    }
}
