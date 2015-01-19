package com.eaw1805.battles.field.visibility;

import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.utils.VisualisationUtils;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleSector;
import com.eaw1805.data.model.battles.field.enumerations.FormationEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FieldBattleVisibilityProcessor {

    private FieldBattleProcessor parent;
    private static final boolean VERBOSE = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(FieldBattleVisibilityProcessor.class);

    public FieldBattleVisibilityProcessor(FieldBattleProcessor parent) {
        this.parent = parent;
    }

    public void setParent(FieldBattleProcessor parent) {
        this.parent = parent;
    }

    public boolean visible(Brigade brigade0, Brigade brigade1) {

        List<FieldBattleSector> lineTopView = Bresenham.findLine(parent.getFbMap().getSectors(),
                brigade0.getFieldBattlePosition().getX(),
                brigade0.getFieldBattlePosition().getY(),
                brigade1.getFieldBattlePosition().getX(),
                brigade1.getFieldBattlePosition().getY());

        int viewerSide = parent.findSide(brigade0);

        // Create grid for line
        Boolean[][] sideVisibilityGrid = createSideVisibilityGrid(lineTopView, viewerSide);

        if (VERBOSE) {
            System.out.println("Vertical view visibility");
            VisualisationUtils.visualize(parent.getFbMap().getSectors(), lineTopView, false);
            System.out.println("Side view visibility");
            printVisibilityGrid(sideVisibilityGrid);
        }

        // altitudes are 1-based
        List<Boolean> lineSideView =
                Bresenham.findLine(sideVisibilityGrid,
                        0,
                        lineTopView.get(0).getAltitude() - 1,
                        sideVisibilityGrid.length - 1,
                        lineTopView.get(lineTopView.size() - 1).getAltitude() - 1);

        if (VERBOSE) {
            System.out.println("Side line-of-sight");
            printLineSideView(lineTopView, viewerSide);
        }

        List<Boolean> lineOfSightExcludingFirstAndLast = lineSideView.subList(1, lineSideView.size() - 1);
        boolean visibilityResult = lineOfSightExcludingFirstAndLast.isEmpty()
                || !lineOfSightExcludingFirstAndLast.contains(Boolean.FALSE);

        LOGGER.debug("Checked visibility from {} to {}: {}", new Object[]{brigade0, brigade1, visibilityResult});
        return visibilityResult;
    }

    public boolean visibleForLongRange(Brigade brigade0, Brigade brigade1) {

        return visible(brigade0, brigade1);
    }

    private Boolean[][] createSideVisibilityGrid(List<FieldBattleSector> line, int viewerSide) {
        Boolean[][] visibilityGrid = new Boolean[line.size()][5];
        FieldBattleSector viewerSector = line.get(0);
        FieldBattleSector targetSector = line.get(line.size() - 1);

        for (int x = 0; x < line.size(); x++) {
            // altitudes are 1-based, so sector altitude should be between 1-5
            int sectorAltitude = calculateAugmentedAltitude(line.get(x), viewerSector, targetSector, viewerSide);
            for (int y = 0; y < 5; y++) {
                visibilityGrid[x][y] = y + 2 > sectorAltitude;
            }
        }
        ;
        return visibilityGrid;
    }

    private int calculateAugmentedAltitude(FieldBattleSector fieldBattleSector,
                                           FieldBattleSector viewerSector,
                                           FieldBattleSector targetSector,
                                           int viewerSide) {

        int altitudeAugmentation = 0;

        if (fieldBattleSector.getWall() > 0
                || fieldBattleSector.getChateau() > 0
                || fieldBattleSector.getTown() > 0
                || fieldBattleSector.getVillage() > 0
                || fieldBattleSector.isForest()
                || containsNonScirmishBrigade(fieldBattleSector)
                ) {
            altitudeAugmentation = 1;
        }

        return fieldBattleSector.getAltitude() + altitudeAugmentation;
    }

    private boolean containsNonScirmishBrigade(FieldBattleSector fieldBattleSector) {
        Brigade brigadeInSector = parent.getBrigadeInSector(fieldBattleSector);
        return brigadeInSector != null
                && brigadeInSector.getFormationEnum() != FormationEnum.SKIRMISH;
    }

    private void printVisibilityGrid(Boolean[][] grid) {

        StringBuffer sb = new StringBuffer();
        // upper border
        for (int x = 0; x < grid.length * 2 + 1; x++) {
            sb.append("+");
        }
        sb.append("\n");

        // content
        for (int y = grid[0].length - 1; y >= 0; y--) {

            sb.append("+");
            for (int x = 0; x < grid.length; x++) {
                if (x != 0) {
                    sb.append("|");
                }
                sb.append(grid[x][y] ? 1 : 0);
            }
            sb.append("+");
            sb.append("\n");
        }

        // lower border
        for (int x = 0; x < grid.length * 2 + 1; x++) {
            sb.append("+");
        }
        sb.append("\n");

        System.out.println(sb.toString());
    }

    private void printLineSideView(List<FieldBattleSector> linePlanView, int viewerSide) {

        Point[][] visibilityGrid = new Point[linePlanView.size()][5];
        for (int x = 0; x < linePlanView.size(); x++) {
            int sectorAltitude = calculateAugmentedAltitude(linePlanView.get(x), linePlanView.get(0),
                    linePlanView.get(linePlanView.size() - 1), viewerSide);
            for (int y = 0; y < 5; y++) {
                visibilityGrid[x][y] = new Point(x, y, y + 2 > sectorAltitude);
            }
        }
        ;

        List<Point> lineSideView =
                Bresenham.findLine(visibilityGrid,
                        0,
                        linePlanView.get(0).getAltitude() - 1,
                        visibilityGrid.length - 1,
                        linePlanView.get(linePlanView.size() - 1).getAltitude() - 1);

        VisualisationUtils.visualize(visibilityGrid, lineSideView, true);

    }

    @SuppressWarnings("unused")
    private static class Point {
        private final int x;
        private final int y;
        private final boolean visibility;

        public Point(int x, int y, boolean visibility) {
            this.x = x;
            this.y = y;
            this.visibility = visibility;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public boolean isVisibility() {
            return visibility;
        }


    }

}
