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
import androidx.test.uiautomator.UiSelector;

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
            Thread.sleep(5000);
            registerWatchers();
            Log.i("registerWatchers");
            runWatchers();
            Log.i("runWatchers");
        }
    }
    void runWatchers() {
        AutomatorService automatorService = server.getAutomatorService();
        automatorService.runWatchers();
    }
    boolean isWatcherRegistered(String watcherName) {
        AutomatorService automatorService = server.getAutomatorService();
        String[] registeredWatchers = automatorService.getWatchers();
        for (String registeredWatcher : registeredWatchers) {
            if (registeredWatcher.equals(watcherName)) {
                return true;
            }
        }
        return false;
    }
    void registerWatchers() {
        AutomatorService automatorService = server.getAutomatorService();
        String[] registeredWatchers = automatorService.getWatchers();

        //check if MatchBack is registered
        if (!isWatcherRegistered("MatchBack")) {
            Selector conditions=new Selector().textMatches("Read status|Post view history turned on|Profile view history turned on|View your friends’ posts|Chat with more people|Feed accessibility tool is on|See who is online now|Posts are from followers that you follow back|Follow your friends|Let's do a quick security checkup|Create your TikTok avatar");
            String[] keys={"back"};
            automatorService.registerPressKeyskWatcher("MatchBack", new Selector[] {conditions}, keys);
        }
        //check if Match_v5 is registered
        if (!isWatcherRegistered("Match_v5")) {
            Selector conditions=new Selector().textMatches("Your friends on TikTok|Dismiss|Start watching|Don’t allow|Continue anyway|Post anyway|Not now|Agree and continue|Accept|Got it|Remind me later|Choose how ads are shown|Choose your interests");
            Selector target=new Selector().textMatches("Done|Dismiss|Start watching|Don’t allow|Skip|Continue anyway|Post anyway|Not now|Agree and continue|Accept|Got it|Remind me later|Select");
            automatorService.registerClickUiObjectWatcher("Match_v5", new Selector[] {conditions}, target);
        }
        //check if MatchCancel is registered
        if (!isWatcherRegistered("MatchCancel")) {
            Selector conditions=new Selector().textMatches("Your support means a lot to creators|Keep|How do you feel about the video you just watched?|Authorize|How do you feel about the video you just watched?");
            Selector target=new Selector().text("Cancel");
            automatorService.registerClickUiObjectWatcher("MatchCancel", new Selector[] {conditions}, target);
        }
        //check if MatchOk is registered
        if (!isWatcherRegistered("MatchOk")) {
            Selector conditions=new Selector().textMatches("Review your date of birth|Introducing 10 minutes video|View your friends’ posts");
            Selector target=new Selector().text("OK");
            automatorService.registerClickUiObjectWatcher("MatchOk", new Selector[] {conditions}, target);
        }
        //check if MatchConfirm is registered
        if (!isWatcherRegistered("MatchConfirm")) {        
            Selector conditions=new Selector().textMatches("Change|What languages do you understand");
            Selector target=new Selector().text("Confirm");
            automatorService.registerClickUiObjectWatcher("MatchConfirm", new Selector[] {conditions}, target);
        }
        //check if Discard is registered
        if (!isWatcherRegistered("Discard")) {
            Selector conditions=new Selector().text("Edit");
            Selector target=new Selector().text("Discard");
            automatorService.registerClickUiObjectWatcher("Discard", new Selector[] {conditions}, target);
        }
        //check if permission_allow_button is registered
        if (!isWatcherRegistered("permission_allow_button")) {
            Selector conditions=new Selector().text("Allow");
            Selector target=new Selector().text("Allow");
            automatorService.registerClickUiObjectWatcher("permission_allow_button", new Selector[] {conditions}, target);
        }
        //instagram Terms and Privacy Policy
        if (!isWatcherRegistered("Terms")) {
            Selector conditions=new Selector().text("Terms and Privacy Policy");
            Selector target=new Selector().text("Continue");
            automatorService.registerClickUiObjectWatcher("Terms", new Selector[] {conditions}, target);
        }
    }
}
