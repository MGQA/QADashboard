package net.nitrogen.ates.dashboard.controller;

import com.jfinal.core.Controller;

import net.nitrogen.ates.core.model.project.ProjectModel;
import net.nitrogen.ates.core.model.test_case.TestCaseListFactory;

public class TestCaseController extends Controller {
    public void index() {
        ControllerHelper.setExecResultEnumAttr(this);
        setAttr("projectModel", ProjectModel.me.findById(ControllerHelper.getProjectPrefFromCookie(this)));
        setAttr(
                "testCaseListWithAdditionalInfo",
                TestCaseListFactory.me().createTestCaseListWithAdditionalInfoForProject(ControllerHelper.getProjectPrefFromCookie(this)));
        render("index.html");
    }
}
