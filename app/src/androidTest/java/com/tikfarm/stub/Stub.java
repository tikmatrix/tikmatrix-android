/*
 * The MIT License (MIT)
 * Copyright (c) 2015 xiaocong@gmail.com, 2018 codeskyblue@gmail.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.tikmatrix.stub;

import android.content.Context;
import android.content.Intent;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.ErrorResolver;
import com.googlecode.jsonrpc4j.JsonRpcServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Use JUnit test to start the uiautomator jsonrpc server.
 *
 * @author xiaocong@gmail.com
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class Stub {
    // http://www.jsonrpc.org/specification#error_object
    private static final int CUSTOM_ERROR_CODE = -32001;

    int PORT = 9008;
    AutomatorHttpServer server = new AutomatorHttpServer(PORT);

    @Before
    public void setUp() throws Exception {
        Log.i("Launch Stub Server");
        AutomatorService automatorService = new AutomatorServiceImpl();
        JsonRpcServer jrs = new JsonRpcServer(new ObjectMapper(), automatorService, AutomatorService.class);
        jrs.setShouldLogInvocationErrors(true);
        jrs.setErrorResolver(new ErrorResolver() {
            @Override
            public JsonError resolveError(Throwable throwable, Method method, List<JsonNode> list) {
                String data = throwable.getMessage();
                if (!throwable.getClass().equals(UiObjectNotFoundException.class)) {
                    throwable.printStackTrace();
                    StringWriter sw = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(sw));
                    data = sw.toString();
                }
                return new JsonError(CUSTOM_ERROR_CODE, throwable.getClass().getName(), data);
            }
        });
        server.route("/jsonrpc/0", jrs);
        server.setAutomatorService(automatorService);
        server.start();
    }

    @After
    public void tearDown() {
        server.stop();
    }

    @Test
    @LargeTest
    public void testUIAutomatorStub() throws InterruptedException {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        while (server.isAlive()) {
            context.sendBroadcast(new Intent("com.github.tikmatrix.stub.STUB_RUNNING"));
            Thread.sleep(1000);
        }
    }
}