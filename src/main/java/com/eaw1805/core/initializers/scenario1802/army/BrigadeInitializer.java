package com.eaw1805.core.initializers.scenario1802.army;

import com.eaw1805.core.initializers.AbstractThreadedInitializer;
import com.eaw1805.data.constants.NationConstants;
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
            "1,HUNGARY BRIG. 1,1,52,33,GR,GR,Fu,Fu,Jg,Rm",
            "1,HUNGARY BRIG. 2,1,52,33,GR,GR,Fu,Fu,Jg,Rm",
            "1,HUNGARY BRIG. 3,1,52,33,GR,GR,Fu,Fu,Jg,Rm",
            "1,HUNGARY BRIG. 4,1,52,33,GR,GR,Fu,Fu,Jg,Rm",
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
            "2,MARINE-BRIG 2,3,40,28,Kt,Kt,Kt,Ca,Mc,--",
            "2,COLONIAL-BRIG 1,3,40,28,Kt,Ca,Kt,Mc,Mc,Mc",
            "3,BRIGADE 1,1,37,16,GR,GR,GR,Rm,Rm,La",
            "3,BRIGADE 2,1,37,16,GR,GR,Ln,Ln,Li,La",
            "3,BRIGADE 3,1,37,16,GR,GR,Ln,Ln,Li,La",
            "3,BRIGADE 4,1,37,16,GR,GR,Ln,Ln,Li,La",
            "3,BRIGADE 5,1,37,16,Ln,Ln,Pi,Pi,La,La",
            "3,DRAGOON BRIG,1,37,16,Dr,Dr,Dr,Dr,Ma,Ma",
            "3,GARDE CAVALRY BRIG,1,37,16,Kg,Kg,Kg,Kg,Ma,Ma",
            "3,BRIGADE 6,1,31,12,GR,GR,GR,GR,La,La",
            "3,BRIGADE 7,1,31,12,GR,GR,Ln,Ln,Rm,Rm",
            "3,BRIGADE 8,1,31,12,Ln,Ln,Ln,Li,Li,Li",
            "3,CAVALRY BRIG,1,31,12,Cu,Cu,Uh,Uh,Uh,--",
            "3,MARINE-BRIG,2,35,14,Kt,Kt,Kt,Kt,Ca,Ca",
            "3,COLONIAL-BRIG,2,35,14,Ca,Kt,Ca,Mc,Mc,--",
            "4,BRIGADE 1,1,13,41,Cu,Cu,Dr,Dr,Ma,Ma",
            "4,BRIGADE 2,1,13,41,Cu,Cu,Dr,Dr,Hu,Hu",
            "4,BRIGADE 3,1,13,41,GR,Ln,Ln,Ln,Li,Cz",
            "4,BRIGADE 4,1,13,41,GR,Ln,Ln,Ln,Li,Cz",
            "4,BRIGADE 5,1,13,41,GR,GR,Pi,Pi,Pi,Pi",
            "4,BRIGADE 6,1,07,34,Cu,Cu,Uh,Uh,Dr,Hu",
            "4,BRIGADE 7,1,07,34,GR,Ln,Ln,Ln,Li,La",
            "4,BRIGADE 8,1,07,34,GR,Ln,Ln,Ln,Li,La",
            "4,BRIGADE 9,1,07,34,GR,Ln,Ln,Ln,La,Pi",
            "4,BRIGADE 10,1,07,34,GR,Ln,Ln,Ln,La,Pi",
            "4,MARINE-BRIG,2,26,23,Kt,Kt,Kt,CD,CD,Ca",
            "4,COLONIAL-BRIG,2,26,23,Ca,Ca,Ca,Ca,Ca,Ca",
            "4,COLONIAL-BRIG,2,26,23,Mc,Mc,Mc,Mc,Mc,--",
            "4,MARINE-BRIG,2,09,15,Kt,Kt,Kt,Ca,Ca,Ca",
            "4,COLONIAL-BRIG,2,09,15,Ca,Ca,Ca,Mc,Mc,Mc",
            "4,COLONIAL-BRIG,2,09,15,Ca,Ca,Mc,Mc,Mc,--",
            "4,MARINE-BRIG,2,22,11,Kt,Kt,Ca,Ca,Ca,Mc",
            "4,MARINE-BRIG,3,40,15,Kt,Kt,Kt,Ca,Ca,Mc",
            "4,COLONIAL-BRIG,3,40,15,Ca,Ca,Ca,Ca,Ca,Mc",
            "4,COLONIAL-BRIG,3,40,15,Ca,Ca,Ca,Mc,Mc,Mc",
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
            "5,MARINE-BRIG,3,18,6,Kt,Kt,Ca,Ca,Ca,Ca",
            "5,COLONIAL-BRIG,3,18,6,Ca,Ca,Ca,Mc,Mc,Mc",
            "5,MARINE-BRIG,3,32,19,Kt,Kt,Kt,Kt,Ca,Mc",
            "5,COLONIAL-BRIG,3,32,19,Mc,Mc,Mc,Mc,Mc,Ca",
            "5,AFRICA BRIGADE 1,4,7,4,Ca,Ca,Tw,Tw,--,--",
            "5,AFRICA BRIGADE 2,4,30,14,Kt,Sm,Sm,Sm,--,--",
            "6,ROYAL GUARDS 1,1,23,20,Fg,Fg,Fg,Fg,Rm,La",
            "6,ROYAL GUARDS 2,1,23,20,Fg,Fg,Fg,Fg,Rm,La",
            "6,BRIGADE 1,1,23,20,Be,Be,Ln,Ln,La,La",
            "6,BRIGADE 2,1,23,20,KL,KL,KL,KL,Pi,Mi",
            "6,BRIGADE 3 ,1,23,20,Hi,Hi,Hi,Ln,Ln,Pi",
            "6,BRIGADE 4,1,23,20,Lg,Lg,Dr,Dr,Ma,Ma",
            "6,LIGHT CAVALRY BRIG.,1,13,19,CB,CB,lD,lD,lD,lD",
            "6,DUBLIN GARRISON,1,13,19,Be,Ln,Ln,Ln,Pi,Pi",
            "6,GIBRALTAR GARRISON,1,09,46,Be,Ln,Ln,Ln,Mi,Mi",
            "6,MARINE-BRIG,2,24,8,Kt,Kt,Kt,Ca,Ca,Ca",
            "6,COLONIAL-BRIG,2,24,8,Ca,Ca,Ca,Mc,Mc,Mc",
            "6,MARINE-BRIG,3,6,10,Kt,Kt,Kt,Ca,Ca,Ca",
            "6,COLONIAL-BRIG,3,6,10,Ca,Ca,Ca,Mc,Mc,--",
            "6,MARINE-BRIG,3,12,19,Kt,Kt,Ca,Ca,Ca,--",
            "6,COLONIAL-BRIG,3,12,19,Ca,Ca,Mc,Mc,--,--",
            "6,COLONIAL-BRIG,3,21,5,Ca,Ca,Kt,Mc,Mc,Ca",
            "6,COLONIAL-BRIG,3,21,5,Ca,Ca,Ca,Mc,Mc,Mc",
            "6,COLONIAL-BRIG,3,21,5,Kt,Ca,Ca,Mc,Mc,Mc",
            "6,AFRICA BRIGADE,4,17,26,CM,Kt,Sm,Sm,--,--",
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
            "7,MARINE-BRIG,3,25,26,Kt,Kt,Ca,Mc,Mc,Mc",
            "7,COLONIAL-BRIG,3,25,26,Kt,Ca,CD,Mc,Mc,--",
            "7,COLONIAL-BRIG,3,25,26,Ca,Ca,Ca,Mc,Mc,Mc",
            "7,AFRICA BRIGADE,4,15,26,CM,Kt,Kt,Sm,Sm,--",
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
            "8,BRIGADE 15,1,45,47,Fu,Fu,Fu,Fu,Fu,--",
            "8,MARINE-BRIG,2,30,20,Kt,Kt,Kt,Ca,Ca,--",
            "8,COLONIAL-BRIG,2,30,20,Ca,Kt,Mc,Mc,Mc,--",
            "9,BRIGADE 1,1,05,41,GR,GR,GR,GR,AT,AT",
            "9,BRIGADE 2,1,05,41,GR,GR,Cz,Cz,Pi,La",
            "9,BRIGADE 3,1,05,41,Ln,Ln,Ln,Ln,Cz,La",
            "9,BRIGADE 4,1,05,41,Li,Li,Li,Li,Cz,La",
            "9,BRIGADE 5,1,05,41,Dr,Dr,lD,lD,Uh,Uh",
            "9,BRIGADE 6,1,05,41,Dr,Dr,Uh,Uh,Ma,Ma",
            "9,BRIGADE 7,1,05,41,Ln,Ln,Ln,Ln,lD,lD",
            "9,BRIGADE 8,1,05,41,GR,Cz,Ln,Ln,La,La",
            "9,BRIGADE 9,1,05,41,GR,Cz,Ln,Ln,La,La",
            "9,GRENADIERS BRIG 1,1,05,41,GR,GR,GR,GR,GR,Ha",
            "9,GRENADIERS BRIG 2,1,05,41,GR,GR,GR,GR,GR,Ha",
            "9,GUARD BRIG 1,1,05,41,Gg,Gg,GR,GR,Ha,Ha",
            "9,MARINE-BRIG,2,40,27,Kt,Kt,Kt,Ca,Ca,Ca",
            "9,COLONIAL-BRIG,2,40,27,Ca,Ca,Ca,Mc,Mc,Mc",
            "9,COLONIAL-BRIG,3,5,5,Ca,Ca,Ca,Mc,Mc,Mc",
            "9,MARINE-BRIG,3,5,5,Kt,Kt,Kt,Mc,Mc,Mc",
            "9,AFRICA BRIGADE,4,26,14,CM,Kt,Sm,Sm,--,--",
            "10,BRIGADE 1,1,05,55,Pa,Pa,Pa,Pa,La,La",
            "10,BRIGADE 2,1,05,55,Tc,Ac,Bf,Bf,La,La",
            "10,BRIGADE 3,1,05,55,Tc,Ac,Bf,Bf,La,La",
            "10,BRIGADE 4,1,05,55,Rf,Rf,Rf,Rf,Rf,Rf",
            "10,NOMADS BRIG 1,1,05,55,No,No,No,No,No,No",
            "10,BRIGADE 5,1,21,50,Tc,Tm,Tm,Tm,Tm,La",
            "10,BRIGADE 6,1,21,50,Tc,Tm,Tm,Tm,Tm,La",
            "10,BRIGADE 7,1,21,50,Ct,Ct,Be,Be,Be,Be",
            "10,BRIGADE 8,1,21,50,Ct,Ct,Tu,Tu,Tu,Tu",
            "10,BRIGADE 9,1,21,50,Rf,Rf,Rf,Rf,Rf,Rf",
            "10,NOMADS BRIG 2,1,21,50,No,No,No,No,No,No",
            "10,MARINE-BRIG,3,4,18,Kt,Kt,Kt,Ca,Ca,--",
            "10,COLONIAL-BRIG,3,4,18,Ca,Ca,Ca,Mc,Mc,--",
            "10,AFRICA BRIGADE,4,5,0,Tw,Tw,Th,Th,Th,--",
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
            "11,BRIGADE 10,1,38,59,GR,Ln,Ln,Ln,Uh,Uh",
            "11,BRIGADE 11,1,38,59,GR,Ln,Ln,Ln,Uh,Uh",
            "11,BRIGADE 12,1,29,43,Ve,Ve,Ve,Ve,Ve,La",
            "11,BRIGADE 13,1,38,59,Ve,Ve,Ve,Ve,Ve,La",
            "11,BRIGADE 14,1,38,59,Ve,Ve,Ve,Ve,Ve,--",
            "11,BRIGADE 15,1,38,59,Ln,Ln,Ln,Ln,Pi,Pi",
            "11,BRIGADE 16,1,29,44,Ve,Ve,Ve,Ve,Pi,Pi",
            "11,MARINE-BRIG,3,23,16,Kt,Kt,Kt,Ca,Ca,--",
            "11,COLONIAL-BRIG,3,23,16,Ca,Kt,Ca,Mc,Mc,Mc",
            "12,GUARD BRIGADE 1,1,39,22,GS,GS,GS,GS,Ha,Ha",
            "12,GUARD BRIGADE 2,1,39,22,FG,FG,FG,FG,Ha,Ha",
            "12,BRIGADE 3,1,39,22,Cu,Cu,Dr,Dr,Uh,Uh",
            "12,BRIGADE 4,1,39,22,GR,GR,Rm,Fu,Fu,Pi",
            "12,BRIGADE 5,1,39,22,GR,GR,Rm,Fu,Fu,Pi",
            "12,BRIGADE 6,1,39,22,GR,GR,Pi,Pi,Pi,Pi",
            "12,BRIGADE 7,1,48,16,Cu,Cu,Dr,Dr,Uh,Uh",
            "12,BRIGADE 8,1,48,16,GR,GR,Fu,Fu,Rm,La",
            "12,BRIGADE 9,1,48,16,GR,GR,Fu,Fu,Rm,La",
            "12,BRIGADE 10,1,48,16,GR,GR,Fu,Fu,Rm,La",
            "12,BRIGADE 11,1,48,16,GR,GR,Fu,Fu,Rm,La",
            "12,AFRICA BRIGADE,4,27,5,CM,Ca,Ca,Ca,--,--",
            "13,PREOBRAZHENSKY BRIG,1,58,06,Gg,Gg,Gg,GA,GA,Ha",
            "13,ISMAILOVSKY BRIG,1,58,06,Gg,Gg,Gg,GA,GA,Ha",
            "13,LIFE GUARDS 1,1,58,06,Gc,Gc,Dr,Dr,Ma,Ma",
            "13,LIFE GUARDS 2,1,58,06,Gc,Gc,Dr,Dr,Ma,Ma",
            "13,BRIGADE 1,1,69,17,GR,GR,Jg,Mu,Mu,La",
            "13,BRIGADE 2,1,69,17,GR,GR,Jg,Mu,Mu,La",
            "13,BRIGADE 3,1,69,17,GR,GR,Jg,Mu,Mu,La",
            "13,BRIGADE 4,1,69,17,GR,GR,Jg,Mu,Mu,La",
            "13,BRIGADE 5,1,63,31,Cu,Cu,Dr,Dr,Hu,Hu",
            "13,BRIGADE 6,1,63,31,GR,GR,Rm,Mu,Mu,La",
            "13,BRIGADE 7,1,63,31,GR,GR,Rm,Mu,Mu,La",
            "14,GUARD BRIGADE 1,1,43,10,KL,KL,KL,KL,Dr,Ma",
            "14,GUARD BRIGADE 2,1,43,10,KL,KL,KL,KL,Dr,Ma",
            "14,BRIGADE 3,1,43,10,LG,LG,Li,Li,Pi,Ha",
            "14,BRIGADE 4,1,43,10,GR,GR,Mu,Mu,Pi,Ha",
            "14,BRIGADE 5,1,43,10,GR,GR,Mu,Mu,Pi,Ha",
            "14,BRIGADE 6,1,43,10,Rm,Rm,Rm,Rm,Ma,Ma",
            "14,BRIGADE 7,1,55,04,Dr,Dr,Hu,Hu,Hu,Hu",
            "14,BRIGADE 8,1,55,04,GR,GR,Rm,Rm,Mu,La",
            "14,BRIGADE 9,1,55,04,GR,GR,Rm,Rm,Mu,La",
            "14,MARINE-BRIG,3,23,18,Kt,Kt,Kt,Ca,Ca,Mc",
            "14,COLONIAL-BRIG,3,23,18,Ca,Kt,Ca,Mc,Mc,Mc",
            "14,AFRICA BRIGADE,4,9,9,Sm,Sm,Ca,Ca,--,--",
            "15,PALACE BRIGADE 1,1,58,40,JG,JG,Ja,Ja,Ha,Ha",
            "15,PALACE BRIGADE 2,1,58,40,JG,JG,Ja,Ja,Ha,Ha",
            "15,PALACE SIPAHIS 1,1,58,40,Sp,Sp,Si,Si,Si,Si",
            "15,PALACE SIPAHIS 2,1,58,40,Sp,Sp,Si,Si,Si,Si",
            "15,BRIGADE 1,1,51,48,Ja,Ja,Ja,Tu,La,La",
            "15,BRIGADE 2,1,51,48,Ni,Ni,Ni,Tu,La,La",
            "15,BRIGADE 3,1,51,48,Dr,Dr,Ak,Ak,Yo,Yo",
            "15,BRIGADE 4,1,72,39,Ni,Ni,Ni,Tu,La,La",
            "15,BRIGADE 5,1,72,49,Ni,Ni,Ni,Tu,La,La",
            "15,BRIGADE 6,1,72,49,Ba,Ba,Ba,Tu,La,La",
            "16,BRIGADE 1,1,51,13,GR,GR,Fu,Fu,Li,Li",
            "16,BRIGADE 2,1,51,13,GR,GR,Fu,Fu,Li,Li",
            "16,BRIGADE 3,1,52,20,Gc,Gc,Gc,Gc,Cu,Cu",
            "16,BRIGADE 4,1,52,20,Gc,Gc,Gc,Gc,Ma,Ma",
            "16,BRIGADE 5,1,52,20,GR,Vo,Fu,Pi,Pi,La",
            "16,BRIGADE 6,1,52,20,GR,Vo,Fu,Pi,Pi,La",
            "16,LIGHT CAVALRY BRIG. 1,1,52,20,Cl,Cl,MR,MR,Ma,Ma",
            "16,LIGHT CAVALRY BRIG. 2,1,52,20,Cl,Cl,MR,MR,Ma,Ma",
            "16,COSSACK BRIG,1,51,13,Co,Co,Co,Co,Co,Co",
            "16,BRIGADE,1,51,13,Kt,Kt,Kt,Vo,Vo,Vo",
            "17,PALACE BRIGADE 1,1,58,60,Pg,Pg,Fe,Fe,Pi,La",
            "17,PALACE BRIGADE 2,1,58,60,Pg,Pg,Fe,Fe,Pi,La",
            "17,PALACE MAMELUKES 1,1,58,60,Pm,Pm,Pm,Pm,Mm,Mm",
            "17,PALACE MAMELUKES 2,1,58,60,Pm,Pm,Pm,Pm,Mm,Mm",
            "17,BRIGADE 1,1,58,60,Ct,Ct,Ct,Ct,Ac,Ac",
            "17,BRIGADE 2,1,58,60,Sk,Sk,Mg,Bf,Bf,La",
            "17,BRIGADE 3,1,58,60,Sk,Sk,Mg,Bf,Bf,La",
            "17,BRIGADE 4,1,58,60,Sk,Sk,Fe,Fe,Bf,Bf",
            "17,BRIGADE 5,1,58,60,Mm,Mm,Mm,Mm,Mm,Mm",
            "17,BRIGADE 6,1,62,73,Sk,Sk,Mg,Mg,Bf,Bf",
            "17,BRIGADE 7,1,62,73,Sk,Sk,Mg,Mg,Bf,Bf",
            "17,BRIGADE 8,1,62,73,Ac,Ac,Bc,Bc,Bc,Bc"
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

            thisBrigade.updateMP();
            BrigadeManager.getInstance().add(thisBrigade);
        }

        LOGGER.info("BrigadeInitializer complete.");
    }

}
