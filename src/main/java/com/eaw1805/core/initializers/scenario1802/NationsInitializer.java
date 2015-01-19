package com.eaw1805.core.initializers.scenario1802;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.model.Nation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Initializes the table with the nation data.
 */
public class NationsInitializer
        extends AbstractThreadedInitializer {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER =
            LogManager.getLogger(NationsInitializer.class);

    /**
     * The codes of the 17 records.
     */
    private static final char[] CODES =
            {'A', 'B', 'D', 'E',
                    'F', 'G', 'H', 'I', 'K',
                    'M', 'N', 'P', 'R', 'S', 'T',
                    'W', 'Y'};

    /**
     * The names of the 17 records.
     */
    private static final String[] NAMES =
            {"Austria-Hungary", "Confederation of the Rhine", "Denmark", "Spain",
                    "France", "Great Britain", "Holland", "Italy", "Kingdom of Portugal",
                    "Morocco", "Naples", "Prussia", "Russia", "Sweden", "Ottoman Empire",
                    "Duchy of Warsaw", "Egypt"};

    /**
     * The taxation rates of the 17 records.
     */
    private static final int[] TAX_RATE =
            {5, 6, 5, 5,
                    5, 7, 6, 6, 6,
                    4, 6, 5, 4, 5, 4,
                    5, 4};

    /**
     * The spheres of the 17 records.
     */
    private static final String[] SPHERES =
            {"B,I,P,R,T,W", "A,D,F,H,I,P", "B,P,S", "F,K,M",
                    "B,E,H,I", "G", "B,F", "A,B,F,N", "E,M",
                    "E,K,7", "I,7", "A,B,D,W", "A,S,T,W,1,2,3", "D,R,1", "A,R,3,4,5",
                    "A,P,R", "4,5,6,7"};

    /**
     * The colors of the 17 records.
     */
    private static final String[] COLORS =
            {"c9c9a7", "262626", "232188", "e5e800",
                    "c1c0ff", "ff0000", "fe5200", "12ffff", "c4ffb9",
                    "086d6e", "be81ff", "0000ff", "116d00", "ffc400", "28ff00",
                    "fa00ff", "575287"};

    /**
     * The morale of the 17 records.
     */
    private static final int[] MORALE =
            {4, 4, 6, 6,
                    7, 8, 7, 4, 6,
                    5, 4, 4, 5, 6, 5,
                    4, 6};

    /**
     * The initial VPs of the 17 records.
     */
    private static final int[] VP_INIT =
            {40, 30, 30, 40,
                    45, 45, 35, 30, 35,
                    35, 30, 35, 45, 35, 35,
                    35, 35};

    /**
     * The winning VPs of the 17 records.
     */
    private static final int[] VP_WINNING =
            {400, 300, 300, 400,
                    450, 450, 350, 300, 350,
                    350, 300, 350, 450, 350, 350,
                    350, 350};

    /**
     * Total number of records.
     */
    public static final int TOTAL_RECORDS = NAMES.length + 1;


    /**
     * Default constructor.
     */
    public NationsInitializer() {
        super();
        LOGGER.debug("NationsInitializer instantiated.");
    }

    /**
     * Checks if the database is properly initialized.
     *
     * @return true if database is not correctly initialized.
     */
    public boolean needsInitialization() {
        final List<Nation> records = NationManager.getInstance().list();
        return (records.size() != TOTAL_RECORDS);
    }

    /**
     * Initializes the database by populating it with the proper records.
     */
    public void initialize() {
        LOGGER.debug("NationsInitializer invoked.");

        // Add free nation
        NationManager.getInstance().addNegativeNationID();

        // Initialize records
        for (int i = 0; i < NAMES.length; i++) {
            final Nation thisNation = new Nation(); //NOPMD
            thisNation.setCode(CODES[i]);
            thisNation.setName(NAMES[i]);
            thisNation.setTaxRate(TAX_RATE[i]);
            thisNation.setSphereOfInfluence(SPHERES[i]);
            thisNation.setColor(COLORS[i]);
            thisNation.setMorale(MORALE[i]);
            thisNation.setCost(10);
            thisNation.setVpInit(VP_INIT[i]);
            thisNation.setVpWin(VP_WINNING[i]);
            NationManager.getInstance().add(thisNation);
        }

        LOGGER.info("NationsInitializer complete.");
    }
}
