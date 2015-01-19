package com.eaw1805.battles.field;

import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.army.BrigadeManager;
import com.eaw1805.data.managers.army.CommanderManager;
import com.eaw1805.data.managers.battles.FieldBattleReportManager;
import com.eaw1805.data.managers.field.FieldBattleMapManager;
import com.eaw1805.data.managers.map.SectorManager;
import com.eaw1805.data.model.Nation;
import com.eaw1805.data.model.army.Brigade;
import com.eaw1805.data.model.army.Commander;
import com.eaw1805.data.model.battles.FieldBattleReport;
import com.eaw1805.data.model.battles.field.FieldBattleMap;
import com.eaw1805.data.model.map.Position;
import com.eaw1805.data.model.map.Sector;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class FieldBattleProcessorResourceLocator {

    private final int scenarioId;
    
    public FieldBattleProcessorResourceLocator(final int scenarioId) {
        this.scenarioId = scenarioId;
    }
    
    public FieldBattleMap getFieldBattleMap(final int battleId) {
        Transaction trans = HibernateUtil.getInstance()
                .getSessionFactory(scenarioId).getCurrentSession()
                .beginTransaction();
        FieldBattleMap fbMap = FieldBattleMapManager.getInstance()
                .getByBattleID(battleId);
        trans.commit();
        return fbMap;
    }

    public Commander getCommanderById(final int commanderId) {
        Transaction trans = HibernateUtil.getInstance()
                .getSessionFactory(scenarioId).getCurrentSession()
                .beginTransaction();
        Commander commander = CommanderManager.getInstance().getByID(commanderId);
        trans.commit();
        return commander;

    }

    public void save(FieldBattleMap fbMap) {
        Transaction trans = HibernateUtil.getInstance()
                .getSessionFactory(scenarioId).getCurrentSession()
                .beginTransaction();
        FieldBattleMapManager.getInstance().add(fbMap);
        trans.commit();

    }

    public void updateFieldBattleReport(final FieldBattleReport fbReport) {
        Transaction trans = HibernateUtil.getInstance().getSessionFactory(scenarioId).getCurrentSession().beginTransaction();
        FieldBattleReportManager.getInstance().update(fbReport);
        trans.commit();
    }

    public FieldBattleReport getFieldBattleReport(final int battleId) {
        Transaction trans = HibernateUtil.getInstance().getSessionFactory(scenarioId).getCurrentSession().beginTransaction();
        FieldBattleReport fbReport = FieldBattleReportManager.getInstance().getByID(battleId);
        trans.commit();
        return fbReport;
    }

    public Sector getSectorByPosition(final Position position) {
        Transaction trans = HibernateUtil.getInstance().getSessionFactory(scenarioId).getCurrentSession().beginTransaction();
        Sector sector = SectorManager.getInstance().getByPosition(position);
        trans.commit();
        return sector;
    }

    public List<Brigade> getBrigadesForPositionAndNation(final Position position, final Nation nation) {
        Transaction trans = HibernateUtil.getInstance().getSessionFactory(scenarioId).getCurrentSession().beginTransaction();
        List<Brigade> brigades = BrigadeManager.getInstance().listByPositionNation(position, nation);
        trans.commit();
        filterPlaced(brigades);
        return brigades;
    }

    public void updateBrigades(final List<Brigade>[] sideBrigades) {
        List<Brigade> allBrigades = new ArrayList<Brigade>();
        allBrigades.addAll(sideBrigades[0]);
        allBrigades.addAll(sideBrigades[1]);

        Transaction trans = HibernateUtil.getInstance().getSessionFactory(scenarioId).getCurrentSession().beginTransaction();

        for (Brigade brigade : allBrigades) {
            BrigadeManager.getInstance().update(brigade);
        }
        trans.commit();
    }

    public void updateMap(final FieldBattleMap fbMap) {
        Transaction trans = HibernateUtil.getInstance().getSessionFactory(scenarioId).getCurrentSession().beginTransaction();
        FieldBattleMapManager.getInstance().update(fbMap);
        trans.commit();
    }

    public void updateCommanders(final Set<Commander> commanders) {
        Transaction trans = HibernateUtil.getInstance().getSessionFactory(scenarioId).getCurrentSession().beginTransaction();
        for (Commander commander : commanders) {
            CommanderManager.getInstance().update(commander);
        }
        trans.commit();
    }

    public void filterPlaced(final List<Brigade> inBrigades) {
        Iterator<Brigade> iter = inBrigades.iterator();
        while (iter.hasNext()) {
            if (!iter.next().getFieldBattlePosition().isPlaced()) {
                iter.remove();
            }
        }
    }

}
