package com.eaw1805.events.scenario1808;

import com.eaw1805.data.constants.GoodConstants;
import com.eaw1805.data.constants.NationConstants;
import com.eaw1805.data.constants.RegionConstants;
import com.eaw1805.data.constants.ReportConstants;
import com.eaw1805.data.managers.NationManager;
import com.eaw1805.data.managers.army.ArmyTypeManager;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.economy.WarehouseManager;
import com.eaw1805.data.managers.map.RegionManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.ArmyType;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattlePosition;
import com.eaw1805.data.model.economy.Warehouse;
import com.eaw1805.data.model.map.CarrierInfo;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.events.AbstractEventProcessor;
import com.eaw1805.events.EventInterface;
import com.eaw1805.events.EventProcessor;
import com.eaw1805.events.RebellionEvent;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Scenario 1808.
 * Additional Scenario events.
 */
public class HistoricEvent
        extends AbstractEventProcessor
        implements EventInterface, RegionConstants, ReportConstants, NationConstants {

    /**
     * Total number of battalions that a brigade can hold.
     */
    public static final int TOT_BATT = 6;

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = Logger.getLogger(RebellionEvent.class);

    /**
     * Default constructor.
     *
     * @param myParent the parent object that invoked us.
     */
    public HistoricEvent(final EventProcessor myParent) {
        super(myParent);
        LOGGER.debug("HistoricEvent instantiated.");
    }

    /**
     * Processes the event.
     */
    public final void process() {
        final int turn = getGame().getTurn();

        final Nation spain = NationManager.getInstance().getByID(NATION_SPAIN);
        final Nation france = NationManager.getInstance().getByID(NATION_FRANCE);
        final Nation britain = NationManager.getInstance().getByID(NATION_GREATBRITAIN);

        switch (turn) {

            case 1: // September 1808
            case 2: // October 1808
                break;

            case 3: // November 1808
            {
                /**
                 * "Nation ID,Name,Region,Position,MP,Corp,
                 * "Nation ID,Name,Region,Position,MP,Federation,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man",
                 * "Nation ID,Name,Region,X,Y,Bat,Bat,Bat,Bat,Bat,Bat",
                 */
                final String[] DATA = { // NOPMD
                        "5,SAN SEBASTIAN REINFORCEMENTS BRIG. 1,1,31,5,Ma,Ma,Ma,Ma,GM,GM",
                        "5,SAN SEBASTIAN REINFORCEMENTS BRIG. 2,1,31,5,Hu,Hu,Hu,Hu,Cr,Cr",
                        "5,SAN SEBASTIAN REINFORCEMENTS BRIG. 3,1,31,5,Cl,Cl,Cl,Cl,Cu,Cu",
                        "5,SAN SEBASTIAN REINFORCEMENTS BRIG. 4,1,31,5,Vo,Vo,Vo,Vo,Ti,Ti",
                        "5,SAN SEBASTIAN REINFORCEMENTS BRIG. 5,1,31,5,Vo,Vo,Vo,Vo,Ti,Ti",
                        "5,SAN SEBASTIAN REINFORCEMENTS BRIG. 6,1,31,5,Pi,Pi,Pi,Pi,Pi,Pi",
                        "5,SAN SEBASTIAN REINFORCEMENTS BRIG. 7,1,31,5,Ln,Ln,Ln,Ln,Gr,Gr",
                        "5,SAN SEBASTIAN REINFORCEMENTS BRIG. 8,1,31,5,Ln,Ln,Ln,Ln,Gr,Gr",
                        "5,SAN SEBASTIAN REINFORCEMENTS BRIG. 9,1,31,5,Vo,Vo,Vo,Ln,Ln,Ln",
                        "5,SAN SEBASTIAN REINFORCEMENTS BRIG. 10,1,31,5,IG,IG,IG,IG,IG,IG",
                };

                /**
                 * Total number of records.
                 */
                final int TOTAL_RECORDS = DATA.length;

                // Initialize records
                for (int i = 0; i < TOTAL_RECORDS; i++) {
                    setupBrigade(DATA[i]);
                }

                // Add news entry
                newsGlobal(france, NEWS_FRONT, true,
                        "Napoleon Assumes Command of Grande Armee in Spain!",
                        "Napoleon Assumes Command of Grande Armee in Spain!");

                break;
            }

            case 4: // December 1808
                break;

            case 5: // January 1809
            {
                // Add news entry
                newsGlobal(france, NEWS_FRONT, true,
                        "Napoleon leaves Peninsula, Joseph in charge of the war effort!",
                        "Napoleon leaves Peninsula, Joseph in charge of the war effort!");

                // Decrease VPs
                changeVP(getParent().getGameEngine().getGame(), france, NAPOLEON_LEAVES, "Napoleon leaves Peninsula");
                break;
            }

            case 6: // February 1809
            case 7: // March 1809
                break;

            case 8: // April 1809
            {
                /**
                 * "Nation ID,Name,Region,Position,MP,Corp,
                 * "Nation ID,Name,Region,Position,MP,Federation,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man",
                 * "Nation ID,Name,Region,X,Y,Bat,Bat,Bat,Bat,Bat,Bat",
                 */
                final String[] DATA = { // NOPMD
                        "6,LISBON REINFORCEMENTS BRIG. 1,1,4,25,Ra,Ra,Ra,Ra,Ra,Ra",
                        "6,LISBON REINFORCEMENTS BRIG. 2,1,4,25,CB,CB,CB,CB,Lg,Lg",
                        "6,LISBON REINFORCEMENTS BRIG. 3,1,4,25,KL,KL,KL,KL,KL,KL",
                        "6,LISBON REINFORCEMENTS BRIG. 4,1,4,25,Be,Be,Be,Fg,Fg,Fg",
                };

                /**
                 * Total number of records.
                 */
                final int TOTAL_RECORDS = DATA.length;

                // Initialize records
                for (int i = 0; i < TOTAL_RECORDS; i++) {
                    setupBrigade(DATA[i]);
                }

                // Add news entry
                newsGlobal(britain, NEWS_FRONT, true,
                        "Wellesley returns to Portugal to take command of British troops",
                        "Wellesley returns to Portugal to take command of British troops");

                // Add news entry
                newsGlobal(france, NEWS_FRONT, true,
                        "Austria invades Bavaria!",
                        "Austria invades Bavaria!");

                break;
            }

            case 9: // May 1809
            case 10: // June 1809
                break;

            case 11: // July 1809
            {
                /**
                 * "Nation ID,Name,Region,Position,MP,Corp,
                 * "Nation ID,Name,Region,Position,MP,Federation,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man,Bat,Bat-Exp,Bat-Man",
                 * "Nation ID,Name,Region,X,Y,Bat,Bat,Bat,Bat,Bat,Bat",
                 */
                final String[] DATA = { // NOPMD
                        "4,CADIZ REINFORCEMENTS BRIG. 1,1,16,36,La,La,La,La,La,La",
                        "4,CADIZ REINFORCEMENTS BRIG. 2,1,16,36,Dr,Dr,Dr,Dr,Gc,Gc",
                        "4,CADIZ REINFORCEMENTS BRIG. 3,1,16,36,Ln,Ln,Ln,Ln,GR,GR",
                        "4,CADIZ REINFORCEMENTS BRIG. 4,1,16,36,Li,Li,Li,Li,Cz,Cz",
                };

                /**
                 * Total number of records.
                 */
                final int TOTAL_RECORDS = DATA.length;

                // Initialize records
                for (int i = 0; i < TOTAL_RECORDS; i++) {
                    setupBrigade(DATA[i]);
                }

                // Add news entry
                newsGlobal(spain, NEWS_FRONT, true,
                        "Spanish veterans from all over Spain regroup to continue the war with British funding",
                        "Spanish veterans from all over Spain regroup to continue the war with British funding");

                break;
            }

            case 12: // August 1809
            case 13: // September 1809
                break;

            case 14: // October 1809
            {
                // Add news entry
                newsGlobal(france, NEWS_FRONT, true,
                        "Treaty of Schonbrunn between Austria and France signed!",
                        "Treaty of Schonbrunn between Austria and France signed!");

                // Increase VPs
                changeVP(getParent().getGameEngine().getGame(), france, SCHONBRUNN_TREATY, "Treaty of Schonbrunn");

                // Add news entry
                newsGlobal(britain, NEWS_FRONT, true,
                        "Wellington starts building defensive fortifications: the Lines of Torres Vedras.",
                        "Wellington starts building defensive fortifications: the Lines of Torres Vedras.");

                // Retrieve warehouse
                final Warehouse britishWarehouse = WarehouseManager.getInstance().getByNationRegion(getGame(),
                        britain, RegionManager.getInstance().getByID(RegionConstants.EUROPE));

                final Map<Integer, Integer> storedGoods =  britishWarehouse.getStoredGoodsQnt();
                storedGoods.put(GoodConstants.GOOD_MONEY, storedGoods.get(GoodConstants.GOOD_MONEY) + 10000000);
                storedGoods.put(GoodConstants.GOOD_INPT, storedGoods.get(GoodConstants.GOOD_INPT) + 5000);
                storedGoods.put(GoodConstants.GOOD_STONE, storedGoods.get(GoodConstants.GOOD_STONE) + 20000);
                britishWarehouse.setStoredGoodsQnt(storedGoods);
                WarehouseManager.getInstance().update(britishWarehouse);

                break;
            }

            case 15: // November 1809
                break;

            case 16: // December 1809
            {
                // Add news entry
                newsGlobal(france, NEWS_FRONT, true,
                        "Napoleon divorces Joesphine",
                        "Napoleon divorces Joesphine");
                break;
            }

            case 17: // January 1810
            case 18: // February 1810
            case 19: // March 1810
                break;

            case 20: // April 1810
            {
                // Add news entry
                newsGlobal(france, NEWS_FRONT, true,
                        "Napoleon marries Marie-Louise of Austria",
                        "Napoleon marries Marie-Louise of Austria");

                // Increase VPs
                changeVP(getParent().getGameEngine().getGame(), france, NAPOLEON_MARRIES, "Napoleon marries Marie-Louise");

                break;
            }

            case 21: // May 1810
            case 22: // June 1810
            case 23: // July 1810
            case 24: // August 1810
            case 25: // September 1810
            case 26: // October 1810
            case 27: // November 1810
            case 28: // December 1810
            case 29: // January 1811
            case 30: // February 1811
            case 31: // March 1811
                break;

            case 32: // April 1811
            {
                // Add news entry
                newsGlobal(france, NEWS_FRONT, true,
                        "Marie-Louise bears Napoleon a son!",
                        "Marie-Louise bears Napoleon a son!");

                // Increase VPs
                changeVP(getParent().getGameEngine().getGame(), france, NAPOLEON_GETSCHILD, "Marie-Louise bears a son");

                break;
            }

            case 33: // May 1811
            case 34: // June 1811
            case 35: // July 1811
            case 36: // August 1811
            case 37: // September 1811
            case 38: // October 1811
            case 39: // November 1811
                break;

            case 40: // December 1811
            {
                // Add news entry
                newsGlobal(france, NEWS_FRONT, true,
                        "Preparations for the invasion of Russia begin",
                        "Preparations for the invasion of Russia begin");

                break;
            }

            case 41: // January 1812
            case 42: // February 1812
            case 43: // March 1812
            case 44: // April 1812
            case 45: // May 1812
                break;

            case 46: // June 1812
            {
                // Add news entry
                newsGlobal(france, NEWS_FRONT, true,
                        "The Sixth Coalition is formed! Napoleon invades Russia!",
                        "The Sixth Coalition is formed! Napoleon invades Russia!");

                // Increase VPs
                changeVP(getParent().getGameEngine().getGame(), spain, NAPOLEON_INVADESRUSSIA, "Napoleon invades Russia");
                changeVP(getParent().getGameEngine().getGame(), britain, NAPOLEON_INVADESRUSSIA, "Napoleon invades Russia");

                break;
            }

            case 47: // July 1812
            case 48: // August 1812
                break;

            case 49: // September 1812
            {
                // Add news entry
                newsGlobal(france, NEWS_FRONT, true,
                        "Napoleon enters Moscow!",
                        "Napoleon enters Moscow!");

                // Increase VPs
                changeVP(getParent().getGameEngine().getGame(), france, NAPOLEON_ENTERMOSCOW, "Napoleon enters Moscow");

                break;
            }

            case 50: // October 1812
            {
                // Add news entry
                newsGlobal(france, NEWS_FRONT, true,
                        "Napoleon leaves Moscow!",
                        "Napoleon leaves Moscow!");

                // Increase VPs
                changeVP(getParent().getGameEngine().getGame(), spain, NAPOLEON_LEAVESMOSCOW, "Napoleon leaves Moscow");
                changeVP(getParent().getGameEngine().getGame(), britain, NAPOLEON_LEAVESMOSCOW, "Napoleon leaves Moscow");

                break;
            }

            case 51: // November 1812
                break;

            case 52: // December 1812
            {
                // Add news entry
                newsGlobal(france, NEWS_FRONT, true,
                        "Yorck signs Convention of Tauroggen",
                        "Yorck signs Convention of Tauroggen");

                // Add news entry
                newsGlobal(france, NEWS_FRONT, true,
                        "Prussian Corps defects from Grand Armee",
                        "Prussian Corps defects from Grand Armee");

                // Increase VPs
                changeVP(getParent().getGameEngine().getGame(), france, TAUROGGEN_CONVENTION, "Prussian Corps defects");

                break;
            }

            case 53: // January 1813
            case 54: // February 1813
                break;

            case 55: // March 1813
            {
                // Add news entry
                newsGlobal(france, NEWS_FRONT, true,
                        "Prussia declares War on France!",
                        "Prussia declares War on France!");

                // Increase VPs
                changeVP(getParent().getGameEngine().getGame(), spain, PRUSSIA_DECLARESWAR, "Prussia declares War on France");
                changeVP(getParent().getGameEngine().getGame(), britain, PRUSSIA_DECLARESWAR, "Prussia declares War on France");

                break;
            }

            case 56: // April 1813
            case 57: // May 1813
            case 58: // June 1813
            case 59: // July 1813
                break;

            case 60: // August 1813
            {
                // Add news entry
                newsGlobal(france, NEWS_FRONT, true,
                        "Austria declares War on France!",
                        "Austria declares War on France!");

                // Increase VPs
                changeVP(getParent().getGameEngine().getGame(), spain, AUSTRIA_DECLARESWAR, "Austria declares War on France");
                changeVP(getParent().getGameEngine().getGame(), britain, AUSTRIA_DECLARESWAR, "Austria declares War on France");

                break;
            }

            case 61: // September 1813
            case 62: // October 1813
            case 63: // November 1813
            case 64: // December 1813
            case 65: // January 1814
            case 66: // February 1814
                break;

            case 67: // March 1814
            {
                // Add news entry
                newsGlobal(france, NEWS_FRONT, true,
                        "Allies enter Paris!",
                        "Allies enter Paris!");

                // Increase VPs
                changeVP(getParent().getGameEngine().getGame(), spain, ALLIES_ENTERPARIS, "Allies enter Paris");
                changeVP(getParent().getGameEngine().getGame(), britain, ALLIES_ENTERPARIS, "Allies enter Paris");

                break;
            }

            case 68: // April 1814
                break;

            case 69: // May 1814
            {
                // Add news entry
                newsGlobal(france, NEWS_FRONT, true,
                        "France surrenders! The Treaty of Paris ends the War between France and the Sixth Coalition.",
                        "France surrenders! The Treaty of Paris ends the War between France and the Sixth Coalition.");

                break;
            }
        }

        LOGGER.info("HistoricEvent processed.");
    }


    protected void setupBrigade(final String format) {
        final StringTokenizer thisStk = new StringTokenizer(format, ","); // NOPMD

        final int nationId = Integer.parseInt(thisStk.nextToken()); // NOPMD

        final CarrierInfo emptyCarrierInfo = new CarrierInfo();
        emptyCarrierInfo.setCarrierType(0);
        emptyCarrierInfo.setCarrierId(0);

        final Brigade thisBrigade = new Brigade(); //NOPMD
        thisBrigade.setNation(NationManager.getInstance().getByID(nationId));
        thisBrigade.setName(thisStk.nextToken());

        final Position thisPosition = new Position(); // NOPMD
        thisPosition.setRegion(RegionManager.getInstance().getByID(Integer.parseInt(thisStk.nextToken())));
        thisPosition.setX(Integer.parseInt(thisStk.nextToken()));
        thisPosition.setY(Integer.parseInt(thisStk.nextToken()));
        thisPosition.setGame(getGame());

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

        thisBrigade.updateMP();
        BrigadeManager.getInstance().add(thisBrigade);
    }

}
