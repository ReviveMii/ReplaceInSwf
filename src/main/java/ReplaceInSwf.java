/*
 *  Copyright (C) 2026 ReviveMii Project & TheErrorExe, All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.tags.DefineSpriteTag;
import com.jpexs.decompiler.flash.tags.Tag;
import com.jpexs.decompiler.flash.tags.ABCContainerTag;
import com.jpexs.decompiler.flash.tags.base.ASMSource;
import com.jpexs.decompiler.flash.action.Action;
import com.jpexs.decompiler.flash.action.swf4.ActionPush;
import com.jpexs.decompiler.flash.action.swf5.ActionConstantPool;
import com.jpexs.decompiler.flash.abc.ABC;
import com.jpexs.decompiler.flash.abc.avm2.AVM2ConstantPool;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

public class ReplaceInSwf {

    static int replacedCount = 0;
    static int scanCount = 0;

    public static void main(String[] args) throws Exception {

    System.out.println("(c) 2026 ReviveMii, TheErrorExe. All Rights Reserved");


        if (args.length < 4) {
            System.out.println("Usage: java ReplaceInSwf.jar <input.swf> <output.swf> <search> <replace>");
            System.exit(1);
        }

        String inputFile  = args[0];
        String outputFile = args[1];
        String search     = args[2];
        String replace    = args[3];

        System.out.println("loading " + inputFile + "...");

        SWF swf;
        try (FileInputStream fis = new FileInputStream(inputFile)) {
            swf = new SWF(fis, true);
        }

        System.out.println("swf version:" + swf.version);
        System.out.println("scanning...");

        for (Tag t : swf.getTags()) {
            processTag(t, "root", search, replace);
        }

        System.out.println("saving " + outputFile + "...");
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            swf.saveTo(fos);
        }

        System.out.println();
        System.out.println("scanned script tags:" + scanCount);
        System.out.println("replaced: " + replacedCount);
        System.out.println("saved: " + outputFile);

        System.out.println();
        System.out.println("verifying...");
        SWF verifySwf;
        try (FileInputStream fis2 = new FileInputStream(outputFile)) {
            verifySwf = new SWF(fis2, true);
        }
        int[] remaining = {0};
        for (Tag t : verifySwf.getTags()) {
            countOccurrences(t, "root", search, remaining);
        }
        System.out.println("remaining \"" + search + "\" in " + outputFile + ": " + remaining[0]);
        System.out.println("replacing was SUCCESSFUL!");

    }

    static void processTag(Tag t, String path, String search, String replace) throws InterruptedException {
        String myPath = path + "/" + t.getClass().getSimpleName();

        if (t instanceof ASMSource) {
            ASMSource asmSource = (ASMSource) t;
            List<Action> actions = asmSource.getActions();

            scanCount++;
            boolean changed = false;

            for (Action a : actions) {

                if (a instanceof ActionConstantPool) {
                    ActionConstantPool cpool = (ActionConstantPool) a;
                    for (int i = 0; i < cpool.constantPool.size(); i++) {
                        String s = cpool.constantPool.get(i);
                        if (s != null && s.contains(search)) {
                            String newVal = s.replace(search, replace);
                            System.out.println("[HIT][ConstantPool] " + myPath + " Index " + i + ": \"" + s + "\" -> \"" + newVal + "\"");
                            cpool.constantPool.set(i, newVal);
                            changed = true;
                            replacedCount++;
                        }
                    }
                }

                if (a instanceof ActionPush) {
                    ActionPush push = (ActionPush) a;
                    List<Object> values = push.values;
                    for (int i = 0; i < values.size(); i++) {
                        Object v = values.get(i);
                        if (v instanceof String && ((String) v).contains(search)) {
                            String newVal = ((String) v).replace(search, replace);
                            System.out.println("[HIT][ActionPush]   " + myPath + " Index " + i + ": \"" + v + "\" -> \"" + newVal + "\"");
                            values.set(i, newVal);
                            changed = true;
                            replacedCount++;
                        }
                    }
                }
            }

            if (changed) {
                asmSource.setActions(actions);
                t.setModified(true);
            }
        }

        if (t instanceof ABCContainerTag) {
            ABC abc = ((ABCContainerTag) t).getABC();
            AVM2ConstantPool constants = abc.constants;
            scanCount++;
            boolean changed = false;
            for (int i = 0; i < constants.getStringCount(); i++) {
                String s = constants.getString(i);
                if (s != null && s.contains(search)) {
                    String newVal = s.replace(search, replace);
                    System.out.println("[HIT][AS3 ABC]      " + myPath + " Index " + i + ": \"" + s + "\" -> \"" + newVal + "\"");
                    constants.setString(i, newVal);
                    changed = true;
                    replacedCount++;
                }
            }
            if (changed) {
                t.setModified(true);
            }
        }

        if (t instanceof DefineSpriteTag) {
            for (Tag subTag : ((DefineSpriteTag) t).getTags()) {
                processTag(subTag, myPath, search, replace);
            }
        }
    }

    static void countOccurrences(Tag t, String path, String search, int[] counter) throws InterruptedException {
        String myPath = path + "/" + t.getClass().getSimpleName();

        if (t instanceof ASMSource) {
            List<Action> actions = ((ASMSource) t).getActions();
            for (Action a : actions) {
                if (a instanceof ActionConstantPool) {
                    for (String s : ((ActionConstantPool) a).constantPool) {
                        if (s != null && s.contains(search)) {
                            counter[0]++;
                            System.out.println("[REMAINING][ConstantPool] " + myPath + ": \"" + s + "\"");
                        }
                    }
                }
                if (a instanceof ActionPush) {
                    for (Object v : ((ActionPush) a).values) {
                        if (v instanceof String && ((String) v).contains(search)) {
                            counter[0]++;
                            System.out.println("[REMAINING][ActionPush]   " + myPath + ": \"" + v + "\"");
                        }
                    }
                }
            }
        }

        if (t instanceof ABCContainerTag) {
            AVM2ConstantPool constants = ((ABCContainerTag) t).getABC().constants;
            for (int i = 0; i < constants.getStringCount(); i++) {
                String s = constants.getString(i);
                if (s != null && s.contains(search)) {
                    counter[0]++;
                    System.out.println("[REMAINING][AS3 ABC]      " + myPath + ": \"" + s + "\"");
                }
            }
        }

        if (t instanceof DefineSpriteTag) {
            for (Tag subTag : ((DefineSpriteTag) t).getTags()) {
                countOccurrences(subTag, myPath, search, counter);
            }
        }
    }
}
