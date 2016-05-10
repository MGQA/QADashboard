package net.nitrogen.ates.testresultreporter;

import java.sql.SQLException;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import net.nitrogen.ates.core.enumeration.ExecResult;
import net.nitrogen.ates.core.env.EnvParameter;

public class TestResultListener implements ITestListener {
    @Override
    public void onTestStart(ITestResult result) {
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        this.reportResultIfNotDebug(result, ExecResult.PASSED);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        this.reportResultIfNotDebug(result, ExecResult.FAILED);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        result.setStatus(ITestResult.FAILURE);
        this.reportResultIfNotDebug(result, ExecResult.FAILED);
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
    }

    @Override
    public void onStart(ITestContext context) {
    }

    @Override
    public void onFinish(ITestContext context) {
    }

    private void reportResultIfNotDebug(ITestResult result, ExecResult status) {
        if (!EnvParameter.isDebug()) {
            try {
                new TestResultReporter().report(result, status);
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
