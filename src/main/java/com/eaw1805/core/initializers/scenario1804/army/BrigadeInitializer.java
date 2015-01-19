package com.eaw1805.core.initializers.scenario1804.army;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.managers.GameManager;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.army.ArmyTypeManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.model.Game;
import com.eaw1805.data.model.army.ArmyType;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattlePosition;
import com.eaw1805.data.model.map.CarrierInfo;
import com.eaw1805.data.model.map.Position;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Initializes the army lists of the scenario.
 */
public class BrigadeInitializer
        extends AbstractThreadedInitializer {

    /**
     * Total number of battalions that a brigade can hold.
     */
    public static final int TOT_BATT = 6;

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(BrigadeInitializer.class);

    /**
     * "Nation ID,Name,Region,Position,MP,Corp,
     * "Nation ID,Name,Region,Position,MP,Federation,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man",
     * "Nation ID,Name,Region,X,Y,Bat,Bat,Bat,Bat,Bat,Bat",
     */
    private static final String[] DATA = { // NOPMD
            "1,CAVALRY BRIG. 1,1,41,29,Cu,Cu,Cu,Cu,Hu,Ma",
            "1,CAVALRY BRIG. 2,1,41,29,Cu,Cu,Cu,Cu,Hu,Ma",
            "1,BRIGADE 1,1,41,29,GR,GR,GR,GR,Pi,La",
            "1,BRIGADE 2,1,41,29,GR,GR,GR,GR,Pi,La",
            "1,BRIGADE 3,1,41,29,GR,GR,GR,GR,Pi,La",
            "1,CAVALRY BRIG. 3,1,41,29,Ch,Ch,Uh,Uh,Uh,Uh",
            "1,CAVALRY BRIG. 4,1,41,29,Dr,Dr,Ma,Ma,Ma,Ma",
            "1,BRIGADE 4,1,41,29,GR,Fu,Fu,Jg,La,La",
            "1,HUNGARY BRIG. 1,1,41,29,GR,GR,Fu,Fu,Jg,Rm",
            "1,HUNGARY BRIG. 2,1,41,29,GR,GR,Fu,Fu,Jg,Rm",
            "1,HUNGARY BRIG. 3,1,41,29,GR,GR,Fu,Fu,Jg,Rm",
            "1,HUNGARY BRIG. 4,1,41,29,GR,GR,Fu,Fu,Jg,Rm",
            "2,CAVALRY BRIG. 1,1,35,27,Cu,Cu,GC,GC,Ma,Ma",
            "2,CAVALRY BRIG. 2,1,35,27,Cu,Cu,GC,GC,Ma,Ma",
            "2,BRIGADE 1,1,35,27,GR,Fu,Fu,Fu,Rm,Pi",
            "2,BRIGADE 2,1,35,27,GR,Fu,Fu,Fu,Rm,Pi",
            "2,BRIGADE 3,1,35,27,GR,Fu,Fu,Fu,Rm,Pi",
            "2,BRIGADE 4,1,35,27,GR,GR,Fu,Fu,Rm,Pi",
            "2,BRIGADE 5,1,35,27,GR,GR,Fu,Fu,La,La",
            "2,BRIGADE 6,1,35,27,GR,GR,GR,GR,La,La",
            "2,CAVALRY BRIG. 3,1,33,19,Cu,Cu,GC,GC,Ma,Ma",
            "2,CAVALRY BRIG. 4,1,33,19,Cu,Cu,GC,GC,Ma,Ma",
            "2,LIGHT CAVALRY BRIG.,1,33,19,Hu,Hu,Hu,Hu,Rc,Rc",
            "2,BRIGADE 7,1,33,19,GR,Fu,Fu,Fu,Rm,Pi",
            "2,BRIGADE 8,1,33,19,GR,Fu,Fu,Fu,Rm,Pi",
            "2,BRIGADE 9,1,33,19,GR,GR,Fu,Fu,La,La",
            "2,BRIGADE 10,1,33,19,GR,Fu,Fu,Fu,La,La",
            "2,BRIGADE 11,1,33,19,GR,GR,GR,GR,La,La",
            "2,BRIGADE 12,1,33,19,GR,GR,Fu,Fu,La,La",
            "2,MARINE-BRIG 1,1,35,27,Kt,Kt,Kt,Kt,Kt,--",
            "3,BRIGADE 1,1,37,16,GR,GR,GR,Rm,Rm,La",
            "3,BRIGADE 2,1,37,16,GR,GR,Ln,Ln,Li,La",
            "3,BRIGADE 3,1,37,16,GR,GR,Ln,Ln,Li,La",
            "3,BRIGADE 4,1,37,16,GR,GR,Ln,Ln,Li,La",
            "3,BRIGADE 5,1,37,16,Ln,Ln,Pi,Pi,La,La",
            "3,DRAGOON BRIG,1,37,16,Dr,Dr,Dr,Dr,Ma,Ma",
            "3,GARDE CAVALRY BRIG,1,37,16,Kg,Kg,Kg,Kg,Ma,Ma",
            "3,MARINE-BRIG,2,35,14,Kt,Kt,Kt,Kt,Ca,Ca",
            "3,COLONIAL-BRIG,2,35,14,Ca,Kt,Ca,Mc,Mc,--",
            "4,BRIGADE 1,1,13,41,Cu,Cu,Dr,Dr,Ma,Ma",
            "4,BRIGADE 2,1,13,41,Cu,Cu,Dr,Dr,Hu,Hu",
            "4,BRIGADE 3,1,13,41,GR,Ln,Ln,Ln,Li,Cz",
            "4,BRIGADE 4,1,13,41,GR,Ln,Ln,Ln,Li,Cz",
            "4,BRIGADE 5,1,13,41,GR,GR,Pi,Pi,Pi,Pi",
            "4,BRIGADE 6,1,13,41,Cu,Cu,Uh,Uh,Dr,Hu",
            "4,BRIGADE 7,1,13,41,GR,Ln,Ln,Ln,Li,La",
            "4,BRIGADE 8,1,13,41,GR,Ln,Ln,Ln,Li,La",
            "4,BRIGADE 9,1,13,41,GR,Ln,Ln,Ln,La,Pi",
            "4,BRIGADE 10,1,13,41,GR,Ln,Ln,Ln,La,Pi",
            "4,MARINE-BRIG,2,26,23,Kt,Kt,Kt,CD,CD,Ca",
            "4,COLONIAL-BRIG,2,26,23,Ca,Ca,Ca,Ca,Ca,Ca",
            "4,COLONIAL-BRIG,2,26,23,Mc,Mc,Mc,Mc,Mc,--",
            "4,MARINE-BRIG,2,09,15,Kt,Kt,Kt,Ca,Ca,Ca",
            "4,COLONIAL-BRIG,2,09,15,Ca,Ca,Ca,Mc,Mc,Mc",
            "4,COLONIAL-BRIG,2,09,15,Ca,Ca,Mc,Mc,Mc,--",
            "4,MARINE-BRIG,2,22,11,Kt,Kt,Ca,Ca,Ca,Mc",
            "5,GARDE IMPERIALE,1,23,27,IG,IG,IG,IG,IG,La",
            "5,GARDE IMPERIALE,1,23,27,IG,IG,IG,IG,IG,La",
            "5,GARDE IMPERIALE,1,23,27,GT,GT,GT,GT,La,La",
            "5,GARDE IMPERIALE,1,23,27,GT,GT,GT,GT,La,La",
            "5,BRIGADE 1,1,23,27,Gr,Gr,Ti,Ti,Ln,Pi",
            "5,BRIGADE 2,1,23,27,Gr,Gr,Ti,Ti,Ln,Pi",
            "5,CAVALRY BRIG. 1,1,23,27,Cr,Cr,Hu,Hu,Ma,Ma",
            "5,CAVALRY BRIG. 2,1,27,36,Cr,Cr,Cr,Cr,Hu,Hu",
            "5,CAVALRY BRIG. 3,1,27,36,Dr,Dr,Cl,Cl,Cl,Cl",
            "5,BRIGADE 3,1,27,36,Gr,Gr,Ti,Ti,Ln,Pi",
            "5,BRIGADE 4,1,27,36,Gr,Gr,Ti,Ti,Ln,Pi",
            "5,BRIGADE 5,1,27,36,Gr,Gr,Vo,Vo,La,La",
            "5,MARINE-BRIG,2,13,4,Kt,Kt,Kt,Ca,Ca,Ca",
            "5,COLONIAL-BRIG,2,13,4,Ca,Ca,Mc,Mc,Mc,Mc",
            "5,COLONIAL-BRIG,2,28,12,Ca,Ca,Mc,Mc,Mc,Mc",
            "6,ROYAL GUARDS 1,1,23,20,Fg,Fg,Fg,Fg,Rm,La",
            "6,ROYAL GUARDS 2,1,23,20,Fg,Fg,Fg,Fg,Rm,La",
            "6,BRIGADE 1,1,23,20,Be,Be,Ln,Ln,La,La",
            "6,BRIGADE 2,1,23,20,KL,KL,KL,KL,Pi,Mi",
            "6,BRIGADE 3 ,1,23,20,Hi,Hi,Hi,Ln,Ln,Pi",
            "6,BRIGADE 4,1,23,20,Lg,Lg,Dr,Dr,Ma,Ma",
            "6,LIGHT CAVALRY BRIG.,1,13,19,CB,CB,lD,lD,lD,lD",
            "6,DUBLIN GARRISON,1,13,19,Be,Ln,Ln,Ln,Pi,Pi",
            "6,MARINE-BRIG,2,24,8,Kt,Kt,Kt,Ca,Ca,Ca",
            "6,COLONIAL-BRIG,2,24,8,Ca,Ca,Ca,Mc,Mc,Mc",
            "7,BRIGADE 1,1,28,20,Cu,Cu,Gc,Gc,Gc,Ma",
            "7,BRIGADE 2,1,28,20,Cu,Cu,Gc,Gc,Gc,Ma",
            "7,BRIGADE 3,1,28,20,Hu,Hu,Uh,Uh,Ma,Ma",
            "7,BRIGADE 4,1,28,20,Gg,Fu,Fu,Fu,Rm,La",
            "7,BRIGADE 5,1,28,20,Gg,Fu,Fu,Fu,Rm,La",
            "7,BRIGADE 6,1,28,20,GR,GR,Fu,Fu,Rm,Rm",
            "7,BRIGADE 7,1,28,20,GR,GR,Fu,Fu,Rm,Rm",
            "7,BRIGADE 8,1,30,27,Dr,Dr,Hu,Hu,Hu,Hu",
            "7,BRIGADE 9,1,30,27,GR,Fu,Fu,Rm,Pi,Pi",
            "7,BRIGADE 10,1,30,27,GR,Fu,Fu,Rm,Pi,Pi",
            "7,BRIGADE 11,1,30,27,GR,Fu,Fu,Rm,La,La",
            "7,BRIGADE 12,1,30,27,GR,Fu,Fu,Rm,La,La",
            "7,BRIGADE 13,1,30,27,Dr,Dr,Hu,Hu,Hu,Hu",
            "7,MARINE-BRIG,2,34,23,Kt,Kt,Kt,Ca,Ca,Ca",
            "7,COLONIAL-BRIG,2,34,23,Kt,Ca,Ca,Mc,Mc,Mc",
            "7,MARINE-BRIG,2,34,23,Kt,Ca,Ca,Mc,Mc,--",
            "8,BRIGADE 1,1,31,32,GR,GR,Fu,Uh,Uh,Ma",
            "8,BRIGADE 2,1,31,32,GR,GR,Fu,Uh,Uh,Ma",
            "8,BRIGADE 3,1,31,32,GR,Rm,Fu,Fu,Pi,Pi",
            "8,BRIGADE 4,1,31,32,GR,Rm,Fu,Fu,Pi,Pi",
            "8,BRIGADE 5,1,31,32,GR,Rm,Fu,Fu,La,La",
            "8,BRIGADE 6,1,35,40,Cu,Cu,GC,GC,GC,Ma",
            "8,BRIGADE 7,1,35,40,Cu,Cu,GC,GC,GC,Ma",
            "8,BRIGADE 8,1,35,40,GR,Fu,Fu,Fu,Rm,La",
            "8,BRIGADE 9,1,35,40,GR,Fu,Fu,Fu,Rm,La",
            "8,BRIGADE 10,1,35,40,Gg,Gg,GR,GR,Ha,Ha",
            "8,BRIGADE 11,1,35,40,GR,GR,Fu,Fu,La,La",
            "8,BRIGADE 12,1,35,40,Fu,Fu,Fu,Fu,Fu,La",
            "8,BRIGADE 13,1,35,40,Rm,Rm,Rm,Fu,Fu,La",
            "8,BRIGADE 14,1,35,40,GC,GC,Dr,Dr,Dr,Dr",
            "8,MARINE-BRIG,2,30,20,Kt,Kt,Kt,Ca,Ca,--",
            "8,COLONIAL-BRIG,2,30,20,Ca,Kt,Mc,Mc,Mc,--",
            "9,MARINE-BRIG,2,40,27,Kt,Kt,Kt,Ca,Ca,Ca",
            "9,COLONIAL-BRIG,2,40,27,Ca,Ca,Ca,Mc,Mc,Mc",
            "11,BRIGADE 1,1,38,43,Cu,Cu,Ch,Ch,Ch,Ch",
            "11,BRIGADE 2,1,38,43,Cu,Cu,Cl,Cl,Cl,Cl",
            "11,BRIGADE 3,1,38,43,GR,GR,Fu,Fu,Ve,La",
            "11,BRIGADE 4,1,38,43,GR,GR,Fu,Fu,Ve,La",
            "11,BRIGADE 5,1,38,43,GR,GR,Fu,Fu,Ve,La",
            "11,BRIGADE 6,1,38,43,RG,RG,Fu,Pi,Pi,La",
            "11,BRIGADE 7,1,38,43,GR,GR,GR,Rm,Rm,La",
            "11,DRAGOON BRIG,1,38,43,Dr,Dr,Dr,Dr,Ma,Ma",
            "11,GUARD BRIG,1,38,43,GC,GC,Cu,Cu,Ma,Ma",
            "11,BRIGADE 8,1,38,48,GR,Ln,Ln,Ln,Ve,La",
            "11,BRIGADE 9,1,38,48,GR,Ln,Ln,Ln,Ve,La",
            "11,BRIGADE 10,1,38,43,GR,Ln,Ln,Ln,Uh,Uh",
            "11,BRIGADE 11,1,38,43,GR,Ln,Ln,Ln,Uh,Uh",
            "11,BRIGADE 12,1,38,43,Ve,Ve,Ve,Ve,Ve,La",
            "11,BRIGADE 13,1,38,43,Ve,Ve,Ve,Ve,Ve,La",
            "11,BRIGADE 14,1,38,43,Ve,Ve,Ve,Ve,Ve,--",
            "11,BRIGADE 15,1,38,43,Ln,Ln,Ln,Ln,Pi,Pi",
            "11,BRIGADE 16,1,29,44,Ve,Ve,Ve,Ve,Pi,Pi",
    };

    /**
     * Total number of records.
     */
    public static final int TOTAL_RECORDS = DATA.length;

    /**
     * Default constructor.
     */
    public BrigadeInitializer() {
        super();
        LOGGER.debug("BrigadeInitializer instantiated.");
    }

    /**
     * Checks if the database is properly initialized.
     *
     * @return true if database is not correctly initialized.
     */
    public boolean needsInitialization() {
        final List<Brigade> records = BrigadeManager.getInstance().list();
        return (records.size() != TOTAL_RECORDS);
    }

    /**
     * Initializes the database by populating it with the proper records.
     */
    public void initialize() {
        LOGGER.debug("BrigadeInitializer invoked.");

        final Game game = GameManager.getInstance().getByID(-1);

        final CarrierInfo emptyCarrierInfo = new CarrierInfo();
        emptyCarrierInfo.setCarrierType(0);
        emptyCarrierInfo.setCarrierId(0);

        // Initialize records
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            final Brigade thisBrigade = new Brigade(); //NOPMD

            final StringTokenizer thisStk = new StringTokenizer(DATA[i], ","); // NOPMD

            final int nationId = Integer.parseInt(thisStk.nextToken()); // NOPMD
            thisBrigade.setNation(NationManager.getInstance().getByID(nationId));
            thisBrigade.setName(thisStk.nextToken());

            final Position thisPosition = new Position(); // NOPMD
            thisPosition.setRegion(RegionManager.getInstance().getByID(Integer.parseInt(thisStk.nextToken())));
            thisPosition.setX(Integer.parseInt(thisStk.nextToken()) - 1);
            thisPosition.setY(Integer.parseInt(thisStk.nextToken()) - 1);

            // Adjust position due to smaller European map
            if (thisPosition.getRegion().getId() == RegionConstants.EUROPE) {
                thisPosition.setX(thisPosition.getX() - 12);
                thisPosition.setY(thisPosition.getY() - 14);
            }

            thisPosition.setGame(game);

            thisBrigade.setPosition(thisPosition);

            final FieldBattlePosition fbPosition = new FieldBattlePosition();
            fbPosition.setPlaced(false);
            fbPosition.setX(0);
            fbPosition.setY(0);
            thisBrigade.setFieldBattlePosition(fbPosition);

            thisBrigade.setBattalions(new HashSet<Battalion>()); // NOPMD
            for (int bat = 0; bat < TOT_BATT; bat++) {
                final String thisType = thisStk.nextToken(); // NOPMD
                if ("--".equals(thisType)) {
                    // Empty battalion
                    break;

                } else {
                    final Battalion thisBat = new Battalion(); // NOPMD
                    try {
                        thisBat.setType(ArmyTypeManager.getInstance().getByShortName(thisType, thisBrigade.getNation()));
                    } catch (Exception ex) {
                        LOGGER.fatal("Type " + thisType + " for nation " + thisBrigade.getNation().getId() + " is not unique.", ex);
                        break;
                    }

                    if (thisBat.getType() == null) {
                        List<ArmyType> lstTypes = ArmyTypeManager.getInstance().list(thisBrigade.getNation());
                        thisBat.setType(lstTypes.get(0));
                        LOGGER.error("Unknown type " + thisType + " for nation " + thisBrigade.getNation().getId());
                    }

                    thisBat.setExperience(thisBat.getType().getMaxExp());

                    // Determine the maximum headcount
                    int headcount = 800;
                    if (thisBat.getType().getNation().getId() == NationConstants.NATION_MOROCCO
                            || thisBat.getType().getNation().getId() == NationConstants.NATION_OTTOMAN
                            || thisBat.getType().getNation().getId() == NationConstants.NATION_EGYPT) {
                        headcount = 1000;
                    }

                    thisBat.setHeadcount(headcount);
                    thisBat.setOrder(bat + 1);
                    thisBat.setHasMoved(false);
                    thisBat.setNotSupplied(false);
                    thisBat.setHasLost(false);
                    thisBat.setCarrierInfo(emptyCarrierInfo);
                    thisBrigade.getBattalions().add(thisBat);
                }
            }
            thisBrigade.setFromInit(true);
            thisBrigade.updateMP();
            BrigadeManager.getInstance().add(thisBrigade);
        }

        LOGGER.info("BrigadeInitializer complete.");
    }

}
