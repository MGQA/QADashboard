package net.nitrogen.ates.core.model.test_group;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.IAtom;
import com.jfinal.plugin.activerecord.Model;

public class TestGroupModel extends Model<TestGroupModel> {
    public static final String TABLE = "test_group";

    public class Fields {
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String PROJECT_ID = "project_id";
    }

    public static final TestGroupModel me = new TestGroupModel();

    public static TestGroupModel createByResultSet(ResultSet rs) {
        TestGroupModel testGroup = new TestGroupModel();

        try {
            testGroup.setId(rs.getLong(Fields.ID));
            testGroup.setName(rs.getString(Fields.NAME));
            testGroup.setProjectId(rs.getLong(Fields.PROJECT_ID));
        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        return testGroup;
    }

    public long getId() {
        return getLong(Fields.ID);
    }

    public void setId(long id) {
        this.set(Fields.ID, id);
    }

    public String getName() {
        return getStr(Fields.NAME);
    }

    public void setName(String name) {
        this.set(Fields.NAME, name);
    }

    public long getProjectId() {
        return getLong(Fields.PROJECT_ID);
    }

    public void setProjectId(long projectId) {
        this.set(Fields.PROJECT_ID, projectId);
    }

    public List<TestGroupTestCaseModel> getTestGroupTestCases() {
        return TestGroupTestCaseModel.me.findTestGroupTestCases(this.getId());
    }

    public TestGroupModel findTestGroup(long testGroupId) {
        return findById(testGroupId);
    }

    public List<TestGroupModel> findTestGroups(long projectId) {
        return find(
                String.format("SELECT `%s`,`%s`,`%s` FROM `%s` WHERE `%s`=?", Fields.ID, Fields.NAME, Fields.PROJECT_ID, TABLE, Fields.PROJECT_ID),
                projectId);
    }

    public void insertTestGroups(List<TestGroupModel> testGroupsToReload) {
        final int INSERT_TEST_GROUP_PARAMS_SIZE = 2;
        final String insertTestGroupSql = String.format("INSERT `%s`(`%s`,`%s`) VALUES(?,?)", TABLE, Fields.NAME, Fields.PROJECT_ID);
        final Object[][] insertTestGroupParams = new Object[testGroupsToReload.size()][INSERT_TEST_GROUP_PARAMS_SIZE];

        for (int i = 0; i < testGroupsToReload.size(); i++) {
            insertTestGroupParams[i][0] = testGroupsToReload.get(i).getName();
            insertTestGroupParams[i][1] = testGroupsToReload.get(i).getProjectId();
        }

        Db.tx(new IAtom() {
            @Override
            public boolean run() throws SQLException {
                // Insert new test_group records
                Db.batch(insertTestGroupSql, insertTestGroupParams, 500);
                return true;
            }
        });
    }

    public void deleteTestGroupsAndRespectiveTestGroupTestCases(final long projectId) {
        final String deleteTestGroupTestCaseSql = String.format(
                "DELETE `tg-tc` FROM `%s` AS `tg-tc` INNER JOIN `%s` AS `tg` ON `tg`.`%s`=`tg-tc`.`%s` WHERE `tg`.`%s`=?",
                TestGroupTestCaseModel.TABLE,
                TestGroupModel.TABLE,
                TestGroupModel.Fields.ID,
                TestGroupTestCaseModel.Fields.TEST_GROUP_ID,
                TestGroupModel.Fields.PROJECT_ID);

        Db.tx(new IAtom() {
            @Override
            public boolean run() throws SQLException {
                // Delete existing test_group-test_case records
                Db.update(deleteTestGroupTestCaseSql, projectId);

                // Then delete existing test_group records
                Db.update(String.format("DELETE FROM `%s` WHERE `%s`=?", TABLE, Fields.PROJECT_ID), projectId);
                return true;
            }
        });
    }

}
