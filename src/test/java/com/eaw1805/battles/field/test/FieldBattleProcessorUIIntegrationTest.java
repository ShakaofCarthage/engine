package com.eaw1805.battles.field.test;

import com.eaw1805.battles.field.FieldBattleProcessor;
import com.eaw1805.data.HibernateUtil;
import com.eaw1805.data.managers.battles.FieldBattleReportManager;
import com.eaw1805.data.model.battles.FieldBattleReport;
import com.eaw1805.data.model.battles.field.FieldBattleHalfRoundStatistics;
import org.hibernate.Transaction;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class FieldBattleProcessorUIIntegrationTest {


    private FieldBattleProcessor fieldBattleProcessor;

    @Before
    public void setup() throws Exception {

        HibernateUtil.connectEntityManagers(HibernateUtil.DB_S1);
    }

    @Test
    public void fieldBattle1() {

        fieldBattleProcessor = new FieldBattleProcessor(false, HibernateUtil.DB_S1);
        fieldBattleProcessor.processFirstHalfRounds(20);
    }


    @Test
    @SuppressWarnings("unchecked")
    public void checkStats1() throws Exception {
        Transaction trans = HibernateUtil.getInstance()
                .getSessionFactory(HibernateUtil.DB_S1).getCurrentSession()
                .beginTransaction();
        FieldBattleReport fbReport = FieldBattleReportManager.getInstance().getByID(1);
        trans.commit();

        final ByteArrayInputStream bais = new ByteArrayInputStream(fbReport.getStats());
        final GZIPInputStream zis = new GZIPInputStream(bais);
        final ObjectInputStream is = new ObjectInputStream(zis);
        List<byte[]> fbStats = (List<byte[]>) is.readObject();
        is.close();
        zis.close();
        bais.close();

        List<FieldBattleHalfRoundStatistics> hrStats = new ArrayList<FieldBattleHalfRoundStatistics>();
        for (byte[] fbStatsBytes : fbStats) {

            ByteArrayInputStream bais2 = new ByteArrayInputStream(fbStatsBytes);
            ObjectInputStream is2 = new ObjectInputStream(bais2);

            hrStats.add((FieldBattleHalfRoundStatistics) is2.readObject());

            bais2.close();
            is2.close();
        }

        System.out.println("Done1");
    }

}
