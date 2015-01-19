package com.eaw1805.core.initializers.scenario1802.map;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.data.managers.map.ProductionSiteManager;
import com.eaw1805.data.model.map.ProductionSite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.StringTokenizer;

/**
 * Initializes the production sites.
 */
public class ProductionSiteInitializer
        extends AbstractThreadedInitializer {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(ProductionSiteInitializer.class);

    /**
     * The date of the 17 records.
     * "Production Site|Cost|Terrain|Symbol|Maintenance|PopulationMin|PopulationMax|AttritionMin|AttritionMax|Description",
     */
    private static final String[] DATA = {
            "Estate|100000|B|@|10000|1|3|1|3|Estates produce the necessary food for feeding troops and population. Estates can only be built on arable land with population density of 1 to 3. Each farm produces between 8 to 280 units of food per month, depending on the climatic zone, the time of the year and a random factor. If an estate is situated on a special food resource location it receives a production bonus.",
            "Factory|500000|B, H|*|50000|4|9|4|8|Factories produce industrial points which is a vital resource needed for raising and maintaining military forces. Factories can be built on arable land or hills with population density of 4 to 9. Each factory uses a quantity of ore, fabrics and wood to produce 1000 - 3000 industrial points per month, if the base materials are available. If the resources are not available, then the factory ceases production.",
            "Horse Farm|250000|B, H, Q, K|=|10000|1|3|1|2|Horse Farms breed horses needed to raise cavalry units as well as building baggage trains. Horse Breeding farms can be built on arable land or hills with population density of 1 to 3. They produce 200-400 horses per month. If a horse farm is situated on a special horse resource location it receives a production bonus.",
            "Lumbercamp|200000|W, T, J|%|7500|1|3|2|6|Lumbercamps produce timber needed for building ships, raising population as well as for the production of industrial points. Lumbercamps can only be built at forest and jungle  tiles with population density of 1 to 3. They produce 200-400 units of wood per month.",
            "Mine|600000|B, H, K, Q, G, W, T, D|^|60000|1|3|4|12|mines extract ore and precious metals from specific locations on the map. Ore is needed for producing Industrial points, while precious metals may be minted to produce money. Mines can only be built at locations that have the appropriate special resource (ore, gems or precious metals). The production of mines varies depending on its location (Europe or the Colonies) and the type of resource produced.",
            "Mint|250000|B, H|~|25000|4|9|0|1|Mints convert precious metals into money. Mints can only be built in Europe, on arable land or hills with population density of 4 to 9. They mint 400.000-800,000 worth of precious metals per month, using the respective quantity of resource.",
            "Plantation|450000|B, H, K, Q, G, W, T, D, S|<|40000|2|6|3|9|Plantations extract special resources from specified locations on the map. They can only be built in the colonies. Plantations can only be built at locations with the appropriate special resource (colonial goods) and the population density of the area must be between 1 to 3. Each plantation produces between 90 to 150 tons of colonial goods per month.",
            "Quarry|100000|G|/|5000|1|2|4|12|Quarries produce stone needed for raising population and building fortresses. Quarries can only be built at mountains with population density of 1 to 2. Each quarry produces 500 - 900 tons of stone per month.",
            "Sheep Farm|150000|B, H, Q, K|:|10000|1|3|1|2|Sheep farms produce wool needed for the production of fabric. Sheep farms can be built on arable land or hills with population density of 1 to 3. Each sheep farm produces 30 - 70 units of wool per month. If a sheep farm is situated on a special wool resource location it receives a production bonus.",
            "Vineyard|400000|B, H, K, Q, G, W, T, D|!|40000|1|3|1|2|Vineyards produce wine on specified locations on the map. Wine is needed for keeping marines happy. Vineyards can only be built at location with the appropriate special resource (wine) and the population density of the area must be between 1 to 4. Each vineyard produces 20 - 40 units of wine per month but only during September to December.",
            "Weaving Mill|250000|B, H, Q, K|#|35000|3|9|2|3|Weaving mills produce fabric needed for building ships sails and producing industrial points. Mills can be built on arable land or hills with population density of 3 to 9. They consume wool in order to produce 50 - 200 units of fabric per month. ",
            "Barrack|500000|B, H, K, Q, G, W, T, D, S, J|&|25000|0|9|0|0|Barracks do not produce goods, but allow operations related to military forces management as well as transferring of goods between the warehouses and trains or ships. They also provide supply to troops in their vicinity. Barracks can be built on any terrain regardless of population. If a barrack is situated on a tile adjacent to the sea, then it also functions as a port and shipyard.",
            "Small Fortified Barrack|350000|B, H, K, Q, G, W, T, D, S, J|$|30000|0|9|0|0|Fortresses provide a defensive bonus. No overrun takes place on a fortress and the casualties inflicted are reduced by up to 70%. Barracks can be built on any terrain regardless of population. If a barrack is situated on a tile adjacent to the sea, then it also functions as a port and shipyard.",
            "Medium Fortified Barrack|750000|B, H, K, Q, G, W, T, D, S, J|7|40000|0|9|0|0|Fortresses provide a defensive bonus. No overrun takes place on a fortress and the casualties inflicted are reduced by up to 70%. Barracks can be built on any terrain regardless of population. If a barrack is situated on a tile adjacent to the sea, then it also functions as a port and shipyard.",
            "Large Fortified Barrack|2000000|B, H, K, Q, G, W, T, D, S, J|8|50000|0|9|0|0|Fortresses provide a defensive bonus. No overrun takes place on a fortress and the casualties inflicted are reduced by up to 70%. Barracks can be built on any terrain regardless of population. If a barrack is situated on a tile adjacent to the sea, then it also functions as a port and shipyard.",
            "Huge Fortified Barrack|9000000|B, H, K, Q, G, W, T, D, S, J|9|75000|0|9|0|0|Fortresses provide a defensive bonus. No overrun takes place on a fortress and the casualties inflicted are reduced by up to 70%. Barracks can be built on any terrain regardless of population. If a barrack is situated on a tile adjacent to the sea, then it also functions as a port and shipyard."
    };

    /**
     * Total number of records.
     */
    public static final int TOTAL_RECORDS = DATA.length;

    /**
     * Default constructor.
     */
    public ProductionSiteInitializer() {
        super();
        LOGGER.debug("ProductionSiteInitializer instantiated.");
    }

    /**
     * Checks if the database is properly initialized.
     *
     * @return true if database is not correctly initialized.
     */
    public boolean needsInitialization() {
        final List<ProductionSite> records = ProductionSiteManager.getInstance().list();
        return (records.size() != TOTAL_RECORDS);
    }

    /**
     * Initializes the database by populating it with the proper records.
     */
    public void initialize() {
        LOGGER.debug("ProductionSiteInitializer invoked.");

        // Initialize records
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            final ProductionSite thisProductionSite = new ProductionSite(); //NOPMD
            final StringTokenizer thisStk = new StringTokenizer(DATA[i], "|"); // NOPMD

            thisProductionSite.setName(thisStk.nextToken());
            thisProductionSite.setCost(Integer.parseInt(thisStk.nextToken()));
            thisProductionSite.setTerrainsSuitable(thisStk.nextToken());
            thisProductionSite.setCode(thisStk.nextToken().charAt(0));
            thisProductionSite.setMaintenanceCost(Integer.parseInt(thisStk.nextToken()));
            thisProductionSite.setMinPopDensity(Integer.parseInt(thisStk.nextToken()));
            thisProductionSite.setMaxPopDensity(Integer.parseInt(thisStk.nextToken()));
            thisProductionSite.setAttritionMin(Integer.parseInt(thisStk.nextToken()));
            thisProductionSite.setAttritionMax(Integer.parseInt(thisStk.nextToken()));
            thisProductionSite.setDescription(thisStk.nextToken());
            ProductionSiteManager.getInstance().add(thisProductionSite);
        }

        LOGGER.info("ProductionSiteInitializer complete.");
    }

}

