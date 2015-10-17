package net.nitrogen.ates.core.entity;

public class TestGroupTestCase {
	private long testGroupId;
	private long testCaseId;
	private String testName;

	public long getTestGroupId() {
		return testGroupId;
	}

	public void setTestGroupId(long testGroupId) {
		this.testGroupId = testGroupId;
	}

	public String getTestName() {
		return testName;
	}

	public void setTestName(String testName) {
		this.testName = testName;
	}

	public long getTestCaseId() {
		return testCaseId;
	}

	public void setTestCaseId(long testCaseId) {
		this.testCaseId = testCaseId;
	}
}
