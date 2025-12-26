package zju.cst.aces.runner;

import zju.cst.aces.api.config.Config;
import zju.cst.aces.dto.MethodInfo;

import java.io.IOException;

public class LineRunner extends MethodRunner {

    private final int lineNumber;

    public LineRunner(Config config,
                      String fullClassName,
                      MethodInfo methodInfo,
                      int lineNumber) throws IOException {
        super(config, fullClassName, methodInfo);
        this.lineNumber = lineNumber;
    }

    @Override
    public boolean startRounds(final int num) throws IOException {
        // expose line constraint
        methodInfo.targetLine = lineNumber;
        return super.startRounds(num);
    }
}