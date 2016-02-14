package net.nitrogen.ates.testresultreporter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.URL;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestResult;

import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.druid.DruidPlugin;

import net.nitrogen.ates.core.config.DBConfig;
import net.nitrogen.ates.core.enumeration.ExecResult;
import net.nitrogen.ates.core.env.EnvParameter;
import net.nitrogen.ates.core.model.email.EmailModel;
import net.nitrogen.ates.core.model.slave.SlaveModel;
import net.nitrogen.ates.core.model.test_case.TestCaseModel;
import net.nitrogen.ates.core.model.test_result.TestResultModel;
import net.nitrogen.ates.util.PropertiesUtil;

public class TestResultReporter {
    public static final String DRIVER_ATTR = "driver";
    public static String IP = "";
    private static final String TESTCLASS_TESTMETHOD_DELIMITER = ".";
    private static final String NGINX_PORT = "80";
    private static final String NGINX_PATH = "ates_test_result_screenshots";
    private static final String NGINX_REAL_PATH = "C:/ates/test_output_screenshots";

    public void report(ITestResult result, ExecResult status) throws SQLException {
        Properties props = PropertiesUtil.load("config.txt");
        DruidPlugin druidPlugin = DBConfig
                .createDruidPlugin(props.getProperty("jdbcUrl"), props.getProperty("dbuser"), props.getProperty("dbpassword"), 0, 0, 1);
        druidPlugin.start();

        String configName = String.format("ates_testresultreporter_arp_config_%d", DateTime.now().getMillis());
        System.out.println("ConfigName:" + configName);
        ActiveRecordPlugin arp = DBConfig.createActiveRecordPlugin(druidPlugin, configName);
        arp.start();
        TestResultModel.me.insertTestResult(this.prepareTestResult(result, status));
        EmailModel.me.checkAndMarkExecutionEmailAsReady(EnvParameter.executionId());
        arp.stop();
        druidPlugin.stop();
    }

    private TestResultModel prepareTestResult(ITestResult result, ExecResult status) {
        TestResultModel testResult = new TestResultModel();
        testResult.setEntryId(EnvParameter.entryId());
        final String caseName = String.format("%s%s%s", result.getTestClass().getName(), TESTCLASS_TESTMETHOD_DELIMITER, result.getMethod().getMethodName());
        testResult.setTestCaseId(TestCaseModel.me.findValidTestCase(EnvParameter.projectId(), caseName).getId());
        testResult.setSlaveName(EnvParameter.machineName());
        testResult.setStartTime(new DateTime(result.getStartMillis()));
        testResult.setEndTime(new DateTime(result.getEndMillis()));
        testResult.setMessage("");
        testResult.setStackTrace("");
        testResult.setScreenshotUrl("");
        testResult.setExecutionId(EnvParameter.executionId());
        testResult.setProjectId(EnvParameter.projectId());
        StringBuilder message = new StringBuilder();

        if (result.getThrowable() != null) {
            Throwable t = result.getThrowable();
            StringWriter errors = new StringWriter();
            t.printStackTrace(new PrintWriter(errors));
            testResult.setStackTrace(errors.toString());
            message.append(t.getMessage());
        }

        switch (status) {
        case FAILED:
            testResult.setExecResult(ExecResult.FAILED.getValue());
            this.takeScreenshot(result, message, testResult);
            break;
        case PASSED:
            testResult.setExecResult(ExecResult.PASSED.getValue());
            break;
        case SKIPPED:
            testResult.setExecResult(ExecResult.SKIPPED.getValue());
            this.takeScreenshot(result, message, testResult);
            break;
        default:
            testResult.setExecResult(ExecResult.UNKNOWN.getValue());
            break;
        }

        testResult.setMessage(message.toString());
        return testResult;
    }

    private void takeScreenshot(ITestResult result, StringBuilder message, TestResultModel testResult) {
        String fileName = String.format("%s.png", result.getName() + "_" + System.currentTimeMillis());
        String imageFolderPath = String.format("%s/%d", NGINX_REAL_PATH, testResult.getProjectId());
        String imagePath = String.format("%s/%d/%s", NGINX_REAL_PATH, testResult.getProjectId(), fileName);
        File folder = new File(imageFolderPath);
        folder.mkdirs();
        // String imagePath = String.format("%s%s", file.getAbsolutePath(), imageRelativePath);

        try {
            if (IP == null || IP.isEmpty()) {
                IP = System.getProperty("slave.ip");
            }
            if (IP == null || IP.isEmpty()) {
                IP = SlaveModel.me.findFirst("select * from slave where machine_name = '" + EnvParameter.machineName() + "'").get(SlaveModel.Fields.PUBLIC_IP);
            }
            if (IP == null || IP.isEmpty()) {
                URL whatismyip;
                whatismyip = new URL("http://checkip.amazonaws.com");
                BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
                IP = in.readLine(); // you get the IP as a String
                in.close();
                SlaveModel.me.findFirst("select * from slave where machine_name = '" + EnvParameter.machineName() + "'").set(SlaveModel.Fields.PUBLIC_IP, IP)
                        .save();
                System.setProperty("slave.ip", IP);
            }
            if (IP == null || IP.isEmpty()) {
                IP = Inet4Address.getLocalHost().getHostAddress();
                SlaveModel.me.findFirst("select * from slave where machine_name = '" + EnvParameter.machineName() + "'").set(SlaveModel.Fields.PUBLIC_IP, IP)
                        .save();
                System.setProperty("slave.ip", IP);
            }
        }
        catch (IOException e) {
            message.append("Error generating screenshot: " + e.getMessage());
            e.printStackTrace();
            IP = "";
        }

        String imageUrl = String.format("http://%s:%s/%s/%d/%s", IP, NGINX_PORT, NGINX_PATH, testResult.getProjectId(), fileName);
        ITestContext context = result.getTestContext();
        WebDriver driver = (WebDriver) context.getAttribute(DRIVER_ATTR + Thread.currentThread().getId());

        if (driver != null && !(driver.toString().contains("(null)"))) {
            try {
                message.append(String.format(" ; Current url:%s", driver.getCurrentUrl()));
                File f = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                File saved = new File(imagePath);
                FileUtils.copyFile(f, saved);
                testResult.setScreenshotUrl(imageUrl);
            }
            catch (Exception e) {
                message.append("Error generating screenshot: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
