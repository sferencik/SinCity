package sferencik.teamcity.sincity.failureCulpritOverview;

import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.vcs.SVcsModification;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sferencik.teamcity.sincity.FinishedBuildWithChanges;

import java.util.*;

public class Table {
    private final List<TestName> testFailures;
    private List<TableCell<FinishedBuildWithChanges>> buildRow = new ArrayList<TableCell<FinishedBuildWithChanges>>();
    private List<SVcsModification> changeRow = new ArrayList<SVcsModification>();
    private ArrayList<ArrayList<TableCell<FinishedBuildWithChanges>>> tableCells;

    public Table(
            List<TestName> testFailures,
            List<FinishedBuildWithChanges> builds,
            Map<TestName, FinishedBuildWithChanges> buildInWhichIssueFirstOccurred,
            Map<TestName, Set<FinishedBuildWithChanges>> buildsSuspectedOfCausingIssue) {

        this.testFailures = testFailures;

        int columns = 0;
        Map<FinishedBuildWithChanges, Integer> buildColumn = new HashMap<FinishedBuildWithChanges, Integer>();
        for (FinishedBuildWithChanges build : builds) {
            buildColumn.put(build, columns);
            List<SVcsModification> changeDelta = build.getChangeDelta();
            if (changeDelta.isEmpty()) {
                buildRow.add(new TableCell<FinishedBuildWithChanges>(build, 1));
                changeRow.add(null);
                columns++;
            }
            else {
                int changeDeltaSize = changeDelta.size();
                buildRow.add(new TableCell<FinishedBuildWithChanges>(build, changeDeltaSize));
                buildRow.addAll(Collections.<TableCell<FinishedBuildWithChanges>>nCopies(changeDeltaSize - 1, null));
                changeRow.addAll(changeDelta);
                columns += changeDeltaSize;
            }
        }

        tableCells = new ArrayList<ArrayList<TableCell<FinishedBuildWithChanges>>>(testFailures.size());
        List<TableCell<FinishedBuildWithChanges>> emptyRow = Collections.nCopies(columns, new TableCell<FinishedBuildWithChanges>(null, 1));

        for (TestName testFailure : testFailures) {
            ArrayList<TableCell<FinishedBuildWithChanges>> rowOfCells = new ArrayList<TableCell<FinishedBuildWithChanges>>(emptyRow);
            FinishedBuildWithChanges buildToShowInCell = buildInWhichIssueFirstOccurred.get(testFailure);
            int colSpan = 0;
            for (FinishedBuildWithChanges build : buildsSuspectedOfCausingIssue.get(testFailure))
                colSpan += build.getChangeDelta().isEmpty() ? 1 : build.getChangeDelta().size();
            int column = buildColumn.get(buildToShowInCell);
            rowOfCells.set(column, new TableCell<FinishedBuildWithChanges>(buildToShowInCell, colSpan));
            for (int i = column + 1; i < column + colSpan; i++) {
                rowOfCells.set(i, null);
            }
            tableCells.add(rowOfCells);
        }
    }

    /**
     * @return the number of rows in the table, excluding the header row(s)
     */
    public int getNumberOfRows() {
        return testFailures.size();
    }

    /**
     * @return the number of columns in the table, excluding the first column(s)
     */
    public int getNumberOfColumns() {
        return buildRow.size();
    }

    /**
     * The cell in the given column to be put to the "build" header row.
     * @param column
     * @return null if the cell should be skipped altogether (presumably because a previous cell has colspan > 1)
     */
    @Nullable
    public TableCell<FinishedBuildWithChanges> getBuildRowCell(int column) {
        return buildRow.get(column);
    }

    /**
     * The cell in the given column to be put to the "change" header row.
     * @param column
     * @return a cell with null content if there was no change corresponding to this column's build
     */
    @NotNull
    public TableCell<SVcsModification> getChangeRowCell(int column) {
        return new TableCell<SVcsModification>(changeRow.get(column), 1);
    }

    /**
     * The cell in the given row to be put to the first ("failures") column.
     * @param row
     * @return
     */
    @NotNull
    public TableCell<TestName> getFailureColumnCell(int row) {
        return new TableCell<TestName>(testFailures.get(row), 1);
    }

    /**
     * The cell at [row, column] in the main part of the table.
     * @param row
     * @param column
     * @return null if the cell should be skipped altogether (presumably because a previous cell has colspan > 1); a
     * cell with null content if this is not a culprit
     */
    @Nullable
    public TableCell<FinishedBuildWithChanges> getCell(int row, int column) {
        return tableCells.get(row).get(column);
    }
}
