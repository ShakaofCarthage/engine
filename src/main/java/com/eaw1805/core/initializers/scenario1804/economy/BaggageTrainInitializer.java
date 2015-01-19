package com.eaw1805.core.initializers.scenario1804.economy;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.core.initializers.scenario1802.NationsInitializer;
import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.economy.BaggageTrainManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.economy.BaggageTrain;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Initializes BaggageTrain objects.
 */
public class BaggageTrainInitializer
        extends AbstractThreadedInitializer
        implements GoodConstants {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(BaggageTrainInitializer.class);

    /**
     * Total number of records.
     */
    public static final int TOTAL_RECORDS = NationsInitializer.TOTAL_RECORDS * 3;

    /**
     * The total baggage trains per region.
     */
    private static final int[] TOT_TRAINS = {2, 1, 1, 1};

    /**
     * Default constructor.
     */
    public BaggageTrainInitializer() {
        super();
        LOGGER.debug("BaggageTrainInitializer instantiated.");
    }

    /**
     * Checks if the database is properly initialized.
     *
     * @return true if database is not correctly initialized.
     */
    public boolean needsInitialization() {
        final List<BaggageTrain> records = BaggageTrainManager.getInstance().list();
        return (records.size() != TOTAL_RECORDS);
    }

    /**
     * Initializes the database by populating it with the proper records.
     */
    public void initialize() {
        LOGGER.debug("BaggageTrainInitializer invoked.");

        final Game game = GameManager.getInstance().getByID(-1);
        final List<Region> lstRegion = RegionManager.getInstance().list();
        final List<Nation> lstNations = new ArrayList<Nation>();

        // add single nation
        lstNations.add(NationManager.getInstance().getByID(NationConstants.NATION_FRANCE));

        // iterate through all records
        for (final Nation thisNation : lstNations) {
            int totTrains = 1;
            // iterate through all regions
            for (final Region region : lstRegion) {
                final List<Sector> lstTradeCity = SectorManager.getInstance().listByGameRegionNation(game, region, thisNation, true);
                if (lstTradeCity.size() > 0) {
                    // Since the nation has economy city in this region, setup spies
                    for (int i = 0; i < TOT_TRAINS[region.getId() - 1]; i++) {
                        final BaggageTrain thisBaggageTrain = new BaggageTrain(); //NOPMD
                        thisBaggageTrain.setNation(thisNation);
                        thisBaggageTrain.setName("BaggageTrain " + totTrains++);
                        thisBaggageTrain.setCondition(100);
                        thisBaggageTrain.setPosition((Position) lstTradeCity.get(0).getPosition().clone());

                        final Map<Integer, Integer> qteGoods = new HashMap<Integer, Integer>(); // NOPMD
                        for (int goodID = GOOD_FIRST; goodID <= GOOD_COLONIAL; goodID++) {
                            qteGoods.put(goodID, 0);
                        }
                        thisBaggageTrain.setStoredGoods(qteGoods);

                        BaggageTrainManager.getInstance().add(thisBaggageTrain);
                    }
                }
            }
        }

        LOGGER.info("BaggageTrainInitializer complete.");
    }

}