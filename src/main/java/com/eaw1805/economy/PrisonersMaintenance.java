package com.eaw1805.economy;

import com.eaw1805.core.GameEngine;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.RelationsManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.NationsRelation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Responsible for processing the maintenance costs of prisoners of war.
 */
public class PrisonersMaintenance
        extends AbstractMaintenance {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER =
            LogManager.getLogger(ArmyMaintenance.class);

    /**
     * Default constructor.
     *
     * @param caller the game engine that invoked us.
     */
    public PrisonersMaintenance(final GameEngine caller) {
        super(caller);
        LOGGER.debug("PrisonersMaintenance instantiated.");
    }

    /**
     * Prisoners of war food.
     * Prisoners consume food at the ratio of 1ton per 2000 men per month.
     * If the country's warehouse runs out of food, then 60-90% of prisoners instantly die.
     */
    @SuppressWarnings("unchecked")
    public void maintainPrisoners() {
        final Transaction theTrans = HibernateUtil.getInstance().beginTransaction(getGame().getScenarioId());
        final List<Nation> activeNations = getGameEngine().getAliveNations();
        for (Nation thisNation : activeNations) {
            final int nation = thisNation.getId();
            if (nation == NATION_NEUTRAL) {
                continue;
            }

            // If the country's warehouse runs out of food, then 60-90% of prisoners instantly die.
            final int lossRate = getRandomGen().nextInt(31) + 60;
            int totPrisoners = 0;

            final List<NationsRelation> allRelations = RelationsManager.getInstance().listByGameNation(getGameEngine().getGame(), thisNation);
            for (final NationsRelation relation : allRelations) {
                totPrisoners += relation.getPrisoners();
            }

            // Prisoners consume food at the ratio of 1ton per 2000 men per month.
            final int reqFood = (int) (totPrisoners / FOOD_RATE_PRI);
            if (getTotGoods(nation, EUROPE, GOOD_FOOD) >= reqFood) {
                // Update totals
                decTotGoods(nation, EUROPE, GOOD_FOOD, reqFood);
                report(thisNation, P_TOT, totPrisoners);
                report(thisNation, P_TOT + P_FOOD, reqFood);

            } else {
                report(thisNation, P_TOT + P_FOOD, getTotGoods(nation, EUROPE, GOOD_FOOD));
                decTotGoods(nation, EUROPE, GOOD_FOOD, 0);
                int remainingPrisoners = 0;

                for (final NationsRelation relation : allRelations) {
                    if (relation.getPrisoners() > 0) {
                        final int prisonerLoss = relation.getPrisoners() * lossRate / 100;
                        relation.setPrisoners(relation.getPrisoners() - prisonerLoss);
                        if (relation.getPrisoners() < 0) {
                            relation.setPrisoners(0);
                        }
                        remainingPrisoners += relation.getPrisoners();

                        RelationsManager.getInstance().update(relation);

                        // The brigade was not maintained properly
                        newsPair(thisNation, relation.getTarget(), NEWS_ECONOMY,
                                "We did not have enough food for the prisoners of " + relation.getTarget().getName() + ". " + prisonerLoss + " prisoners died this month.",
                                thisNation.getName() + " did not have enough food for our soldiers that is keeping as prisoners of war. " + prisonerLoss + " of our soldiers died this month.");

                        report(thisNation, P_TOT + P_NATION + relation.getTarget().getId(), relation.getPrisoners());

                        LOGGER.info("Starving Prisoners [Owner=" + thisNation.getName()
                                + "/Nation=" + relation.getTarget().getName()
                                + "] prisoners starved=" + prisonerLoss + " remaining prisoners=" + relation.getPrisoners());
                    }
                }

                report(thisNation, P_TOT, remainingPrisoners);
            }
        }

        theTrans.commit();

        saveData();

        LOGGER.info("PrisonersMaintenance completed.");
    }
}
