package com.eaw1805.battles.field.test;

import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.morale.MoraleChecker;
import com.eaw1805.battles.field.processors.commander.CommanderProcessor;
import com.eaw1805.battles.field.processors.commander.CommanderType;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.enumerations.MoraleStatusEnum;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MoraleCheckerTest {

    private MoraleChecker moraleChecker = new MoraleChecker(null);
    private static final Logger LOGGER = LoggerFactory.getLogger(MoraleCheckerTest.class);
    private CommanderProcessor commanderProcessorMock;

    private FieldBattleProcessor fbProcessor = new FieldBattleProcessor(false, HibernateUtil.DB_S1);
    private List<Brigade> side0;
    private List<Brigade> side1;

    @Before
    public void setup() {

        side0 = new ArrayList<Brigade>();
        side1 = new ArrayList<Brigade>();

        commanderProcessorMock = mock(CommanderProcessor.class);
        when(commanderProcessorMock.influencedByCommander(any(Brigade.class))).thenReturn(false);
        when(commanderProcessorMock.influencedByCommanderOfType(any(Brigade.class), any(CommanderType.class))).thenReturn(false);

        @SuppressWarnings("unchecked")
        List<Brigade>[] sideBrigadesArray = (List<Brigade>[]) Array.newInstance(List.class, 2);
        sideBrigadesArray[0] = side0;
        sideBrigadesArray[1] = side1;
        fbProcessor.setSideBrigades(sideBrigadesArray);
        fbProcessor.setInitialSideBrigades(sideBrigadesArray);
    }

    @Test
    public void testCalculateBrigadeMorale() {
        Brigade brigade = new Brigade();
        brigade.setBattalions(new HashSet<Battalion>());

        Battalion battalion1 = new Battalion();
        brigade.getBattalions().add(battalion1);
        Battalion battalion2 = new Battalion();
        brigade.getBattalions().add(battalion2);
        Battalion battalion3 = new Battalion();
        brigade.getBattalions().add(battalion3);

        battalion1.setHeadcount(100);
        battalion1.setExperience(5);
        battalion2.setHeadcount(100);
        battalion2.setExperience(5);
        battalion3.setHeadcount(100);
        battalion3.setExperience(5);
        assertSame(moraleChecker.calculateBrigadeMorale(brigade), 5);

        battalion1.setHeadcount(300);
        battalion1.setExperience(8);
        battalion2.setHeadcount(100);
        battalion2.setExperience(5);
        battalion3.setHeadcount(300);
        battalion3.setExperience(2);
        assertSame(moraleChecker.calculateBrigadeMorale(brigade), 5);

        battalion1.setHeadcount(50);
        battalion1.setExperience(7);
        battalion2.setHeadcount(150);
        battalion2.setExperience(5);
        battalion3.setHeadcount(300);
        battalion3.setExperience(4);
        assertSame(moraleChecker.calculateBrigadeMorale(brigade), 5);

    }

    @Test
    public void testCalculateSideMorale() {

        Brigade brigade0 = createBrigade(0, 6, 300, 4);    // 7200
        Brigade brigade1 = createBrigade(1, 5, 600, 5);    // 15000
        Brigade brigade2 = createBrigade(2, 2, 800, 8);    // 12800
        side0.add(brigade0);
        side0.add(brigade1);
        side0.add(brigade2);

        Brigade brigade4 = createBrigade(4, 2, 700, 6);
        Brigade brigade5 = createBrigade(5, 6, 200, 4);
        Brigade brigade6 = createBrigade(6, 4, 500, 2);
        side1.add(brigade4);
        side1.add(brigade5);
        side1.add(brigade6);

        fbProcessor.setCommanderProcessor(commanderProcessorMock);

        moraleChecker = new MoraleChecker(fbProcessor);

        moraleChecker.calculateInitialSideMorale(0);
        moraleChecker.calculateInitialSideMorale(1);

        assertSame(moraleChecker.calculateBrigadeMorale(brigade0), 4);
        assertSame(moraleChecker.calculateBrigadeMorale(brigade1), 5);
        assertSame(moraleChecker.calculateBrigadeMorale(brigade2), 8);

        assertSame(21, (int) Math.round(moraleChecker.getBrigadeMoraleRelativeContributions().get(brigade0.getBrigadeId())));
        assertSame(43, (int) Math.round(moraleChecker.getBrigadeMoraleRelativeContributions().get(brigade1.getBrigadeId())));
        assertSame(37, (int) Math.round(moraleChecker.getBrigadeMoraleRelativeContributions().get(brigade2.getBrigadeId())));

        assertSame(6, moraleChecker.calculateBrigadeMorale(brigade4));
        assertSame(4, moraleChecker.calculateBrigadeMorale(brigade5));
        assertSame(2, moraleChecker.calculateBrigadeMorale(brigade6));

        assertSame(49, (int) Math.round(moraleChecker.getBrigadeMoraleRelativeContributions().get(brigade4.getBrigadeId())));
        assertSame(28, (int) Math.round(moraleChecker.getBrigadeMoraleRelativeContributions().get(brigade5.getBrigadeId())));
        assertSame(23, (int) Math.round(moraleChecker.getBrigadeMoraleRelativeContributions().get(brigade6.getBrigadeId())));
    }

    @Test
    public void testMoraleCheck() {

        LOGGER.debug("Brigade{} has {} battalions of {} men with experience {}", new Object[]{0, 6, 300, 4});
        Brigade brigade0 = createBrigade(0, 6, 300, 4);    // 7200
        LOGGER.debug("Brigade{} has {} battalions of {} men with experience {}", new Object[]{1, 5, 600, 5});
        Brigade brigade1 = createBrigade(1, 5, 600, 5);    // 15000
        LOGGER.debug("Brigade{} has {} battalions of {} men with experience {}", new Object[]{2, 2, 800, 8});
        Brigade brigade2 = createBrigade(2, 2, 800, 8);    // 12800
        side0.add(brigade0);
        side0.add(brigade1);
        side0.add(brigade2);

        Brigade brigade4 = createBrigade(4, 2, 700, 6);
        Brigade brigade5 = createBrigade(5, 6, 200, 3);
        Brigade brigade6 = createBrigade(6, 4, 500, 2);
        side1.add(brigade4);
        side1.add(brigade5);
        side1.add(brigade6);
        fbProcessor.setCommanderProcessor(commanderProcessorMock);

        moraleChecker = new MoraleChecker(fbProcessor);

        moraleChecker.calculateInitialSideMoralesIfRequired();

        LOGGER.debug("Brigade{}: The relative morale contribution is {}", new Object[]{0, moraleChecker.getBrigadeMoraleRelativeContributions().get(brigade0)});
        LOGGER.debug("Brigade{}: The relative morale contribution is {}", new Object[]{1, moraleChecker.getBrigadeMoraleRelativeContributions().get(brigade1)});
        LOGGER.debug("Brigade{}: The relative morale contribution is {}", new Object[]{2, moraleChecker.getBrigadeMoraleRelativeContributions().get(brigade2)});


        LOGGER.debug("Without legendary commander present, Brigade{} has morale threshold {}", new Object[]{0, 80});
        assertSame(80, moraleChecker.calculateMoraleThreshold(brigade0));
        LOGGER.debug("Without legendary commander present, Brigade{} has morale threshold {}", new Object[]{1, 85});
        assertSame(85, moraleChecker.calculateMoraleThreshold(brigade1));
        LOGGER.debug("Without legendary commander present, Brigade{} has morale threshold {}", new Object[]{2, 100});
        assertSame(100, moraleChecker.calculateMoraleThreshold(brigade2));

        // The above thresholds increase when there is a legendary commander for this side
        when(commanderProcessorMock.sideHasAliveCommanderOfType(anyInt(), any(CommanderType.class))).thenReturn(true);

        LOGGER.debug("With legendary commander present, Brigade{} has morale threshold {}", new Object[]{0, 82});
        assertSame(82, moraleChecker.calculateMoraleThreshold(brigade0));
        LOGGER.debug("With legendary commander present, Brigade{} has morale threshold {}", new Object[]{1, 87});
        assertSame(87, moraleChecker.calculateMoraleThreshold(brigade1));
        LOGGER.debug("With legendary commander present, Brigade{} has morale threshold {}", new Object[]{2, 102});
        assertSame(102, moraleChecker.calculateMoraleThreshold(brigade2));
        when(commanderProcessorMock.sideHasAliveCommanderOfType(anyInt(), any(CommanderType.class))).thenReturn(false);

        // the more brigades are routing, the lower the threshold becomes
        brigade1.setMoraleStatusEnum(MoraleStatusEnum.ROUTING);
        LOGGER.debug("Without legendary commander present, if Brigade1 is routing, Brigade0 has morale threshold {}", 59);
        assertSame(59, moraleChecker.calculateMoraleThreshold(brigade0));
        brigade2.setMoraleStatusEnum(MoraleStatusEnum.ROUTING);
        LOGGER.debug("Without legendary commander present, if Brigade1 and Brigade2 are routing, Brigade0 has morale threshold {}", 41);
        assertSame(41, moraleChecker.calculateMoraleThreshold(brigade0));
        brigade0.setMoraleStatusEnum(MoraleStatusEnum.ROUTING);
        LOGGER.debug("Without legendary commander present, if Brigade1, Brigade2 and Brigade0 are routing, Brigade0 has morale threshold {}", 30);
        assertSame(30, moraleChecker.calculateMoraleThreshold(brigade0));


        assertSame(90, moraleChecker.calculateMoraleThreshold(brigade4));
        assertSame(75, moraleChecker.calculateMoraleThreshold(brigade5));
        assertSame(70, moraleChecker.calculateMoraleThreshold(brigade6));

    }

    @Test
    public void exhaustiveMoralePrintout() {


        for (int example = 0; example <= 10; example++) {

            int brigadeNum = MathUtils.generateRandomIntInRange(5, 50);

            LOGGER.debug("*************** EXAMPLE #{} ({} brigades) ***************", new Object[]{example, brigadeNum});

            for (int i = 0; i <= brigadeNum; i++) {

                int battalionNumber = MathUtils.generateRandomIntInRange(1, 6);
                int battalionHeadcount = MathUtils.generateRandomIntInRange(100, 600);
                int experience = MathUtils.generateRandomIntInRange(1, 10);

                Brigade brigade = createBrigade(0, battalionNumber, battalionHeadcount, experience);
                side0.add(brigade);
                LOGGER.debug("Brigade{} has {} battalions of {} men with experience {}", new Object[]{i, battalionNumber, battalionHeadcount, experience});
            }

            fbProcessor.setCommanderProcessor(commanderProcessorMock);

            moraleChecker = new MoraleChecker(fbProcessor);

            moraleChecker.calculateInitialSideMorale(0);

            for (int i = 0; i <= brigadeNum; i++) {
                Brigade brigade = side0.get(i);
                LOGGER.debug("Brigade{}: morale threshold if noone is running is {}", new Object[]{i, moraleChecker.calculateMoraleThreshold(brigade)});
            }

            for (int i = 0; i <= brigadeNum; i++) {
                Brigade brigade = side0.get(i);
                LOGGER.debug("Brigade{}: The relative morale contribution is {}", new Object[]{i, moraleChecker.getBrigadeMoraleRelativeContributions().get(brigade)});
            }

            Brigade brigade0 = side0.get(0);
            LOGGER.debug("If noone is routing, brigade0 has morale threshold {}", new Object[]{moraleChecker.calculateMoraleThreshold(brigade0)});
            side0.get(1).setMoraleStatusEnum(MoraleStatusEnum.ROUTING);
            LOGGER.debug("If the brigades 1 is routing, brigade0 has morale threshold {}", new Object[]{moraleChecker.calculateMoraleThreshold(brigade0)});
            for (int i = 2; i <= brigadeNum; i++) {
                side0.get(i).setMoraleStatusEnum(MoraleStatusEnum.ROUTING);
                LOGGER.debug("If the brigades 1-{} are routing, brigade0 has morale threshold {}", new Object[]{i, moraleChecker.calculateMoraleThreshold(brigade0)});
            }

            LOGGER.debug("\n\n\n\n\n\n\n\n\n");
        }

    }


    private Brigade createBrigade(int brigadeId, int battalionCount, int headcount, int experience) {
        Brigade brigade = new Brigade();
        brigade.setBrigadeId(brigadeId);
        brigade.setBattalions(new HashSet<Battalion>());
        for (int i = 0; i < battalionCount; i++) {
            Battalion battalion = new Battalion();
            battalion.setExperience(experience);
            battalion.setHeadcount(headcount);
            brigade.getBattalions().add(battalion);
        }
        brigade.setMoraleStatusEnum(MoraleStatusEnum.NORMAL);
        return brigade;
    }

}
