package zju.cst.aces.api.phase.step;

import zju.cst.aces.api.config.Config;
import zju.cst.aces.api.impl.PromptConstructorImpl;
import zju.cst.aces.dto.ClassInfo;
import zju.cst.aces.dto.MethodInfo;
import zju.cst.aces.dto.PromptInfo;
import zju.cst.aces.util.symprompt.PathConstraintExtractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class PromptGeneration {
    private final Config config;
    private final ClassInfo classInfo;
    private final MethodInfo methodInfo;
    private static final String separator = "_";

    public PromptGeneration(Config config, ClassInfo classInfo, MethodInfo methodInfo) {
        this.config = config;
        this.classInfo = classInfo;
        this.methodInfo = methodInfo;
    }

    public PromptConstructorImpl execute(int num) {
        String linePart = (methodInfo.targetLine == null)
                ? ""
                : separator + "L" + methodInfo.targetLine;

        String testName = classInfo.getClassName()
                + separator + methodInfo.methodName
                + separator + classInfo.methodSigs.get(methodInfo.methodSignature)
                + linePart
                + separator + num
                + separator + "Test";

        String fullTestName = classInfo.getFullClassName()
                + separator + methodInfo.methodName
                + separator + classInfo.methodSigs.get(methodInfo.methodSignature)
                + linePart
                + separator + num
                + separator + "Test";
        config.getLogger().info(String.format("\n==========================\n[%s] Generating test for method < ",
                config.pluginSign) + methodInfo.methodName + " > number " + num + "...\n");

        try {
            PromptConstructorImpl pc = new PromptConstructorImpl(config);
            if (!methodInfo.dependentMethods.isEmpty()) {
                pc.setPromptInfoWithDep(classInfo, methodInfo);
            } else {
                pc.setPromptInfoWithoutDep(classInfo, methodInfo);            }
            pc.setFullTestName(fullTestName);
            pc.setTestName(testName);

            PromptInfo promptInfo = pc.getPromptInfo();
            if (methodInfo.targetLine != null) {
                String ctx = promptInfo.getContext();
                promptInfo.setContextWithTargetLine(injectTargetLineNumber(ctx, methodInfo));
            }
            promptInfo.setFullTestName(fullTestName);
            Path savePath = config.getTestOutput().resolve(fullTestName.replace(".", File.separator) + ".java");
            promptInfo.setTestPath(savePath);

            promptInfo.setTestNum(num);

            return pc;

        } catch (IOException e) {
            throw new RuntimeException("In PromptGeneration.execute: " + e);
        }
    }

    private String injectTargetLineNumber(String context, MethodInfo methodInfo) {
        if (methodInfo.targetLine == null) {
            return context;
        }

        String[] lines = context.split("\n", -1);

        int targetLine = methodInfo.targetLine;
        int digits = String.valueOf(targetLine).length();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits; i++) {
            sb.append(' ');
        }
        String blankGutter = sb.toString() + "   ";
        String targetGutter = targetLine + " : ";

        // add gutter to every line
        for (int i = 0; i < lines.length; i++) {
            lines[i] = blankGutter + lines[i];
        }

        // find method declaration line (skip annotations like @Override)
        int declIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(methodInfo.methodName + "(")) {
                declIndex = i;
                break;
            }
        }

        if (declIndex == -1) {
            return String.join("\n", lines);
        }

        // compute relative offset
        int relative = methodInfo.targetLine - methodInfo.startLine;

        // adjust for leading annotations in sourceCode
        if (methodInfo.sourceCode != null) {
            String[] srcLines = methodInfo.sourceCode.split("\n");
            int annotationCount = 0;
            for (String l : srcLines) {
                if (l.trim().startsWith("@")) {
                    annotationCount++;
                } else {
                    break;
                }
            }
            relative -= annotationCount;
        }

        int targetIndex = declIndex + relative;

        if (targetIndex >= 0 && targetIndex < lines.length) {
            lines[targetIndex] =
                    targetGutter + lines[targetIndex].substring(blankGutter.length());
        }

        return String.join("\n", lines);
    }
}
