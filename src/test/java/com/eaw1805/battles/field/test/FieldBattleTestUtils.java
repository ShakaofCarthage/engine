package com.eaw1805.battles.field.test;

import com.eaw1805.battles.BattleField;
import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.battles.field.generation.MapBuilder;
import com.eaw1805.battles.field.utils.ArmyUtils;
import com.eaw1805.battles.field.utils.MathUtils;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.constants.TerrainConstants;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.ArmyType;
import com.eaw1805.data.model.army.Battalion;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.battles.field.FieldBattlePosition;
import com.eaw1805.data.model.battles.field.Order;
import com.eaw1805.data.model.battles.field.enumerations.ArmEnum;
import com.eaw1805.data.model.battles.field.enumerations.FormationEnum;
import com.eaw1805.data.model.battles.field.enumerations.MoraleStatusEnum;
import com.eaw1805.data.model.battles.field.enumerations.OrdersEnum;
import com.eaw1805.data.model.map.CarrierInfo;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.ProductionSite;
import com.eaw1805.data.model.map.Region;
import com.eaw1805.data.model.map.Sector;
import com.eaw1805.data.model.map.Terrain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class FieldBattleTestUtils {

    static MapBuilder prepareMapBuilder(int terrainType,
                                        int terrainMaxDensity, int battalionNumber,
                                        int fortSize, int side1, int side2) {

        BattleField bf = FieldBattleTestUtils.prepareBattleField(terrainType, terrainMaxDensity, fortSize, side1, side2);

        List<List<Brigade>> sideBrigades = FieldBattleTestUtils.initializeSideBrigades(battalionNumber);
        List<Nation> defendingSide = bf.getSide(MathUtils.generateRandomIntInRange(0, 1));
        int defendingNation = MathUtils.generateRandomIntInRange(0, defendingSide.size() - 1);
        bf.getField().setNation(defendingSide.get(defendingNation));

        return new MapBuilder(bf, sideBrigades);
    }

    static List<List<Brigade>> initializeSideBrigades(int totalBattalionCount) {

        List<List<Brigade>> sideBrigades = new ArrayList<List<Brigade>>();

        List<Brigade> side1Brigades = new ArrayList<Brigade>();
        List<Brigade> side2Brigades = new ArrayList<Brigade>();

        sideBrigades.add(side1Brigades);
        sideBrigades.add(side2Brigades);

        for (int i = 0; i < totalBattalionCount / 10; i++) {
            Brigade brigade1 = new Brigade();
            brigade1.setBrigadeId(i);
            Position position1 = new Position();
            Region region1 = new Region();
            position1.setRegion(region1);
            position1.setX(i);
            position1.setY(i);
            region1.setId(i);
            brigade1.setPosition(position1);
            brigade1.setMps(i);
            brigade1.setCorp(i);
            brigade1.setName("name" + i);
            Nation nation1 = new Nation();
            nation1.setId(i);
            brigade1.setNation(nation1);

            brigade1.setBattalions(new HashSet<Battalion>());
            side1Brigades.add(brigade1);

            Brigade brigade2 = new Brigade();
            brigade2.setBrigadeId(i);
            Position position2 = new Position();
            Region region2 = new Region();
            position2.setRegion(region2);
            position2.setX(i);
            position2.setY(i);
            region2.setId(i);
            brigade2.setPosition(position2);
            brigade2.setMps(i);
            brigade2.setCorp(i);
            brigade2.setName("name" + i);
            Nation nation2 = new Nation();
            nation2.setId(i);
            brigade2.setNation(nation2);

            brigade2.setBattalions(new HashSet<Battalion>());
            side2Brigades.add(brigade2);
        }

        for (int i = 0; i < totalBattalionCount / 10; i++) {

            Brigade brigade1 = side1Brigades.get(i);

            Brigade brigade2 = side2Brigades.get(i);

            for (int k = 0; k < 10; k++) {

                Battalion battalion1 = new Battalion();
                battalion1.setBrigade(brigade1);
                ArmyType armyType1 = new ArmyType();
                armyType1.setId(i);
                Nation nation1 = new Nation();
                nation1.setId(i);
                armyType1.setNation(nation1);
                battalion1.setType(armyType1);
                CarrierInfo carrierInfo1 = new CarrierInfo();
                carrierInfo1.setCarrierId(i);
                battalion1.setCarrierInfo(carrierInfo1);
                carrierInfo1.setCarrierType(i);
                brigade1.getBattalions().add(battalion1);

                Battalion battalion2 = new Battalion();
                battalion2.setBrigade(brigade2);
                ArmyType armyType2 = new ArmyType();
                armyType2.setId(i);
                Nation nation2 = new Nation();
                nation2.setId(i);
                armyType2.setNation(nation2);
                battalion2.setType(armyType2);
                CarrierInfo carrierInfo2 = new CarrierInfo();
                carrierInfo2.setCarrierId(i);
                carrierInfo2.setCarrierType(i);
                battalion2.setCarrierInfo(carrierInfo2);
                brigade2.getBattalions().add(battalion2);
            }
        }

        return sideBrigades;
    }


    static BattleField prepareBattleField(int terrainType,
                                          int terrainMaxDensity,
                                          int fortSize, int side1, int side2) {

        Terrain terrain = new Terrain();
        terrain.setId(terrainType);
        terrain.setMaxDensity(terrainMaxDensity);

        Sector sector = new Sector();
        sector.setTerrain(terrain);
        sector.setPopulation(0);
        sector.setProductionSite(new ProductionSite(fortSize));

        BattleField bf = new BattleField(sector);

        for (int i = 0; i < side1; i++) {
            Nation nation = new Nation();
            nation.setId(i);
            bf.addNation(0, nation);
        }

        for (int i = 0; i < side2; i++) {
            Nation nation = new Nation();
            nation.setId(side1 + i);
            bf.addNation(1, nation);
        }
        return bf;
    }

    static FieldBattleProcessor prepareBattleFieldBattleProcessor(int terrainType,
                                                                  int terrainMaxDensity,
                                                                  int fortSize, int side1, int side2) {


        FieldBattleProcessor fbProcessor = new FieldBattleProcessor(false, HibernateUtil.DB_S1);
        return fbProcessor;

    }

    static void initializeOrdersForBrigades(List<Brigade>[] sideBrigades, FieldBattleMap fbMap) {

        List<Brigade> side0Brigades = sideBrigades[0];
        for (Brigade brigade : side0Brigades) {
            Order basicOrder = new Order();
            basicOrder.setActivationRound(0);
            basicOrder.setCheckpoint1(new FieldBattlePosition(20, fbMap.getSizeY() - 1));
            basicOrder.setOrderTypeEnum(OrdersEnum.MOVE_TO_FIRE);
            brigade.setBasicOrder(basicOrder);
        }

        List<Brigade> side1Brigades = sideBrigades[1];
        for (Brigade brigade : side1Brigades) {
            Order basicOrder = new Order();
            basicOrder.setActivationRound(0);
            basicOrder.setCheckpoint1(new FieldBattlePosition(20, 0));
            basicOrder.setOrderTypeEnum(OrdersEnum.MOVE_TO_FIRE);
            brigade.setBasicOrder(basicOrder);
        }
    }

    /**
     * Constants related to Terrains.
     */
    static enum TerrainConstantsEnum {

        FIRST(TerrainConstants.TERRAIN_FIRST, "First terrain"),
        VERY_FIRST(TerrainConstants.TERRAIN_VERY_F, "Very first terrain"),
        LAST(TerrainConstants.TERRAIN_LAST, "Last terrain"),
        PLAINS(TerrainConstants.TERRAIN_B, "Plains"),
        DESERT(TerrainConstants.TERRAIN_D, "Desert"),
        MOUNTAINS(TerrainConstants.TERRAIN_G, "Mountains"),
        HILLS(TerrainConstants.TERRAIN_H, "Hills"),
        STONY_STEPPE(TerrainConstants.TERRAIN_K, "Stony steppe"),
        GRASS(TerrainConstants.TERRAIN_Q, "Grass"),
        WOODS(TerrainConstants.TERRAIN_W, "Woods"),
        SWAMP(TerrainConstants.TERRAIN_S, "Swamp"),
        TAIGA(TerrainConstants.TERRAIN_T, "Taiga"),
        JUNGLE(TerrainConstants.TERRAIN_J, "Jungle"),
        RIVER(TerrainConstants.TERRAIN_R, "River"),
        OCEAN(TerrainConstants.TERRAIN_O, "Ocean"),
        IMPASSABLE(TerrainConstants.TERRAIN_I, "Impassable");

        private TerrainConstantsEnum(int id, String name) {
            this.id = id;
            this.name = name;
        }

        private final int id;
        private final String name;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public static TerrainConstantsEnum fromId(int id) {
            for (TerrainConstantsEnum tce : TerrainConstantsEnum.values()) {
                if (tce.getId() == id) {
                    return tce;
                }
            }
            return null;
        }
    }

    public static Brigade buildCavalryBrigade(int battalionNumber, int battalionHeadcount, int experience, boolean isLightCavalry, boolean isCuirassier) {
        Brigade brigade = new Brigade();

        ArmyType cavalryArmyType = new ArmyType();
        cavalryArmyType.setType("Ca");
        cavalryArmyType.setTroopSpecsLc(isLightCavalry);
        cavalryArmyType.setTroopSpecsCu(isCuirassier);

        brigade.setBattalions(new HashSet<Battalion>());

        for (int i = 0; i < battalionNumber; i++) {
            Battalion battalion = new Battalion();
            battalion.setHeadcount(battalionHeadcount);
            battalion.setType(cavalryArmyType);
            battalion.setExperience(experience);
            brigade.getBattalions().add(battalion);
        }


        return brigade;
    }

    public static Order initializeOrder(int activationRound, OrdersEnum orderEnum, FormationEnum formation) {
        Order order = new Order();
        order.setOrderTypeEnum(orderEnum);
        order.setActivationRound(0);
        order.setFormationEnum(FormationEnum.LINE);
        order.setCheckpoint1(new FieldBattlePosition(-1, -1));
        order.setCheckpoint2(new FieldBattlePosition(-1, -1));
        order.setCheckpoint3(new FieldBattlePosition(-1, -1));
        order.setStrategicPoint1(new FieldBattlePosition(-1, -1));
        order.setStrategicPoint2(new FieldBattlePosition(-1, -1));
        order.setStrategicPoint3(new FieldBattlePosition(-1, -1));
        order.setCustomStrategicPoint1(new FieldBattlePosition(-1, -1));
        order.setCustomStrategicPoint2(new FieldBattlePosition(-1, -1));
        order.setCustomStrategicPoint3(new FieldBattlePosition(-1, -1));

        return order;
    }

    public static void addBattalions(Brigade brigade, Integer battalionHeadCount, ArmEnum armEnum, boolean pioneers) {
        brigade.setBattalions(new HashSet<Battalion>());

        for (int i = 0; i < 6; i++) {
            Battalion battalion = new Battalion();

            battalion.setBrigade(brigade);
            battalion.setHeadcount(battalionHeadCount == null ? MathUtils.generateRandomIntInRange(300, 800) : battalionHeadCount);
            ArmyType armyType = new ArmyType();
            if (pioneers) {
                armyType.setName("Pioneers");
                armyType.setType("in");
            } else {
                armyType.setName(getSampleArmyTypeForArm(armEnum));
                armyType.setType(getSampleArmyTypeForArm(armEnum));
            }
            armyType.setLongRange(8);
            armyType.setLongCombat(7);
            armyType.setHandCombat(8);
            armyType.setMps(8);
            armyType.setSps(8);
            armyType.setFormationSq(true);
            armyType.setNation(new Nation());
            battalion.setCarrierInfo(new CarrierInfo());
            battalion.setType(armyType);
            battalion.setExperience(3);
            brigade.getBattalions().add(battalion);
        }
    }

    private static String getSampleArmyTypeForArm(ArmEnum armEnum) {
        switch (armEnum) {
            case ARTILLERY:
                return "ar";
            case INFANTRY:
                return "in";
            default:
            case CAVALRY:
                return "ca";
        }
    }

    public static Brigade initializeBrigade(int brigadeId, int corpId, FormationEnum formation, int mps, Nation nation) {
        Brigade brigade = new Brigade();
        brigade.setBrigadeId(brigadeId);
        brigade.setCorp(corpId);

        brigade.setNation(nation);
        brigade.setBattalions(new HashSet<Battalion>());

        brigade.setFormationEnum(formation);
        brigade.setArmTypeEnum(ArmyUtils.findArm(brigade));
        brigade.setMps(mps);
        brigade.setPosition(new Position());
        brigade.getPosition().setRegion(new Region());
        brigade.setMoraleStatusEnum(MoraleStatusEnum.NORMAL);
        return brigade;
    }
}
